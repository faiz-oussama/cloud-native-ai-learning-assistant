package com.learningassistant.chat.dto;

import java.time.LocalDateTime;

public class DocumentUploadResponse {
    
    private String documentId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private LocalDateTime uploadedAt;
    private String message;
    
    // Constructors
    public DocumentUploadResponse() {
    }
    
    public DocumentUploadResponse(String documentId, String fileName, String fileType, 
                                 Long fileSize, LocalDateTime uploadedAt, String message) {
        this.documentId = documentId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uploadedAt = uploadedAt;
        this.message = message;
    }
    
    // Getters and Setters
    public String getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
    
    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
