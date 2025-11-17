package com.learningassistant.user.controller;

import com.learningassistant.user.model.LearningLevel;
import com.learningassistant.user.model.User;
import com.learningassistant.user.service.UserService;
import com.learningassistant.user.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtService jwtService;
    
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {
        try {
            User user = userService.registerUser(
                registerRequest.getUsername(),
                registerRequest.getEmail(),
                registerRequest.getPassword()
            );
            
            // Generate JWT token
            String token = jwtService.generateToken(user.getUsername(), user.getId());
            
            // Create response with token and user (without password)
            AuthResponse response = new AuthResponse();
            response.setToken(token);
            
            UserResponse userResponse = new UserResponse();
            userResponse.setId(user.getId().toString());
            userResponse.setUsername(user.getUsername());
            userResponse.setEmail(user.getEmail());
            response.setUser(userResponse);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest) {
        try {
            User user = userService.authenticateUser(
                loginRequest.getUsername(),
                loginRequest.getPassword()
            );
            
            // Generate JWT token
            String token = jwtService.generateToken(user.getUsername(), user.getId());
            
            // Create response with token and user (without password)
            AuthResponse response = new AuthResponse();
            response.setToken(token);
            
            UserResponse userResponse = new UserResponse();
            userResponse.setId(user.getId().toString());
            userResponse.setUsername(user.getUsername());
            userResponse.setEmail(user.getEmail());
            response.setUser(userResponse);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.badRequest().body(error);
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
    public static class LoginRequest {
        private String username;
        private String password;
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }
    
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
    
    public static class AuthResponse {
        private String token;
        private UserResponse user;
        
        public String getToken() {
            return token;
        }
        
        public void setToken(String token) {
            this.token = token;
        }
        
        public UserResponse getUser() {
            return user;
        }
        
        public void setUser(UserResponse user) {
            this.user = user;
        }
    }
    
    public static class UserResponse {
        private String id;
        private String username;
        private String email;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
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
    }
    
    public static class ErrorResponse {
        private String message;
        
        public ErrorResponse(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
}