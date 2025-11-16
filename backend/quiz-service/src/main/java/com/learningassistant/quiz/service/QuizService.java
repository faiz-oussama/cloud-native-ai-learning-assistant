package com.learningassistant.quiz.service;

import com.learningassistant.quiz.dto.CreateQuizRequest;
import com.learningassistant.quiz.model.Question;
import com.learningassistant.quiz.model.Quiz;
import com.learningassistant.quiz.repository.QuizRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuizService {

    private final QuizRepository quizRepository;

    public QuizService(QuizRepository quizRepository) {
        this.quizRepository = quizRepository;
    }

    public Quiz createAndSaveQuiz(CreateQuizRequest request) {
        List<Question> generatedQuestions = generateMockQuestions(request.difficulty());

        Quiz newQuiz = new Quiz();
        newQuiz.setTitle(request.title());
        newQuiz.setQuestions(generatedQuestions);

        return quizRepository.save(newQuiz);
    }

    private List<Question> generateMockQuestions(String difficulty) {
        Question q1 = new Question();
        q1.setQuestionText("Mock Question (Difficulty: " + difficulty + ") - Q1");
        q1.setOptions(List.of("A", "B", "C", "D"));
        q1.setCorrectAnswer("A");

        Question q2 = new Question();
        q2.setQuestionText("Mock Question - Q2");
        q2.setOptions(List.of("A", "B", "C", "D"));
        q2.setCorrectAnswer("B");

        return List.of(q1, q2);
    }

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
