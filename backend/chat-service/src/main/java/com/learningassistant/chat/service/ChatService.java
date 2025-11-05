package com.learningassistant.chat.service;

import com.learningassistant.chat.client.RagQueryClient;
import com.learningassistant.chat.dto.ChatMessageRequest;
import com.learningassistant.chat.dto.ChatMessageResponse;
import com.learningassistant.chat.dto.CreateSessionRequest;
import com.learningassistant.chat.model.ChatDocument;
import com.learningassistant.chat.model.ChatSession;
import com.learningassistant.chat.model.Message;
import com.learningassistant.chat.repository.ChatDocumentRepository;
import com.learningassistant.chat.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    
    private final ChatSessionRepository sessionRepository;
    private final ChatDocumentRepository documentRepository;
    private final RagQueryClient ragQueryClient;
    
    public ChatService(ChatSessionRepository sessionRepository,
                      ChatDocumentRepository documentRepository,
                      RagQueryClient ragQueryClient) {
        this.sessionRepository = sessionRepository;
        this.documentRepository = documentRepository;
        this.ragQueryClient = ragQueryClient;
    }
    
    public ChatSession createSession(CreateSessionRequest request) {
        // Verify document exists
        Optional<ChatDocument> document = documentRepository.findById(request.getDocumentId());
        if (document.isEmpty()) {
            throw new IllegalArgumentException("Document not found with ID: " + request.getDocumentId());
        }
        
        String title = request.getTitle();
        if (title == null || title.trim().isEmpty()) {
            title = "Chat about " + document.get().getFileName();
        }
        
        ChatSession session = new ChatSession(
            request.getUserId(),
            request.getDocumentId(),
            title
        );
        
        ChatSession savedSession = sessionRepository.save(session);
        logger.info("Created chat session: {} for document: {}", savedSession.getId(), request.getDocumentId());
        
        return savedSession;
    }
    
    public ChatMessageResponse sendMessage(ChatMessageRequest request, String userId) {
        // Get session
        Optional<ChatSession> sessionOpt = sessionRepository.findByIdAndUserId(request.getSessionId(), userId);
        if (sessionOpt.isEmpty()) {
            throw new IllegalArgumentException("Session not found or access denied");
        }
        
        ChatSession session = sessionOpt.get();
        
        // Create user message
        Message userMessage = new Message("user", request.getMessage());
        session.addMessage(userMessage);
        
        // Build conversation history
        String conversationHistory = buildConversationHistory(session.getMessages());
        
        // Query RAG service for answer
        String answer = ragQueryClient.queryDocument(
            session.getDocumentId(),
            request.getMessage(),
            userId,
            conversationHistory
        );
        
        // Create assistant message
        Message assistantMessage = new Message("assistant", answer);
        session.addMessage(assistantMessage);
        
        // Save session
        sessionRepository.save(session);
        
        logger.info("Processed message in session: {}", session.getId());
        
        return new ChatMessageResponse(session.getId(), userMessage, assistantMessage);
    }
    
    public List<ChatSession> getUserSessions(String userId) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }
    
    public Optional<ChatSession> getSessionById(String sessionId, String userId) {
        return sessionRepository.findByIdAndUserId(sessionId, userId);
    }
    
    public void deleteSession(String sessionId, String userId) {
        Optional<ChatSession> session = sessionRepository.findByIdAndUserId(sessionId, userId);
        if (session.isPresent()) {
            sessionRepository.delete(session.get());
            logger.info("Deleted session: {}", sessionId);
        } else {
            throw new IllegalArgumentException("Session not found or access denied");
        }
    }
    
    private String buildConversationHistory(List<Message> messages) {
        // Build a formatted conversation history for context
        return messages.stream()
            .map(msg -> msg.getRole() + ": " + msg.getContent())
            .collect(Collectors.joining("\n"));
    }
}
