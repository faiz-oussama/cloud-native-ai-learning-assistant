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
import org.springframework.transaction.annotation.Transactional;

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
        // Verify documents exist in document-service
        // First check if documents exist in our local database
        if (request.getDocumentIds() != null && !request.getDocumentIds().isEmpty()) {
            for (String documentId : request.getDocumentIds()) {
                Optional<ChatDocument> localDocument = documentRepository.findById(documentId);
                
                if (localDocument.isEmpty()) {
                    // Document not found locally, this is expected if upload went to document-service
                    logger.warn("Document {} not found in chat-service database. This may be normal if using document-service.", 
                        documentId);
                    // We'll allow session creation anyway - the documentId from document-service is the source of truth
                }
            }
        }
        
        String title = request.getTitle();
        if (title == null || title.trim().isEmpty()) {
            if (request.getDocumentIds() != null && !request.getDocumentIds().isEmpty()) {
                // Use the first document's name for the title
                Optional<ChatDocument> firstDocument = documentRepository.findById(request.getDocumentIds().get(0));
                if (firstDocument.isPresent()) {
                    title = "Chat about " + firstDocument.get().getFileName();
                } else {
                    title = "Chat Session";
                }
            } else {
                title = "Chat Session";
            }
        }
        
        ChatSession session = new ChatSession(
            request.getUserId(),
            request.getDocumentIds(),
            title
        );
        
        ChatSession savedSession = sessionRepository.save(session);
        logger.info("Created chat session: {} with documents: {}", savedSession.getId(), request.getDocumentIds());
        
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
        
        // For now, we'll query the RAG service with the first document
        // In a more advanced implementation, we could query multiple documents
        String documentId = session.getDocumentIds() != null && !session.getDocumentIds().isEmpty() 
            ? session.getDocumentIds().get(0) 
            : null;
            
        String answer = documentId != null 
            ? ragQueryClient.queryDocument(documentId, request.getMessage(), userId, conversationHistory)
            : "No document associated with this chat session.";
        
        // Create assistant message
        Message assistantMessage = new Message("assistant", answer);
        session.addMessage(assistantMessage);
        
        // Save session
        sessionRepository.save(session);
        
        logger.info("Processed message in session: {}", session.getId());
        
        return new ChatMessageResponse(session.getId(), userMessage, assistantMessage);
    }
    
    public List<ChatSession> getUserSessions(String userId) {
        return sessionRepository.findByUserId(userId);
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
    
    @Transactional
    public void clearAllData() {
        try {
            sessionRepository.deleteAll();
            documentRepository.deleteAll();
            logger.info("All chat data cleared successfully");
        } catch (Exception e) {
            logger.error("Error clearing chat data: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    private String buildConversationHistory(List<Message> messages) {
        // Build a formatted conversation history for context
        return messages.stream()
            .map(msg -> msg.getRole() + ": " + msg.getContent())
            .collect(Collectors.joining("\n"));
    }
}