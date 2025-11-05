package com.learningassistant.chat.dto;

import com.learningassistant.chat.model.Message;

public class ChatMessageResponse {
    
    private String sessionId;
    private Message userMessage;
    private Message assistantMessage;
    
    // Constructors
    public ChatMessageResponse() {
    }
    
    public ChatMessageResponse(String sessionId, Message userMessage, Message assistantMessage) {
        this.sessionId = sessionId;
        this.userMessage = userMessage;
        this.assistantMessage = assistantMessage;
    }
    
    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public Message getUserMessage() {
        return userMessage;
    }
    
    public void setUserMessage(Message userMessage) {
        this.userMessage = userMessage;
    }
    
    public Message getAssistantMessage() {
        return assistantMessage;
    }
    
    public void setAssistantMessage(Message assistantMessage) {
        this.assistantMessage = assistantMessage;
    }
}
