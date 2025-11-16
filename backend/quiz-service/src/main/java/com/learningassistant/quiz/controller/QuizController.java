package com.learningassistant.quiz.controller;

import com.learningassistant.quiz.dto.CreateQuizRequest;
import com.learningassistant.quiz.dto.QuizResult;
import com.learningassistant.quiz.dto.QuizSubmissionRequest;
import com.learningassistant.quiz.model.Quiz;
import com.learningassistant.quiz.service.QuizService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Quiz createQuiz(@RequestBody CreateQuizRequest request) {
        return quizService.createAndSaveQuiz(request);
    }

    @PostMapping("/{id}/submit")
    public QuizResult submitQuiz(@PathVariable("id") Long id, @RequestBody QuizSubmissionRequest submission) {
        return quizService.gradeQuiz(id, submission);
    }

    @GetMapping("/test")
    public Quiz getTestQuiz() {
        return quizService.createTestQuiz();
    }
}
