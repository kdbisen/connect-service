package com.adyanta.connect.fenergo.service;

import com.adyanta.connect.fenergo.domain.dto.FenergoEntityPayload;
import com.adyanta.connect.fenergo.domain.dto.FenergoEntityResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * HTTP client for Fenergo Entity APIs
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FenergoApiClient {
    
    private final WebClient.Builder webClientBuilder;
    private final OAuthTokenService oAuthTokenService;
    
    @Value("${external.apis.fenergo.base-url:https://api.fenergo.com/v1}")
    private String fenergoBaseUrl;
    
    public FenergoEntityResponse createEntity(FenergoEntityPayload payload) {
        try {
            String token = oAuthTokenService.getAccessToken("fenergo");
            
            WebClient webClient = webClientBuilder
                .baseUrl(fenergoBaseUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("Content-Type", "application/json")
                .build();
            
            return webClient.post()
                .uri("/entitydata-command-v2/api/entity")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(FenergoEntityResponse.class)
                .block();
        } catch (Exception e) {
            log.error("Error creating entity in Fenergo", e);
            throw new RuntimeException("Failed to create entity: " + e.getMessage(), e);
        }
    }
    
    public FenergoEntityResponse updateEntity(String entityId, FenergoEntityPayload payload) {
        try {
            String token = oAuthTokenService.getAccessToken("fenergo");
            
            WebClient webClient = webClientBuilder
                .baseUrl(fenergoBaseUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("Content-Type", "application/json")
                .build();
            
            return webClient.put()
                .uri("/entitydata-command-v2/api/entity/{entityId}", entityId)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(FenergoEntityResponse.class)
                .block();
        } catch (Exception e) {
            log.error("Error updating entity in Fenergo: {}", entityId, e);
            throw new RuntimeException("Failed to update entity: " + e.getMessage(), e);
        }
    }
    
    public FenergoEntityResponse getEntity(String entityId) {
        try {
            String token = oAuthTokenService.getAccessToken("fenergo");
            
            WebClient webClient = webClientBuilder
                .baseUrl(fenergoBaseUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
            
            return webClient.get()
                .uri("/entitydata-command-v2/api/entity/{entityId}", entityId)
                .retrieve()
                .bodyToMono(FenergoEntityResponse.class)
                .block();
        } catch (Exception e) {
            log.error("Error getting entity from Fenergo: {}", entityId, e);
            throw new RuntimeException("Failed to get entity: " + e.getMessage(), e);
        }
    }
}

