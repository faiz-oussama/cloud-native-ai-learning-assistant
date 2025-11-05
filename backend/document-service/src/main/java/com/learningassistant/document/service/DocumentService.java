package com.learningassistant.document.service;

import com.learningassistant.document.client.RagIngestClient;
import com.learningassistant.document.dto.DocumentResponse;
import com.learningassistant.document.model.Document;
import com.learningassistant.document.model.ProcessingStatus;
import com.learningassistant.document.repository.DocumentRepository;
import com.learningassistant.document.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DocumentService {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);
    
    private final DocumentRepository documentRepository;
    private final StorageService storageService;
    private final RagIngestClient ragIngestClient;
    
    public DocumentService(DocumentRepository documentRepository,
                          StorageService storageService,
                          RagIngestClient ragIngestClient) {
        this.documentRepository = documentRepository;
        this.storageService = storageService;
        this.ragIngestClient = ragIngestClient;
        
        // Initialize storage
        storageService.init();
    }
    
    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file, String userId) {
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }
            
            String fileName = file.getOriginalFilename();
            String fileType = file.getContentType();
            Long fileSize = file.getSize();
            
            logger.info("Uploading document: {} for user: {}", fileName, userId);
            
            // Store file
            String filePath = storageService.storeFile(file, userId);
            
            // Create document entity
            Document document = new Document(userId, fileName, fileType, fileSize, filePath);
            document.setStorageLocation("local");
            document.setProcessingStatus(ProcessingStatus.PENDING);
            
            // Save to database
            Document savedDocument = documentRepository.save(document);
            
            logger.info("Document saved with ID: {}", savedDocument.getId());
            
            // Asynchronously ingest to RAG service
            try {
                document.setProcessingStatus(ProcessingStatus.PROCESSING);
                documentRepository.save(document);
                
                Map<String, Object> ragResponse = ragIngestClient.ingestDocument(savedDocument.getId(), file);
                
                if (ragResponse != null && ragResponse.containsKey("ragDocumentId")) {
                    document.setRagDocumentId((String) ragResponse.get("ragDocumentId"));
                    document.setProcessingStatus(ProcessingStatus.COMPLETED);
                    document.setProcessedAt(LocalDateTime.now());
                    logger.info("Document {} ingested successfully", savedDocument.getId());
                } else {
                    document.setProcessingStatus(ProcessingStatus.FAILED);
                    logger.warn("Failed to ingest document {} to RAG service", savedDocument.getId());
                }
                
                documentRepository.save(document);
            } catch (Exception e) {
                logger.error("Error during RAG ingestion: {}", e.getMessage());
                document.setProcessingStatus(ProcessingStatus.FAILED);
                documentRepository.save(document);
            }
            
            return toDocumentResponse(savedDocument);
            
        } catch (Exception e) {
            logger.error("Error uploading document: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload document: " + e.getMessage());
        }
    }
    
    public List<DocumentResponse> getUserDocuments(String userId) {
        List<Document> documents = documentRepository.findByUserIdOrderByUploadedAtDesc(userId);
        return documents.stream()
                .map(this::toDocumentResponse)
                .collect(Collectors.toList());
    }
    
    public Optional<DocumentResponse> getDocumentById(String documentId) {
        return documentRepository.findById(documentId)
                .map(this::toDocumentResponse);
    }
    
    @Transactional
    public void deleteDocument(String documentId) {
        Optional<Document> documentOpt = documentRepository.findById(documentId);
        
        if (documentOpt.isPresent()) {
            Document document = documentOpt.get();
            
            // Delete from storage
            storageService.deleteFile(document.getFilePath());
            
            // Delete from RAG service
            if (document.getRagDocumentId() != null) {
                ragIngestClient.deleteDocument(document.getRagDocumentId());
            }
            
            // Delete from database
            documentRepository.delete(document);
            
            logger.info("Document {} deleted successfully", documentId);
        } else {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }
    }
    
    public List<DocumentResponse> getPendingDocuments() {
        List<Document> documents = documentRepository.findByProcessingStatus(ProcessingStatus.PENDING);
        return documents.stream()
                .map(this::toDocumentResponse)
                .collect(Collectors.toList());
    }
    
    private DocumentResponse toDocumentResponse(Document document) {
        return new DocumentResponse(
            document.getId(),
            document.getUserId(),
            document.getFileName(),
            document.getFileType(),
            document.getFileSize(),
            document.getProcessingStatus(),
            document.getUploadedAt(),
            document.getProcessedAt()
        );
    }
}
