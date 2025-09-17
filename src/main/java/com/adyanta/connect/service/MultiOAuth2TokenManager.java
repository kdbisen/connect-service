package com.adyanta.connect.service;

import com.adyanta.connect.config.ExternalServicesProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token manager that can fetch and cache tokens for multiple providers/clients.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiOAuth2TokenManager {

    private final WebClient.Builder webClientBuilder;
    private final Map<String, String> tokenCache = new ConcurrentHashMap<>();

    public Mono<String> getToken(ExternalServicesProperties.Auth auth) {
        String cacheKey = auth.getProvider() + ":" + auth.getClientId();
        String cached = tokenCache.get(cacheKey);
        if (cached != null) {
            return Mono.just(cached);
        }
        return requestToken(auth)
                .doOnNext(token -> tokenCache.put(cacheKey, token));
    }

    private Mono<String> requestToken(ExternalServicesProperties.Auth auth) {
        String body = "grant_type=client_credentials" +
                "&client_id=" + auth.getClientId() +
                "&client_secret=" + auth.getClientSecret() +
                (auth.getScope() != null ? "&scope=" + auth.getScope() : "");
        return webClientBuilder.build()
                .post()
                .uri(auth.getTokenUri())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(map -> (String) map.get("access_token"))
                .timeout(Duration.ofSeconds(10))
                .doOnError(err -> log.error("Token request failed for provider {}", auth.getProvider(), err));
    }
}



