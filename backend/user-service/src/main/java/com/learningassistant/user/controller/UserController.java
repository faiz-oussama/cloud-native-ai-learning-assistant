package com.learningassistant.user.controller;

import com.learningassistant.user.model.LearningLevel;
import com.learningassistant.user.model.User;
import com.learningassistant.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@RequestBody RegisterRequest registerRequest) {
        try {
            User user = userService.registerUser(
                registerRequest.getUsername(),
                registerRequest.getEmail(),
                registerRequest.getPassword()
            );
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        Optional<User> user = userService.findByUsername(username);
        return user.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{userId}/learning-level")
    public ResponseEntity<User> updateUserLearningLevel(
            @PathVariable Long userId,
            @RequestBody UpdateLearningLevelRequest request) {
        try {
            User user = userService.updateUserLearningLevel(userId, request.getLearningLevel());
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // DTOs
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        
        // Getters and setters
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }
    
    public static class UpdateLearningLevelRequest {
        private LearningLevel learningLevel;
        
        // Getters and setters
        public LearningLevel getLearningLevel() {
            return learningLevel;
        }
        
        public void setLearningLevel(LearningLevel learningLevel) {
            this.learningLevel = learningLevel;
        }
    }
}