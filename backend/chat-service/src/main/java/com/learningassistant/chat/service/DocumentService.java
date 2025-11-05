package com.learningassistant.chat.service;

import com.learningassistant.chat.client.DocumentServiceClient;
import com.learningassistant.chat.dto.DocumentUploadResponse;
import com.learningassistant.chat.model.ChatDocument;
import com.learningassistant.chat.repository.ChatDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DocumentService {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);
    
    private final ChatDocumentRepository documentRepository;
    private final DocumentServiceClient documentServiceClient;
    
    public DocumentService(ChatDocumentRepository documentRepository, 
                          DocumentServiceClient documentServiceClient) {
        this.documentRepository = documentRepository;
        this.documentServiceClient = documentServiceClient;
    }
    
    public DocumentUploadResponse uploadDocument(MultipartFile file, String userId) {
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }
            
            String fileName = file.getOriginalFilename();
            String fileType = file.getContentType();
            Long fileSize = file.getSize();
            
            logger.info("Uploading document: {} for user: {}", fileName, userId);
            
            // Upload to document-service (if available)
            String documentServiceId = null;
            Map<String, Object> documentServiceResponse = documentServiceClient.uploadDocument(file, userId);
            if (documentServiceResponse != null && documentServiceResponse.containsKey("documentId")) {
                documentServiceId = (String) documentServiceResponse.get("documentId");
                logger.info("Document uploaded to document-service with ID: {}", documentServiceId);
            }
            
            // Save document metadata to chat service database
            ChatDocument document = new ChatDocument(userId, fileName, fileType, null, fileSize);
            document.setDocumentServiceId(documentServiceId);
            
            ChatDocument savedDocument = documentRepository.save(document);
            
            logger.info("Document saved with ID: {}", savedDocument.getId());
            
            return new DocumentUploadResponse(
                savedDocument.getId(),
                fileName,
                fileType,
                fileSize,
                savedDocument.getUploadedAt(),
                "Document uploaded successfully"
            );
            
        } catch (Exception e) {
            logger.error("Error uploading document: {}", e.getMessage());
            throw new RuntimeException("Failed to upload document: " + e.getMessage());
        }
    }
    
    public List<ChatDocument> getUserDocuments(String userId) {
        return documentRepository.findByUserIdOrderByUploadedAtDesc(userId);
    }
    
    public Optional<ChatDocument> getDocumentById(String documentId) {
        return documentRepository.findById(documentId);
    }
    
    public void deleteDocument(String documentId) {
        documentRepository.deleteById(documentId);
    }
}
