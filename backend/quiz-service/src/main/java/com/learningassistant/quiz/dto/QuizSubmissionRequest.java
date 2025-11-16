package com.learningassistant.quiz.dto;

import java.util.Map;

public record QuizSubmissionRequest(
        Long userId,
        Map<Long, String> answers
) {
}

