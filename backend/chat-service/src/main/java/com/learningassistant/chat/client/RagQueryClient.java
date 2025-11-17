package com.learningassistant.chat.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
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
    
    public String queryDocument(String documentId, String question, String userId, String conversationHistory) {
        return queryDocuments(List.of(documentId), question, userId, conversationHistory);
    }
    
    public String queryDocuments(List<String> documentIds, String question, String userId, String conversationHistory) {
        try {
            String url = ragQueryServiceUrl + "/query";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", question);
            requestBody.put("user_id", userId);
            
            // For now, we'll use the first document ID for querying
            // In a more advanced implementation, we could query multiple documents
            if (documentIds != null && !documentIds.isEmpty()) {
                requestBody.put("document_id", documentIds.get(0));
            }
            
            requestBody.put("top_k", 5);
            requestBody.put("temperature", 0.7);
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String answer = (String) responseBody.get("answer");
                
                logger.info("RAG query successful for documents: {}", documentIds);
                return answer != null ? answer : "I couldn't generate an answer from the document.";
            } else {
                logger.error("Failed to query RAG service: {}", response.getStatusCode());
                return "I apologize, but I'm having trouble processing your question at the moment. Please try again.";
            }
        } catch (Exception e) {
            logger.error("Error querying RAG service: {}", e.getMessage(), e);
            // Fallback response when RAG service is unavailable
            return "I apologize, but I'm currently unable to answer questions about the document. The AI service may be temporarily unavailable. Please try again later.";
        }
    }
}