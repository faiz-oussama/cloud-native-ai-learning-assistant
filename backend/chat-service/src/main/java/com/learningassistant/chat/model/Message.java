package com.learningassistant.chat.model;

import java.time.LocalDateTime;

public class Message {
    
    private String id;
    private String role; // "user" or "assistant"
    private String content;
    private LocalDateTime timestamp;
    
    // Constructors
    public Message() {
        this.timestamp = LocalDateTime.now();
    }
    
    public Message(String role, String content) {
        this.id = java.util.UUID.randomUUID().toString();
        this.role = role;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
