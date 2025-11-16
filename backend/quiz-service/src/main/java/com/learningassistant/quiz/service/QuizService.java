package com.learningassistant.quiz.service;

import com.learningassistant.quiz.client.QuizGenerationClient;
import com.learningassistant.quiz.dto.CreateQuizRequest;
import com.learningassistant.quiz.dto.GeneratedQuestion;
import com.learningassistant.quiz.dto.QuizGenerationRequest;
import com.learningassistant.quiz.model.Question;
import com.learningassistant.quiz.model.Quiz;
import com.learningassistant.quiz.repository.QuizRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizGenerationClient quizGenerationClient;

    public QuizService(QuizRepository quizRepository, QuizGenerationClient quizGenerationClient) {
        this.quizRepository = quizRepository;
        this.quizGenerationClient = quizGenerationClient;
    }

    public Quiz createAndSaveQuiz(CreateQuizRequest request) {
        QuizGenerationRequest aiRequest = new QuizGenerationRequest(
                request.documentText(),
                5,
                request.difficulty()
        );

        List<GeneratedQuestion> generatedQuestions = quizGenerationClient.generateQuiz(aiRequest);
        List<Question> questionEntities = generatedQuestions.stream()
                .map(this::mapDtoToEntity)
                .collect(Collectors.toList());

        Quiz newQuiz = new Quiz();
        newQuiz.setTitle(request.title());
        newQuiz.setQuestions(questionEntities);

        return quizRepository.save(newQuiz);
    }

    private Question mapDtoToEntity(GeneratedQuestion dto) {
        Question entity = new Question();
        entity.setQuestionText(dto.question());
        entity.setOptions(dto.options());
        entity.setCorrectAnswer(dto.correct_answer());
        return entity;
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
