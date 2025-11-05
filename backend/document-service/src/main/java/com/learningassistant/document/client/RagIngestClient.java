package com.learningassistant.document.client;

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
public class RagIngestClient {
    
    private static final Logger logger = LoggerFactory.getLogger(RagIngestClient.class);
    
    @Value("${services.rag-ingest.url}")
    private String ragIngestServiceUrl;
    
    private final RestTemplate restTemplate;
    
    public RagIngestClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public Map<String, Object> ingestDocument(String documentId, MultipartFile file) {
        try {
            String url = ragIngestServiceUrl + "/api/ingest";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
            body.add("documentId", documentId);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Document {} ingested successfully into RAG service", documentId);
                return response.getBody();
            } else {
                logger.error("Failed to ingest document: {}", response.getStatusCode());
                return null;
            }
        } catch (IOException e) {
            logger.error("Error reading file for ingestion: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Error ingesting document to RAG service: {}", e.getMessage());
            return null;
        }
    }
    
    public boolean deleteDocument(String documentId) {
        try {
            String url = ragIngestServiceUrl + "/api/ingest/" + documentId;
            restTemplate.delete(url);
            logger.info("Document {} deleted from RAG service", documentId);
            return true;
        } catch (Exception e) {
            logger.error("Error deleting document from RAG service: {}", e.getMessage());
            return false;
        }
    }
}
