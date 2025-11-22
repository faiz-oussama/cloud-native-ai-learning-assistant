package com.learningassistant.quiz.dto;

/**
 * DTO describing the payload required to create a quiz from a document.
 * Supports two modes:
 * 1. documentId - fetch text from document-service (recommended)
 * 2. documentText - provide text directly (legacy/testing)
 */
public record CreateQuizRequest(
        String title,
        String documentId,      // NEW: ID of document in document-service
        String documentText,    // LEGACY: Direct text input (optional)
        Long userId
) {
}

