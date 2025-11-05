package com.learningassistant.document.config;

import com.learningassistant.document.storage.StorageService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {
    
    @Bean
    CommandLineRunner init(StorageService storageService) {
        return args -> {
            storageService.init();
        };
    }
}
