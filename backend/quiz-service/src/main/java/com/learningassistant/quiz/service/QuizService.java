package com.learningassistant.quiz.service;

import com.learningassistant.quiz.client.QuizGenerationClient;
import com.learningassistant.quiz.dto.*;
import com.learningassistant.quiz.model.Question;
import com.learningassistant.quiz.model.Quiz;
import com.learningassistant.quiz.model.QuizSubmission;
import com.learningassistant.quiz.repository.QuizRepository;
import com.learningassistant.quiz.repository.QuizSubmissionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizSubmissionRepository submissionRepository;
    private final QuizGenerationClient quizGenerationClient;

    public QuizService(QuizRepository quizRepository,
                       QuizSubmissionRepository submissionRepository,
                       QuizGenerationClient quizGenerationClient) {
        this.quizRepository = quizRepository;
        this.submissionRepository = submissionRepository;
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

    public QuizResult gradeQuiz(Long quizId, QuizSubmissionRequest submissionRequest) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));

        int correctAnswers = 0;
        List<QuizResult.Feedback> feedbackList = new ArrayList<>();

        for (Question question : quiz.getQuestions()) {
            String userAnswer = submissionRequest.answers().get(question.getId());
            String correctAnswer = question.getCorrectAnswer();
            boolean isCorrect = correctAnswer.equals(userAnswer);

            if (isCorrect) {
                correctAnswers++;
            }

            String explanation = isCorrect
                    ? "Correct!"
                    : "This is incorrect. The correct answer is '" + correctAnswer + "'.";

            feedbackList.add(new QuizResult.Feedback(
                    question.getId(),
                    userAnswer,
                    correctAnswer,
                    isCorrect,
                    explanation
            ));
        }

        double score = quiz.getQuestions().isEmpty()
                ? 0
                : (double) correctAnswers / quiz.getQuestions().size() * 100.0;

        QuizSubmission submission = new QuizSubmission();
        submission.setQuiz(quiz);
        submission.setUserId(submissionRequest.userId());
        submission.setAnswers(submissionRequest.answers());
        submission.setScore((int) score);

        QuizSubmission savedSubmission = submissionRepository.save(submission);

        return new QuizResult(
                quizId,
                savedSubmission.getId(),
                correctAnswers,
                quiz.getQuestions().size(),
                score,
                feedbackList
        );
    }

    public Quiz getQuizById(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
    }

    public List<QuizSubmission> getSubmissionsForUser(Long userId) {
        return submissionRepository.findByUserId(userId);
    }
}
