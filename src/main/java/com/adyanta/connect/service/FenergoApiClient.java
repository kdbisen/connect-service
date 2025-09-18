package com.adyanta.connect.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Service for Fenergo API client
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FenergoApiClient {
    
    private final RestTemplate restTemplate;
    private final OAuth2TokenService oAuth2TokenService;
    
    @Value("${external.services.fenergo.base-url:https://api.fenergo.com}")
    private String fenergoBaseUrl;
    
    @Value("${external.services.fenergo.timeout:60000}")
    private int fenergoTimeout;
    
    @Value("${external.services.fenergo.retry-attempts:3}")
    private int fenergoRetryAttempts;
    
    @Value("${external.services.fenergo.api-path:/api/v1/kyc}")
    private String fenergoApiPath;
    
    /**
     * Call Fenergo API with the provided data
     */
    public String callFenergoApi(Map<String, Object> requestData) {
        try {
            log.debug("Calling Fenergo API with data: {}", requestData);
            
            // Get OAuth2 token for Fenergo
            String accessToken = oAuth2TokenService.getFenergoAccessToken();
            
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            headers.set("User-Agent", "ConnectService/1.0.0");
            
            // Create request entity
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestData, headers);
            
            // Prepare URL
            String fenergoUrl = fenergoBaseUrl + fenergoApiPath;
            
            log.debug("Calling Fenergo API URL: {}", fenergoUrl);
            
            // Make POST request to Fenergo
            ResponseEntity<String> response = restTemplate.exchange(
                fenergoUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Fenergo API call successful with status: {}", response.getStatusCode());
                return response.getBody();
            } else {
                throw new RuntimeException("Fenergo API call failed with status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Failed to call Fenergo API", e);
            throw new RuntimeException("Fenergo API call failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get Fenergo base URL
     */
    public String getFenergoBaseUrl() {
        return fenergoBaseUrl;
    }
    
    /**
     * Get Fenergo timeout
     */
    public int getFenergoTimeout() {
        return fenergoTimeout;
    }
    
    /**
     * Get Fenergo retry attempts
     */
    public int getFenergoRetryAttempts() {
        return fenergoRetryAttempts;
    }
}
