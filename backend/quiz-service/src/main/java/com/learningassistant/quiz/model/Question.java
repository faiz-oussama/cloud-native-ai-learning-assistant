package com.learningassistant.quiz.model;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Entity
@Data
@AllArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String questionText;

    @ElementCollection // Tells JPA this is a simple list of strings
    private List<String> options;

    private String correctAnswer;

    // --- Constructors, Getters, and Setters ---
    // (JPA needs a no-arg constructor)

    public Question() {}

}
