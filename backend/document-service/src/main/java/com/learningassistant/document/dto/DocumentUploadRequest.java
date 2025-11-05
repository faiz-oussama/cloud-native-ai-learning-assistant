package com.learningassistant.document.dto;

public class DocumentUploadRequest {
    
    private String userId;
    
    // Constructors
    public DocumentUploadRequest() {
    }
    
    public DocumentUploadRequest(String userId) {
        this.userId = userId;
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
}
