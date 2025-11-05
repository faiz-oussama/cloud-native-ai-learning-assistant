package com.learningassistant.chat.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class RagQueryClient {
    
    private static final Logger logger = LoggerFactory.getLogger(RagQueryClient.class);
    
    @Value("${services.rag-query.url}")
    private String ragQueryServiceUrl;
    
    private final RestTemplate restTemplate;
    
    public RagQueryClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public String queryDocument(String documentId, String question, String conversationHistory) {
        try {
            String url = ragQueryServiceUrl + "/api/query";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("documentId", documentId);
            requestBody.put("question", question);
            requestBody.put("conversationHistory", conversationHistory);
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                return (String) responseBody.get("answer");
            } else {
                logger.error("Failed to query RAG service: {}", response.getStatusCode());
                return "I apologize, but I'm having trouble processing your question at the moment. Please try again.";
            }
        } catch (Exception e) {
            logger.error("Error querying RAG service: {}", e.getMessage());
            // Fallback response when RAG service is unavailable
            return "I apologize, but I'm currently unable to answer questions about the document. The AI service may be temporarily unavailable. Please try again later.";
        }
    }
}
