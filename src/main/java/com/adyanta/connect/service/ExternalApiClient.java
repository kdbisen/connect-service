package com.adyanta.connect.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for external API client configuration
 */
@Service
public class ExternalApiClient {
    
    @Value("${external.services.misc-service.base-url:http://localhost:8081}")
    private String miscServiceBaseUrl;
    
    @Value("${external.services.misc-service.timeout:30000}")
    private int miscServiceTimeout;
    
    @Value("${external.services.misc-service.retry-attempts:3}")
    private int miscServiceRetryAttempts;
    
    /**
     * Get misc service base URL
     */
    public String getMiscServiceUrl() {
        return miscServiceBaseUrl;
    }
    
    /**
     * Get misc service timeout
     */
    public int getMiscServiceTimeout() {
        return miscServiceTimeout;
    }
    
    /**
     * Get misc service retry attempts
     */
    public int getMiscServiceRetryAttempts() {
        return miscServiceRetryAttempts;
    }
}