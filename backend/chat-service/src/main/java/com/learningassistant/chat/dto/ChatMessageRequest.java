package com.learningassistant.chat.dto;

public class ChatMessageRequest {
    
    private String sessionId;
    private String message;
    
    // Constructors
    public ChatMessageRequest() {
    }
    
    public ChatMessageRequest(String sessionId, String message) {
        this.sessionId = sessionId;
        this.message = message;
    }
    
    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
