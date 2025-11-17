package com.learningassistant.chat.controller;

import com.learningassistant.chat.dto.ChatMessageRequest;
import com.learningassistant.chat.dto.ChatMessageResponse;
import com.learningassistant.chat.dto.CreateSessionRequest;
import com.learningassistant.chat.model.ChatSession;
import com.learningassistant.chat.service.ChatService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    private final ChatService chatService;
    
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }
    
    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(@Valid @RequestBody CreateSessionRequest request) {
        try {
            logger.info("Creating chat session for user: {} with documents: {}", 
                       request.getUserId(), request.getDocumentIds());
            ChatSession session = chatService.createSession(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(session);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create session: " + e.getMessage());
        }
    }
    
    @PostMapping("/messages")
    public ResponseEntity<?> sendMessage(
            @Valid @RequestBody ChatMessageRequest request,
            @RequestParam String userId) {
        try {
            logger.info("Processing message for session: {} from user: {}", 
                       request.getSessionId(), userId);
            ChatMessageResponse response = chatService.sendMessage(request, userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing message: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process message: " + e.getMessage());
        }
    }
    
    @GetMapping("/sessions/user/{userId}")
    public ResponseEntity<List<ChatSession>> getUserSessions(@PathVariable String userId) {
        try {
            List<ChatSession> sessions = chatService.getUserSessions(userId);
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            logger.error("Error fetching user sessions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<?> getSession(
            @PathVariable String sessionId,
            @RequestParam String userId) {
        try {
            return chatService.getSessionById(sessionId, userId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error fetching session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> deleteSession(
            @PathVariable String sessionId,
            @RequestParam String userId) {
        try {
            chatService.deleteSession(sessionId, userId);
            return ResponseEntity.ok("Session deleted successfully");
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error deleting session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete session");
        }
    }
    
    @DeleteMapping("/admin/clear-all")
    public ResponseEntity<?> clearAllData() {
        try {
            chatService.clearAllData();
            Map<String, String> response = new HashMap<>();
            response.put("message", "All chat sessions and messages cleared successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error clearing data: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to clear data: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }
}