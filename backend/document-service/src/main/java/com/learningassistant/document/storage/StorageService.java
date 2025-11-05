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
     * Initialize storage
     */
    void init();
}
