package com.learningassistant.document.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "azure")
public class AzureBlobStorageService implements StorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(AzureBlobStorageService.class);
    
    @Value("${storage.azure.connection-string}")
    private String connectionString;
    
    @Value("${storage.azure.container-name}")
    private String containerName;
    
    private BlobContainerClient containerClient;
    
    @Override
    public void init() {
        try {
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();
            
            containerClient = blobServiceClient.getBlobContainerClient(containerName);
            
            // Create container if it doesn't exist
            if (!containerClient.exists()) {
                containerClient.create();
                logger.info("Created blob container: {}", containerName);
            }
            
            logger.info("Azure Blob Storage initialized for container: {}", containerName);
        } catch (Exception e) {
            logger.error("Failed to initialize Azure Blob Storage", e);
            throw new RuntimeException("Could not initialize Azure Blob Storage", e);
        }
    }
    
    @Override
    public String storeFile(MultipartFile file, String userId) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Failed to store empty file");
        }
        
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        // Generate unique blob name with user prefix
        String blobName = userId + "/" + UUID.randomUUID().toString() + extension;
        
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        
        // Upload file to blob storage
        blobClient.upload(file.getInputStream(), file.getSize(), true);
        
        // Set metadata
        blobClient.setMetadata(java.util.Map.of(
            "originalFilename", originalFilename != null ? originalFilename : "unknown",
            "userId", userId,
            "contentType", file.getContentType() != null ? file.getContentType() : "application/octet-stream"
        ));
        
        logger.info("Uploaded file: {} to blob: {}", originalFilename, blobName);
        
        return blobName;
    }
    
    @Override
    public Path loadFile(String blobName) {
        // For Azure Blob, return a temporary path (not typically used)
        return Paths.get(blobName);
    }
    
    @Override
    public String readFileAsText(String blobName) throws IOException {
        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);

            if (!blobClient.exists()) {
                throw new IOException("Blob not found: " + blobName);
            }

            // Download blob content as byte array
            byte[] content = blobClient.downloadContent().toBytes();

            // Convert to string
            String text = new String(content, java.nio.charset.StandardCharsets.UTF_8);

            logger.info("Read {} bytes from blob: {}", content.length, blobName);
            return text;
        } catch (Exception e) {
            logger.error("Failed to read blob as text: {}", blobName, e);
            throw new IOException("Failed to read file content: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String blobName) {
        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            blobClient.delete();
            logger.info("Deleted blob: {}", blobName);
        } catch (Exception e) {
            logger.error("Failed to delete blob: {}", blobName, e);
        }
    }
    
    @Override
    public String getFileUrl(String blobName, String userId) {
        try {
            // 1.  SAS definition
            BlobSasPermission permission = new BlobSasPermission()
                    .setReadPermission(true);

            OffsetDateTime expiry = OffsetDateTime.now(ZoneId.of("UTC")).plusDays(7);
            OffsetDateTime start  = OffsetDateTime.now(ZoneId.of("UTC")).minusMinutes(5);

            BlobServiceSasSignatureValues sasValues =
                    new BlobServiceSasSignatureValues(expiry, permission)
                            .setStartTime(start);

            // 2.  generate SAS token
            String sasToken = containerClient
                    .getBlobClient(blobName)
                    .generateSas(sasValues);   // <-- correct method

            // 3.  build full URL
            String url = containerClient
                    .getBlobClient(blobName)
                    .getBlobUrl()
                    + "?" + sasToken;

            logger.info("[CRITICAL] Generated SAS URL for blob {}: {}", blobName, url);
            return url;

        } catch (Exception e) {
            logger.error("[ERROR] Failed to create SAS for blob {}", blobName, e);
            // fall back to plain blob URL (will fail if container is private)
            return containerClient.getBlobClient(blobName).getBlobUrl();
        }
    }
    
    @Override
    public String getStorageType() {
        return "azure";
    }
}
