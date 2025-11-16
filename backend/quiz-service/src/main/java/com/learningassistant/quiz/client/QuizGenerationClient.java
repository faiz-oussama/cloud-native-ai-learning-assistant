package com.learningassistant.quiz.client;

import com.learningassistant.quiz.dto.GeneratedQuestion;
import com.learningassistant.quiz.dto.QuizGenerationRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QuizGenerationClient {

    public List<GeneratedQuestion> generateQuiz(QuizGenerationRequest request) {
        GeneratedQuestion q1 = new GeneratedQuestion(
                "Mock Question from Client (Difficulty: " + request.difficulty() + ") - Q1",
                List.of("Client-A", "Client-B", "Client-C", "Client-D"),
                "Client-A"
        );

        GeneratedQuestion q2 = new GeneratedQuestion(
                "Mock Question from Client - Q2",
                List.of("Client-A", "Client-B", "Client-C", "Client-D"),
                "Client-B"
        );

        return List.of(q1, q2);
    }
}

