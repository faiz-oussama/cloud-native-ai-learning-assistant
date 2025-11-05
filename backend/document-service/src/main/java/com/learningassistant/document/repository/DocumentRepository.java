package com.learningassistant.document.repository;

import com.learningassistant.document.model.Document;
import com.learningassistant.document.model.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {
    List<Document> findByUserId(String userId);
    List<Document> findByUserIdOrderByUploadedAtDesc(String userId);
    List<Document> findByProcessingStatus(ProcessingStatus status);
}
