package com.learningassistant.document.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private String fileName;
    
    private String fileType;
    
    private Long fileSize;
    
    @Column(nullable = false)
    private String filePath;
    
    private String storageLocation; // local or azure
    
    @Enumerated(EnumType.STRING)
    private ProcessingStatus processingStatus;
    
    private String ragDocumentId; // Reference to RAG service document
    
    @Column(columnDefinition = "TEXT")
    private String extractedText;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;
    
    private LocalDateTime processedAt;
    
    // Constructors
    public Document() {
        this.uploadedAt = LocalDateTime.now();
        this.processingStatus = ProcessingStatus.PENDING;
    }
    
    public Document(String userId, String fileName, String fileType, Long fileSize, String filePath) {
        this.userId = userId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.filePath = filePath;
        this.uploadedAt = LocalDateTime.now();
        this.processingStatus = ProcessingStatus.PENDING;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public String getStorageLocation() {
        return storageLocation;
    }
    
    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }
    
    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }
    
    public void setProcessingStatus(ProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }
    
    public String getRagDocumentId() {
        return ragDocumentId;
    }
    
    public void setRagDocumentId(String ragDocumentId) {
        this.ragDocumentId = ragDocumentId;
    }
    
    public String getExtractedText() {
        return extractedText;
    }
    
    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }
    
    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
    
    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
    
    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
