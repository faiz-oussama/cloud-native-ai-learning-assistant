package com.learningassistant.document.dto;

import com.learningassistant.document.model.ProcessingStatus;
import java.time.LocalDateTime;

public class DocumentResponse {
    
    private String documentId;
    private String userId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private ProcessingStatus processingStatus;
    private LocalDateTime uploadedAt;
    private LocalDateTime processedAt;
    
    // Constructors
    public DocumentResponse() {
    }
    
    public DocumentResponse(String documentId, String userId, String fileName, String fileType, 
                          Long fileSize, ProcessingStatus processingStatus, 
                          LocalDateTime uploadedAt, LocalDateTime processedAt) {
        this.documentId = documentId;
        this.userId = userId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.processingStatus = processingStatus;
        this.uploadedAt = uploadedAt;
        this.processedAt = processedAt;
    }
    
    // Getters and Setters
    public String getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
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
    
    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }
    
    public void setProcessingStatus(ProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }
    
    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
    
    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
    
    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
