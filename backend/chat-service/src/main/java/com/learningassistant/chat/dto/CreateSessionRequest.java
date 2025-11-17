package com.learningassistant.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class CreateSessionRequest {
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotNull(message = "Document IDs are required")
    private List<String> documentIds; // Changed from single documentId to list of documentIds
    
    private String title;
    
    // Constructors
    public CreateSessionRequest() {
    }
    
    public CreateSessionRequest(String userId, List<String> documentIds, String title) {
        this.userId = userId;
        this.documentIds = documentIds;
        this.title = title;
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public List<String> getDocumentIds() {
        return documentIds;
    }
    
    public void setDocumentIds(List<String> documentIds) {
        this.documentIds = documentIds;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
}