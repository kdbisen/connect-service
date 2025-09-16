package com.adyanta.connect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.integration.annotation.IntegrationComponentScan;

/**
 * Main application class for Connect Service
 * 
 * This service provides:
 * - XML/JSON conversion APIs
 * - Fire-and-forget processing
 * - External API integration
 * - MongoDB persistence
 * - Comprehensive monitoring and logging
 */
@SpringBootApplication
@EnableCaching
@EnableReactiveMongoAuditing
@IntegrationComponentScan
public class ConnectServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConnectServiceApplication.class, args);
    }
}
