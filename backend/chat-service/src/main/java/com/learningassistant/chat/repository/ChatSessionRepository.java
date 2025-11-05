package com.learningassistant.chat.repository;

import com.learningassistant.chat.model.ChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {
    List<ChatSession> findByUserId(String userId);
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(String userId);
    Optional<ChatSession> findByIdAndUserId(String id, String userId);
    List<ChatSession> findByDocumentId(String documentId);
}
