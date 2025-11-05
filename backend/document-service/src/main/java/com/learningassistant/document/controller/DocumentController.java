package com.learningassistant.document.controller;

import com.learningassistant.document.dto.DocumentResponse;
import com.learningassistant.document.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
