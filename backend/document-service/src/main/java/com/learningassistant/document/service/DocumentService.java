package com.learningassistant.document.service;

import com.learningassistant.document.client.RagIngestClient;
import com.learningassistant.document.dto.DocumentResponse;
import com.learningassistant.document.model.Document;
import com.learningassistant.document.model.ProcessingStatus;
import com.learningassistant.document.repository.DocumentRepository;
import com.learningassistant.document.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentService {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);
    
    private final DocumentRepository documentRepository;
    private final StorageService storageService;
    private final RagIngestClient ragIngestClient;
    private final ServiceBusService serviceBusService;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private PlatformTransactionManager transactionManager;
    
    public DocumentService(DocumentRepository documentRepository,
                          StorageService storageService,
                          RagIngestClient ragIngestClient,
                          ServiceBusService serviceBusService) {
        this.documentRepository = documentRepository;
        this.storageService = storageService;
        this.ragIngestClient = ragIngestClient;
        this.serviceBusService = serviceBusService;
        
        // Initialize storage
        storageService.init();
    }
    
    public DocumentResponse uploadDocument(MultipartFile file, String userId) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }
            
            String fileName = file.getOriginalFilename();
            String fileType = file.getContentType();
            Long fileSize = file.getSize();
            
            logger.info("[UPLOAD] Uploading document: {} for user: {} | CorrelationId: {}", fileName, userId, correlationId);
            
            // Store file
            String filePath = storageService.storeFile(file, userId);
            
            // Generate and normalize document ID upfront
            String documentId = UUID.randomUUID().toString().toLowerCase().trim();
            
            // Use TransactionTemplate for save only
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            Document savedDocument = template.execute(status -> {
                // Create document entity with COMPLETED status immediately
                Document document = new Document(userId, fileName, fileType, fileSize, filePath);
                document.setId(documentId);
                
                String storageType = storageService.getStorageType();
                document.setStorageLocation(storageType);
                
                // Set COMPLETED status immediately - document is available right away
                document.setProcessingStatus(ProcessingStatus.COMPLETED);
                document.setUploadedAt(LocalDateTime.now());
                document.setProcessedAt(LocalDateTime.now());
                
                // Save and flush
                Document saved = documentRepository.saveAndFlush(document);
                
                logger.info("[UPLOAD] Document saved with ID: {} | Status: {} | CorrelationId: {}", 
                    saved.getId(), saved.getProcessingStatus(), correlationId);
                
                // Verify persistence
                if (!documentRepository.existsById(saved.getId())) {
                    logger.error("[UPLOAD] Document not found after save - potential schema/connection issue | Document ID: {}", saved.getId());
                    throw new IllegalStateException("Document not persisted after save: " + saved.getId());
                }
                
                // Log database connection info for debugging
                try {
                    logger.info("[DEBUG] Database connection info - checking if document exists in DB");
                    Optional<Document> verifyDoc = documentRepository.findById(saved.getId());
                    if (verifyDoc.isPresent()) {
                        logger.info("[DEBUG] Document verified in database with ID: {} and status: {}", 
                            verifyDoc.get().getId(), verifyDoc.get().getProcessingStatus());
                    } else {
                        logger.error("[DEBUG] Document NOT found in database immediately after save - potential transaction issue");
                    }
                } catch (Exception e) {
                    logger.error("[DEBUG] Error verifying document in database: {}", e.getMessage());
                }
                
                return saved;
            });
            
            // Transaction has now committed; document is visible to other transactions
            
            // Trigger RAG ingest in background AFTER document is marked as completed
            String fileUrl = storageService.getFileUrl(filePath, userId);
            try {
                ragIngestClient.triggerDocumentIngestion(
                    savedDocument.getId(), 
                    userId, 
                    fileUrl, 
                    fileName,
                    correlationId
                );
                logger.info("[UPLOAD] RAG ingestion triggered for document: {} (document already marked as completed) | CorrelationId: {}", 
                    savedDocument.getId(), correlationId);
            } catch (Exception e) {
                // Log the error but don't fail the upload since document is already completed
                logger.warn("[UPLOAD] Failed to trigger RAG ingestion for document: {} (document still completed) | CorrelationId: {}", 
                    savedDocument.getId(), correlationId, e);
            }
            
            return toDocumentResponse(savedDocument);
            
        } catch (Exception e) {
            logger.error("[UPLOAD] Error uploading document | CorrelationId: {}", correlationId, e);
            throw new RuntimeException("Failed to upload document: " + e.getMessage());
        } finally {
            MDC.remove("correlationId");
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
    
    @Transactional
    public void markDocumentCompleted(String documentId, String ragDocumentId) {
        // Add retry logic to handle potential timing issues
        Optional<Document> documentOpt = findDocumentWithRetry(documentId, 3, 1000);
        
        if (documentOpt.isEmpty()) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }
        
        Document document = documentOpt.get();
        document.setProcessingStatus(ProcessingStatus.COMPLETED);
        document.setProcessedAt(LocalDateTime.now());
        
        if (ragDocumentId != null && !ragDocumentId.isEmpty()) {
            document.setRagDocumentId(ragDocumentId);
        }
        
        Document savedDocument = documentRepository.save(document);
        logger.info("[SUCCESS] Document {} marked as completed", documentId);
    }
    
    @Transactional
    public void markDocumentFailed(String documentId) {
        // Add retry logic to handle potential timing issues
        Optional<Document> documentOpt = findDocumentWithRetry(documentId, 3, 1000);
        
        if (documentOpt.isEmpty()) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }
        
        Document document = documentOpt.get();
        document.setProcessingStatus(ProcessingStatus.FAILED);
        document.setProcessedAt(LocalDateTime.now());
        
        documentRepository.save(document);
        logger.info("Document {} marked as failed", documentId);
    }
    
    /**
     * Find document with retry logic to handle potential timing issues
     * @param documentId The document ID to find
     * @param maxRetries Maximum number of retries
     * @param delayMs Delay between retries in milliseconds
     * @return Optional containing the document if found
     */
    private Optional<Document> findDocumentWithRetry(String documentId, int maxRetries, long delayMs) {
        Optional<Document> documentOpt = documentRepository.findById(documentId);
        
        int attempts = 0;
        while (documentOpt.isEmpty() && attempts < maxRetries) {
            attempts++;
            logger.info("Document {} not found, attempt {}/{}. Waiting {}ms before retry...", 
                documentId, attempts, maxRetries, delayMs);
            
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            
            documentOpt = documentRepository.findById(documentId);
            
            // Increase delay for next attempt (exponential backoff)
            delayMs *= 2;
        }
        
        if (documentOpt.isPresent()) {
            logger.info("Document {} found after {} attempt(s)", documentId, attempts);
        } else {
            logger.error("Document {} not found after {} attempts", documentId, maxRetries);
        }
        
        return documentOpt;
    }
    
    @Transactional
    public void clearAllDocuments() {
        List<Document> allDocuments = documentRepository.findAll();
        logger.info("Clearing {} documents", allDocuments.size());
        
        // Delete all files from storage
        for (Document document : allDocuments) {
            try {
                storageService.deleteFile(document.getFilePath());
            } catch (Exception e) {
                logger.warn("Failed to delete file for document {}: {}", document.getId(), e.getMessage());
            }
        }
        
        // Delete all documents from database
        documentRepository.deleteAll();
        logger.info("All documents cleared successfully");
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
    
    /**
     * Normalize document IDs consistently across the system
     * @param documentId The document ID to normalize
     * @return Normalized document ID (lowercase, trimmed)
     * @throws IllegalArgumentException if ID is invalid
     */
    public static String normalizeDocumentId(String documentId) {
        if (documentId == null) {
            throw new IllegalArgumentException("Document ID cannot be null");
        }
        String normalized = documentId.toLowerCase().trim();
        
        // Validate UUID format
        try {
            UUID.fromString(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format: " + documentId, e);
        }
        
        return normalized;
    }
}
