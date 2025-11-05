package com.learningassistant.chat.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "documents")
public class ChatDocument {
    
    @Id
    private String id;
    private String userId;
    private String fileName;
    private String fileType;
    private String filePath;
    private Long fileSize;
    private String documentServiceId; // Reference to document-service
    private LocalDateTime uploadedAt;
    
    // Constructors
    public ChatDocument() {
        this.uploadedAt = LocalDateTime.now();
    }
    
    public ChatDocument(String userId, String fileName, String fileType, String filePath, Long fileSize) {
        this.userId = userId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.uploadedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
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
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getDocumentServiceId() {
        return documentServiceId;
    }
    
    public void setDocumentServiceId(String documentServiceId) {
        this.documentServiceId = documentServiceId;
    }
    
    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
    
    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
