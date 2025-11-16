package com.learningassistant.quiz.service;

import com.learningassistant.quiz.model.Question;
import com.learningassistant.quiz.model.Quiz;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuizService {

    /**
     * Temporary mock logic that returns a hard-coded quiz.
     * This will be replaced with real generation logic in later tasks.
     */
    public Quiz createTestQuiz() {
        Question mathQuestion = new Question();
        mathQuestion.setQuestionText("What is 2 + 2?");
        mathQuestion.setOptions(List.of("3", "4", "5", "6"));
        mathQuestion.setCorrectAnswer("4");

        Question capitalQuestion = new Question();
        capitalQuestion.setQuestionText("What is the capital of France?");
        capitalQuestion.setOptions(List.of("London", "Berlin", "Paris", "Rome"));
        capitalQuestion.setCorrectAnswer("Paris");

        Quiz quiz = new Quiz();
        quiz.setId(1L);
        quiz.setTitle("Test Quiz");
        quiz.setQuestions(List.of(mathQuestion, capitalQuestion));

        return quiz;
    }
}

