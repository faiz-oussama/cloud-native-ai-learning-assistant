package com.learningassistant.document.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(LocalStorageService.class);
    
    private final Path rootLocation;
    
    public LocalStorageService(@Value("${storage.local.upload-dir}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir);
    }
    
    @Override
    public void init() {
        try {
            if (!Files.exists(rootLocation)) {
                Files.createDirectories(rootLocation);
                logger.info("Created upload directory: {}", rootLocation);
            }
        } catch (IOException e) {
            logger.error("Failed to initialize storage", e);
            throw new RuntimeException("Could not initialize storage", e);
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
        
        // Generate unique filename
        String uniqueFilename = UUID.randomUUID().toString() + extension;
        
        // Create user-specific subdirectory
        Path userDir = rootLocation.resolve(userId);
        if (!Files.exists(userDir)) {
            Files.createDirectories(userDir);
        }
        
        Path destinationFile = userDir.resolve(uniqueFilename);
        
        // Copy file to destination
        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        }
        
        logger.info("Stored file: {} at {}", originalFilename, destinationFile);
        
        return userId + "/" + uniqueFilename;
    }
    
    @Override
    public Path loadFile(String fileName) {
        return rootLocation.resolve(fileName);
    }
    
    @Override
    public void deleteFile(String fileName) {
        try {
            Path file = rootLocation.resolve(fileName);
            Files.deleteIfExists(file);
            logger.info("Deleted file: {}", fileName);
        } catch (IOException e) {
            logger.error("Failed to delete file: {}", fileName, e);
        }
    }
    
    @Override
    public String getFileUrl(String fileName, String userId) {
        // For local storage, return the file path
        // In Azure deployment, this would be a blob URL
        return rootLocation.resolve(fileName).toString();
    }
}
