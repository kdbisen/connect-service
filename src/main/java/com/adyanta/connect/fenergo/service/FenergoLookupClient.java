package com.adyanta.connect.fenergo.service;

import com.adyanta.connect.fenergo.domain.dto.FenergoLookup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * HTTP client for Fenergo Reference Data API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FenergoLookupClient {
    
    private final WebClient.Builder webClientBuilder;
    private final OAuthTokenService oAuthTokenService;
    
    @Value("${external.apis.fenergo.base-url:https://api.fenergo.com/v1}")
    private String fenergoBaseUrl;
    
    public List<FenergoLookup> fetchLookups(String lookupName) {
        try {
            String token = oAuthTokenService.getAccessToken("fenergo");
            
            WebClient webClient = webClientBuilder
                .baseUrl(fenergoBaseUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
            
            List<FenergoLookup> lookups = webClient.get()
                .uri("/referencedata/lookups/{lookupName}", lookupName)
                .retrieve()
                .bodyToFlux(FenergoLookup.class)
                .collectList()
                .block();
            
            return lookups != null ? lookups : List.of();
        } catch (Exception e) {
            log.error("Error fetching lookups for: {}", lookupName, e);
            // Return empty list instead of throwing - allows graceful degradation
            log.warn("Returning empty lookup list for: {}", lookupName);
            return List.of();
        }
    }
}

