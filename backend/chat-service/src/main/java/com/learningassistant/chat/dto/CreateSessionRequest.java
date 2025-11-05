package com.learningassistant.chat.dto;

public class CreateSessionRequest {
    
    private String userId;
    private String documentId;
    private String title;
    
    // Constructors
    public CreateSessionRequest() {
    }
    
    public CreateSessionRequest(String userId, String documentId, String title) {
        this.userId = userId;
        this.documentId = documentId;
        this.title = title;
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
}
