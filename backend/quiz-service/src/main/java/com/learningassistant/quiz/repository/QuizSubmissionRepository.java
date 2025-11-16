package com.learningassistant.quiz.repository;

import com.learningassistant.quiz.model.QuizSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Long> {
}

