package com.learningassistant.quiz.dto;

import java.util.List;

public record QuizResult(
        Long quizId,
        Long submissionId,
        int correctAnswers,
        int totalQuestions,
        double score,
        List<Feedback> feedback
) {
    public record Feedback(
            Long questionId,
            String yourAnswer,
            String correctAnswer,
            boolean isCorrect,
            String explanation
    ) {
    }
}

