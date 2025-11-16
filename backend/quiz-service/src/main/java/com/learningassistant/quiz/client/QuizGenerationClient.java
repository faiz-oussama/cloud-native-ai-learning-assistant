package com.learningassistant.quiz.client;

import com.learningassistant.quiz.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Component
public class QuizGenerationClient {

    private final RestTemplate restTemplate;
    private final String quizGenerationServiceUrl;

    public QuizGenerationClient(RestTemplate restTemplate,
                                @Value("${services.quiz-generation.url}") String quizGenerationServiceUrl) {
        this.restTemplate = restTemplate;
        this.quizGenerationServiceUrl = quizGenerationServiceUrl;
    }

    public List<GeneratedQuestion> generateQuiz(QuizGenerationRequest request) {
        String url = quizGenerationServiceUrl + "/generate-quiz";
        GeneratedQuestion[] response = restTemplate.postForObject(url, request, GeneratedQuestion[].class);
        return response == null ? List.of() : Arrays.asList(response);
    }

    public ExplanationResponse getExplanation(ExplanationRequest request) {
        String url = quizGenerationServiceUrl + "/get-explanation";
        return restTemplate.postForObject(url, request, ExplanationResponse.class);
    }
}
