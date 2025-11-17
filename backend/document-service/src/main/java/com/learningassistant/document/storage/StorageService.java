package com.learningassistant.document.storage;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Path;

public interface StorageService {
    
    /**
     * Store a file and return the storage path
     */
    String storeFile(MultipartFile file, String userId) throws IOException;
    
    /**
     * Load a file as a Path
     */
    Path loadFile(String fileName);
    
    /**
     * Delete a file
     */
    void deleteFile(String fileName);
    
    /**
     * Get the URL/path for accessing a file
     */
    String getFileUrl(String fileName, String userId);
    
    /**
     * Initialize storage
     */
    void init();
    
    /**
     * Get the storage type (e.g., 'azure', 'local')
     */
    String getStorageType();
}
