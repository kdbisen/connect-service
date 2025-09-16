package com.adyanta.connect.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Client for calling external APIs
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalApiClient {
    
    private final WebClient.Builder webClientBuilder;
    private final OAuth2TokenService oAuth2TokenService;
    
    @Value("${external.apis.xml-to-json.base-url}")
    private String xmlToJsonApiUrl;
    
    @Value("${external.apis.xml-to-json.timeout}")
    private int xmlToJsonTimeout;
    
    @Value("${external.apis.fenergo.base-url}")
    private String fenergoApiUrl;
    
    @Value("${external.apis.fenergo.timeout}")
    private int fenergoTimeout;
    
    /**
     * Convert XML to JSON using external API
     */
    public Mono<String> convertXmlToJson(String xmlPayload, String clientId) {
        log.debug("Converting XML to JSON for client: {}", clientId);
        
        return oAuth2TokenService.getApigeeToken()
                .flatMap(token -> webClientBuilder
                        .build()
                        .post()
                        .uri(xmlToJsonApiUrl + "/convert")
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/xml")
                        .header("Accept", "application/json")
                        .bodyValue(xmlPayload)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofMillis(xmlToJsonTimeout))
                        .doOnSuccess(response -> log.debug("XML to JSON conversion successful"))
                        .doOnError(error -> log.error("XML to JSON conversion failed", error))
                );
    }
    
    /**
     * Call Fenergo API with JSON payload
     */
    public Mono<String> callFenergoApi(String jsonPayload, String clientId) {
        log.debug("Calling Fenergo API for client: {}", clientId);
        
        return oAuth2TokenService.getFenergoToken()
                .flatMap(token -> webClientBuilder
                        .build()
                        .post()
                        .uri(fenergoApiUrl + "/process")
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .bodyValue(jsonPayload)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofMillis(fenergoTimeout))
                        .doOnSuccess(response -> log.debug("Fenergo API call successful"))
                        .doOnError(error -> log.error("Fenergo API call failed", error))
                );
    }
}
