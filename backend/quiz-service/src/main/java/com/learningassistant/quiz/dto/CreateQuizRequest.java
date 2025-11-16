package com.learningassistant.quiz.dto;

/**
 * DTO describing the payload required to create a quiz from a document.
 */
public record CreateQuizRequest(
        String title,
        String documentText,
        Long userId
) {
}

