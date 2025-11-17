package com.learningassistant.document.controller;

import com.learningassistant.document.dto.DocumentResponse;
import com.learningassistant.document.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
    
    private final DocumentService documentService;
    
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId) {
        try {
            logger.info("Received document upload request from user: {}", userId);
            
            DocumentResponse response = documentService.uploadDocument(file, userId);
            
            // Return response compatible with chat-service expectations
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("documentId", response.getDocumentId());
            responseMap.put("fileName", response.getFileName());
            responseMap.put("fileType", response.getFileType());
            responseMap.put("fileSize", response.getFileSize());
            responseMap.put("uploadedAt", response.getUploadedAt());
            responseMap.put("processingStatus", response.getProcessingStatus());
            responseMap.put("message", "Document uploaded successfully");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(responseMap);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error uploading document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to upload document: " + e.getMessage()));
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserDocuments(@PathVariable String userId) {
        try {
            List<DocumentResponse> documents = documentService.getUserDocuments(userId);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            logger.error("Error fetching user documents: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch documents"));
        }
    }
    
    @GetMapping("/{documentId}")
    public ResponseEntity<?> getDocument(@PathVariable String documentId) {
        try {
            return documentService.getDocumentById(documentId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error fetching document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch document"));
        }
    }
    
    @DeleteMapping("/{documentId}")
    public ResponseEntity<?> deleteDocument(@PathVariable String documentId) {
        try {
            documentService.deleteDocument(documentId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Document deleted successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to delete document"));
        }
    }
    
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingDocuments() {
        try {
            List<DocumentResponse> documents = documentService.getPendingDocuments();
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            logger.error("Error fetching pending documents: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch pending documents"));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "document-service");
        return ResponseEntity.ok(health);
    }
    
    @GetMapping("/{documentId}/status")
    public ResponseEntity<?> getDocumentStatus(@PathVariable String documentId) {
        try {
            logger.info("[CRITICAL] Received status request for document: {}", documentId);
            Optional<DocumentResponse> doc = documentService.getDocumentById(documentId);
            if (doc.isEmpty()) {
                logger.warn("[WARNING] Document not found: {}", documentId);
                return ResponseEntity.notFound().build();
            }
            
            logger.info("[CRITICAL] Document {} status: {}", documentId, doc.get().getProcessingStatus());
            Map<String, Object> response = new HashMap<>();
            response.put("documentId", doc.get().getDocumentId());
            response.put("processingStatus", doc.get().getProcessingStatus());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("[ERROR] Error checking document status for {}: {}", documentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to check document status"));
        }
    }
    
    @PostMapping("/{documentId}/check-status")
    public ResponseEntity<?> checkDocumentStatus(@PathVariable String documentId) {
        try {
            Optional<DocumentResponse> doc = documentService.getDocumentById(documentId);
            if (doc.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("documentId", doc.get().getDocumentId());
            response.put("fileName", doc.get().getFileName());
            response.put("processingStatus", doc.get().getProcessingStatus());
            response.put("uploadedAt", doc.get().getUploadedAt());
            response.put("processedAt", doc.get().getProcessedAt());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error checking document status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to check document status"));
        }
    }
    
    @PostMapping("/{documentId}/mark-completed")
    public ResponseEntity<?> markDocumentCompleted(
            @PathVariable String documentId,
            @RequestParam(required = false) String ragDocumentId,
            HttpServletRequest request) {
        try {
            // Normalize document ID to ensure consistency with database storage
            String normalizedDocumentId = DocumentService.normalizeDocumentId(documentId);
            
            logger.info("[DEBUG] === Document Completion Endpoint Hit ===");
            logger.info("[DEBUG] Request URL: {}", request.getRequestURL());
            logger.info("[DEBUG] Request Method: {}", request.getMethod());
            logger.info("[DEBUG] Path Variable documentId: '{}'", documentId);
            logger.info("[DEBUG] Normalized documentId: '{}'", normalizedDocumentId);
            logger.info("[DEBUG] documentId length: {}", normalizedDocumentId.length());
            logger.info("[DEBUG] Query Parameter ragDocumentId: {}", ragDocumentId);
            
            // Log all headers
            request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
                logger.info("[DEBUG] Header - {}: {}", headerName, request.getHeader(headerName));
            });
            
            logger.info("[CRITICAL] Received mark-completed request for document: {}", normalizedDocumentId);
            logger.info("[CRITICAL] ragDocumentId parameter: {}", ragDocumentId);
            
            // Try to find the document BEFORE attempting to mark it complete
            Optional<DocumentResponse> existingDoc = documentService.getDocumentById(normalizedDocumentId);
            if (existingDoc.isEmpty()) {
                logger.error("[ERROR] Document NOT FOUND in database before mark-completed: {}", normalizedDocumentId);
                logger.error("[ERROR] This indicates a database mismatch or timing issue");
                // Log all documents to help debug
                logger.info("[DEBUG] Attempting to query database for similar IDs...");
            } else {
                logger.info("[SUCCESS] Document FOUND in database before mark-completed: {}", normalizedDocumentId);
                logger.info("[DEBUG] Current status: {}", existingDoc.get().getProcessingStatus());
            }
            
            documentService.markDocumentCompleted(normalizedDocumentId, ragDocumentId);
            logger.info("[SUCCESS] Document {} successfully marked as COMPLETED in database", normalizedDocumentId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Document marked as completed");
            response.put("documentId", normalizedDocumentId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("[ERROR] Invalid request for document {}: {}", documentId, e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("[ERROR] Failed to mark document {} as completed: {}", documentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to mark document as completed"));
        }
    }
    
    @PostMapping("/{documentId}/mark-failed")
    public ResponseEntity<?> markDocumentFailed(@PathVariable String documentId) {
        try {
            // Normalize document ID to ensure consistency with database storage
            String normalizedDocumentId = DocumentService.normalizeDocumentId(documentId);
            
            logger.info("[CRITICAL] Received mark-failed request for document: {}", normalizedDocumentId);
            documentService.markDocumentFailed(normalizedDocumentId);
            logger.info("[SUCCESS] Document {} successfully marked as FAILED in database", normalizedDocumentId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Document marked as failed");
            response.put("documentId", normalizedDocumentId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("[ERROR] Invalid request for document {}: {}", documentId, e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("[ERROR] Failed to mark document {} as failed: {}", documentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to mark document as failed"));
        }
    }
    
    @DeleteMapping("/admin/clear-all")
    public ResponseEntity<?> clearAllDocuments() {
        try {
            documentService.clearAllDocuments();
            Map<String, String> response = new HashMap<>();
            response.put("message", "All documents cleared successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error clearing documents: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to clear documents"));
        }
    }
    
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
