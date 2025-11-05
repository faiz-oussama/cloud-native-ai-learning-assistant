package com.learningassistant.chat.repository;

import com.learningassistant.chat.model.ChatDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatDocumentRepository extends MongoRepository<ChatDocument, String> {
    List<ChatDocument> findByUserId(String userId);
    List<ChatDocument> findByUserIdOrderByUploadedAtDesc(String userId);
}
