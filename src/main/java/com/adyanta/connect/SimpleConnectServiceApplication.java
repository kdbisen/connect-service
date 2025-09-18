package com.adyanta.connect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Simple Connect Service Application
 * Focused on ADD_KYC implementation without reactive dependencies
 */
@SpringBootApplication
@EnableCaching
@EnableMongoAuditing
@EnableAsync
public class SimpleConnectServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleConnectServiceApplication.class, args);
    }
}
