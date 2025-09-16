package com.adyanta.connect.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Service for managing OAuth2 tokens for external APIs
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2TokenService {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${spring.security.oauth2.client.registration.apigee.client-id}")
    private String apigeeClientId;
    
    @Value("${spring.security.oauth2.client.registration.apigee.client-secret}")
    private String apigeeClientSecret;
    
    @Value("${spring.security.oauth2.client.provider.apigee.token-uri}")
    private String apigeeTokenUri;
    
    @Value("${spring.security.oauth2.client.registration.fenergo.client-id}")
    private String fenergoClientId;
    
    @Value("${spring.security.oauth2.client.registration.fenergo.client-secret}")
    private String fenergoClientSecret;
    
    @Value("${spring.security.oauth2.client.provider.fenergo.token-uri}")
    private String fenergoTokenUri;
    
    /**
     * Get Apigee OAuth2 token
     */
    @Cacheable(value = "apigee-tokens", key = "#root.method.name")
    public Mono<String> getApigeeToken() {
        log.debug("Requesting Apigee OAuth2 token");
        
        return webClientBuilder
                .build()
                .post()
                .uri(apigeeTokenUri)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(buildTokenRequest(apigeeClientId, apigeeClientSecret))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    String token = (String) response.get("access_token");
                    log.debug("Apigee token obtained successfully");
                    return token;
                })
                .timeout(Duration.ofSeconds(10))
                .doOnError(error -> log.error("Failed to get Apigee token", error));
    }
    
    /**
     * Get Fenergo OAuth2 token
     */
    @Cacheable(value = "fenergo-tokens", key = "#root.method.name")
    public Mono<String> getFenergoToken() {
        log.debug("Requesting Fenergo OAuth2 token");
        
        return webClientBuilder
                .build()
                .post()
                .uri(fenergoTokenUri)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(buildTokenRequest(fenergoClientId, fenergoClientSecret))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    String token = (String) response.get("access_token");
                    log.debug("Fenergo token obtained successfully");
                    return token;
                })
                .timeout(Duration.ofSeconds(10))
                .doOnError(error -> log.error("Failed to get Fenergo token", error));
    }
    
    private String buildTokenRequest(String clientId, String clientSecret) {
        return String.format(
                "grant_type=client_credentials&client_id=%s&client_secret=%s",
                clientId,
                clientSecret
        );
    }
}
