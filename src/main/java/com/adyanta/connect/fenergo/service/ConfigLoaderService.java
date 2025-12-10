package com.adyanta.connect.fenergo.service;

import com.adyanta.connect.fenergo.domain.dto.TransformerConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service for loading transformer configurations
 */
@Slf4j
@Service
public class ConfigLoaderService {
    
    private final ObjectMapper objectMapper;
    
    public ConfigLoaderService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Cacheable(value = "transformer-configs", key = "#entityType + '_' + #version")
    public TransformerConfig loadConfig(String entityType, String version) {
        try {
            // Try to load from classpath first
            String configPath = String.format("fenergo/configs/%s_v%s.json", entityType, version);
            Resource resource = new ClassPathResource(configPath);
            
            if (resource.exists()) {
                return objectMapper.readValue(resource.getInputStream(), TransformerConfig.class);
            }
            
            // Try default version
            if (version == null || version.isEmpty()) {
                configPath = String.format("fenergo/configs/%s.json", entityType);
                resource = new ClassPathResource(configPath);
                if (resource.exists()) {
                    return objectMapper.readValue(resource.getInputStream(), TransformerConfig.class);
                }
            }
            
            // Try file system
            Path filePath = Paths.get("configs", entityType + "_v" + version + ".json");
            if (Files.exists(filePath)) {
                return objectMapper.readValue(Files.readAllBytes(filePath), TransformerConfig.class);
            }
            
            throw new RuntimeException("Config not found for entityType: " + entityType + ", version: " + version);
        } catch (IOException e) {
            log.error("Error loading config for entityType: {}, version: {}", entityType, version, e);
            throw new RuntimeException("Failed to load config: " + e.getMessage(), e);
        }
    }
}

