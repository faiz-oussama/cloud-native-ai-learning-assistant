package com.learningassistant.quiz.dto;

/**
 * Payload sent to the Python quiz-generation service.
 */
public record QuizGenerationRequest(
        String text_content,
        int num_questions,
        String difficulty
) {
}

