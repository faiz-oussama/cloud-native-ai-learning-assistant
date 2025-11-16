package com.learningassistant.quiz.controller;

import com.learningassistant.quiz.model.Question;
import com.learningassistant.quiz.model.Quiz;
import com.learningassistant.quiz.service.QuizService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = QuizController.class)
class QuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuizService quizService;

    private Quiz mockQuiz;

    @BeforeEach
    void setUp() {
        Question q1 = new Question();
        q1.setQuestionText("What is 2 + 2?");
        q1.setOptions(List.of("3", "4", "5", "6"));
        q1.setCorrectAnswer("4");

        Question q2 = new Question();
        q2.setQuestionText("What is the capital of France?");
        q2.setOptions(List.of("London", "Berlin", "Paris", "Rome"));
        q2.setCorrectAnswer("Paris");

        mockQuiz = new Quiz();
        mockQuiz.setId(1L);
        mockQuiz.setTitle("Test Quiz");
        mockQuiz.setQuestions(List.of(q1, q2));

        when(quizService.createTestQuiz()).thenReturn(mockQuiz);
    }

    @Test
    void testGetTestQuiz_returnsMockQuiz() throws Exception {
        mockMvc.perform(get("/api/quizzes/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Quiz"))
                .andExpect(jsonPath("$.questions.length()").value(2))
                .andExpect(jsonPath("$.questions[0].questionText").value("What is 2 + 2?"))
                .andExpect(jsonPath("$.questions[1].correctAnswer").value("Paris"));
    }
}
