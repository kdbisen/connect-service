package com.adyanta.connect.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Custom authentication converter for Apigee authentication
 * Extracts client credentials from request headers
 */
@Slf4j
@Component
public class ApigeeAuthenticationFilter implements ServerAuthenticationConverter {
    
    private static final String CLIENT_ID_HEADER = "X-Client-Id";
    private static final String CLIENT_SECRET_HEADER = "X-Client-Secret";
    private static final String API_KEY_HEADER = "X-API-Key";
    
    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        
        return Mono.fromCallable(() -> {
            String clientId = request.getHeaders().getFirst(CLIENT_ID_HEADER);
            String clientSecret = request.getHeaders().getFirst(CLIENT_SECRET_HEADER);
            String apiKey = request.getHeaders().getFirst(API_KEY_HEADER);
            
            if (clientId == null || clientSecret == null) {
                log.warn("Missing client credentials in request headers");
                return null;
            }
            
            // Create authentication token with client credentials
            ApigeeAuthenticationToken authToken = new ApigeeAuthenticationToken(
                clientId, 
                clientSecret, 
                apiKey
            );
            
            log.debug("Created authentication token for client: {}", clientId);
            return authToken;
        })
        .onErrorResume(throwable -> {
            log.error("Error creating authentication token", throwable);
            return Mono.empty();
        });
    }
}
