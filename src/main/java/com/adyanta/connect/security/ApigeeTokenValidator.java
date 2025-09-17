package com.adyanta.connect.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Service to validate Apigee tokens
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApigeeTokenValidator {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${spring.security.oauth2.client.provider.apigee.token-uri}")
    private String apigeeTokenUri;
    
    /**
     * Validate Apigee authentication token
     */
    public Mono<Boolean> validateToken(ApigeeAuthenticationToken token) {
        return webClientBuilder
                .build()
                .post()
                .uri(apigeeTokenUri)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(buildTokenRequest(token))
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    log.debug("Apigee token validation response: {}", response);
                    return response != null && !response.contains("error");
                })
                .timeout(Duration.ofSeconds(10))
                .onErrorReturn(false)
                .doOnError(error -> log.error("Error validating Apigee token", error));
    }
    
    private String buildTokenRequest(ApigeeAuthenticationToken token) {
        return String.format(
                "grant_type=client_credentials&client_id=%s&client_secret=%s",
                token.getClientId(),
                token.getClientSecret()
        );
    }
}

