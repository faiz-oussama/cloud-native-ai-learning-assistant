package com.learningassistant.chat.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class DocumentServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentServiceClient.class);
    
    @Value("${services.document.url}")
    private String documentServiceUrl;
    
    private final RestTemplate restTemplate;
    
    public DocumentServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public Map<String, Object> uploadDocument(MultipartFile file, String userId) {
        try {
            String url = documentServiceUrl + "/api/documents/upload";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
            body.add("userId", userId);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Document uploaded successfully to document-service");
                return response.getBody();
            } else {
                logger.error("Failed to upload document: {}", response.getStatusCode());
                return null;
            }
        } catch (IOException e) {
            logger.error("Error reading file: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Error uploading document to document-service: {}", e.getMessage());
            return null;
        }
    }
    
    public Map<String, Object> getDocumentById(String documentId) {
        try {
            String url = documentServiceUrl + "/api/documents/" + documentId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }
            return null;
        } catch (Exception e) {
            logger.error("Error fetching document from document-service: {}", e.getMessage());
            return null;
        }
    }
}
