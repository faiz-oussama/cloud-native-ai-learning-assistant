package com.learningassistant.quiz.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client to communicate with the document-service
 */
@Component
public class DocumentServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentServiceClient.class);
    
    private final RestTemplate restTemplate;
    
    @Value("${services.document-service.url}")
    private String documentServiceUrl;
    
    public DocumentServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Fetch the full text content of a document by its ID
     */
    public String getDocumentText(String documentId) {
        try {
            String url = documentServiceUrl + "/api/documents/" + documentId + "/text";
            logger.info("Fetching document text from: {}", url);
            
            // Call document-service endpoint
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response == null || !response.containsKey("text")) {
                throw new RuntimeException("Invalid response from document-service");
            }
            
            String text = (String) response.get("text");
            logger.info("Retrieved {} characters from document: {}", text.length(), documentId);
            
            return text;
            
        } catch (Exception e) {
            logger.error("Error fetching document text for {}: {}", documentId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch document text: " + e.getMessage(), e);
        }
    }
}

