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
    
    public Map<String, Object> triggerIndexer(String documentId, String userId) {
        try {
            String url = ragIngestServiceUrl + "/ingest";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("document_id", documentId);
            requestBody.put("user_id", userId);
            requestBody.put("container_name", "documents");
            requestBody.put("trigger_indexer", true);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Indexer triggered successfully for document {}", documentId);
                return response.getBody();
            } else {
                logger.error("Failed to trigger indexer: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            logger.error("Error triggering indexer: {}", e.getMessage());
            return null;
        }
    }
    
    public Map<String, Object> triggerDocumentIngestion(String documentId, String userId, String documentPath, String documentTitle, String correlationId) {
        try {
            String url = ragIngestServiceUrl + "/ingest-docling";
            
            logger.info("[CRITICAL] Triggering document ingestion | CorrelationId: {}", correlationId);
            logger.info("[CRITICAL] RAG_INGEST_URL (ragIngestServiceUrl): {}", ragIngestServiceUrl);
            logger.info("[CRITICAL] Full ingest URL: {}", url);
            logger.info("[CRITICAL] Document ID: {}, User ID: {} | CorrelationId: {}", documentId, userId, correlationId);
            logger.info("[CRITICAL] Document URL: {}", documentPath);
            logger.info("[CRITICAL] Document Title: {}", documentTitle);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("document_id", documentId);
            requestBody.put("user_id", userId);
            requestBody.put("document_url", documentPath);
            requestBody.put("document_title", documentTitle);
            requestBody.put("correlation_id", correlationId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Correlation-ID", correlationId);
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            logger.info("[CRITICAL] Making HTTP POST request to RAG ingest service | CorrelationId: {}", correlationId);
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );
            
            logger.info("[CRITICAL] RAG ingest service response status: {} | CorrelationId: {}", response.getStatusCode(), correlationId);
            logger.info("[CRITICAL] RAG ingest service response body: {}", response.getBody());
            
            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                logger.info("[SUCCESS] Document ingestion triggered successfully for document {} | CorrelationId: {}", documentId, correlationId);
                return response.getBody();
            } else {
                logger.error("[ERROR] Failed to trigger document ingestion: {} | CorrelationId: {}", response.getStatusCode(), correlationId);
                return null;
            }
        } catch (Exception e) {
            logger.error("[ERROR] Exception while triggering document ingestion | CorrelationId: {}", correlationId, e);
            logger.error("[ERROR] Root cause: {}", e.getCause());
            return null;
        }
    }
}
