package com.learningassistant.quiz.controller;

import com.learningassistant.quiz.model.Quiz;
import com.learningassistant.quiz.service.QuizService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/test")
    public Quiz getTestQuiz() {
        return quizService.createTestQuiz();
    }
}

