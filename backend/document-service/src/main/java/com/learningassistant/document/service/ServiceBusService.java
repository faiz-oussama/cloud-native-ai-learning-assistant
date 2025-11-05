package com.learningassistant.document.service;

import com.azure.messaging.servicebus.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;

@Service
public class ServiceBusService {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceBusService.class);
    
    @Value("${service-bus.connection-string}")
    private String connectionString;
    
    @Value("${service-bus.queue-name}")
    private String queueName;
    
    private ServiceBusSenderClient senderClient;
    
    @PostConstruct
    public void init() {
        try {
            if (connectionString != null && !connectionString.isEmpty()) {
                ServiceBusClientBuilder builder = new ServiceBusClientBuilder()
                        .connectionString(connectionString);
                
                senderClient = builder.sender()
                        .queueName(queueName)
                        .buildClient();
                
                logger.info("Service Bus sender client initialized for queue: {}", queueName);
            } else {
                logger.warn("Service Bus connection string not configured. Service Bus messaging disabled.");
            }
        } catch (Exception e) {
            logger.error("Error initializing Service Bus client: {}", e.getMessage(), e);
        }
    }
    
    public void sendDocumentIngestMessage(String documentId, String userId, String blobUrl, 
                                         String fileName, String contentType) {
        if (senderClient == null) {
            logger.warn("Service Bus not configured. Skipping message send.");
            return;
        }
        
        try {
            // Create message body
            Map<String, String> messageBody = new HashMap<>();
            messageBody.put("document_id", documentId);
            messageBody.put("user_id", userId);
            messageBody.put("blob_url", blobUrl);
            messageBody.put("file_name", fileName);
            messageBody.put("content_type", contentType);
            messageBody.put("container_name", "documents");
            messageBody.put("trigger_indexer", "true");
            
            // Convert to JSON string
            String jsonMessage = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(messageBody);
            
            // Create Service Bus message with session
            ServiceBusMessage message = new ServiceBusMessage(jsonMessage);
            message.setSessionId(documentId); // Use documentId as session ID for ordered processing
            message.setMessageId(documentId);
            message.setContentType("application/json");
            
            // Add application properties
            message.getApplicationProperties().put("DocumentId", documentId);
            message.getApplicationProperties().put("UserId", userId);
            message.getApplicationProperties().put("FileName", fileName);
            
            // Send message
            senderClient.sendMessage(message);
            
            logger.info("Sent ingest message for document {} to Service Bus queue {}", documentId, queueName);
            
        } catch (Exception e) {
            logger.error("Error sending message to Service Bus: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send message to Service Bus", e);
        }
    }
    
    @PreDestroy
    public void cleanup() {
        if (senderClient != null) {
            try {
                senderClient.close();
                logger.info("Service Bus sender client closed");
            } catch (Exception e) {
                logger.error("Error closing Service Bus sender client: {}", e.getMessage());
            }
        }
    }
}
