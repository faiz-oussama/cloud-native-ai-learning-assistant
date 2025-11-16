package com.learningassistant.quiz.dto;

public record ExplanationRequest(
        String context,
        String question,
        String wrongAnswer
) {
}

