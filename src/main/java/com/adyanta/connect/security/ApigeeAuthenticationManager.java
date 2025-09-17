package com.adyanta.connect.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Custom authentication manager for Apigee authentication
 * Validates client credentials against Apigee
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApigeeAuthenticationManager implements ReactiveAuthenticationManager {
    
    private final ApigeeTokenValidator apigeeTokenValidator;
    
    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        if (!(authentication instanceof ApigeeAuthenticationToken)) {
            return Mono.empty();
        }
        
        ApigeeAuthenticationToken token = (ApigeeAuthenticationToken) authentication;
        
        return apigeeTokenValidator.validateToken(token)
                .map(isValid -> {
                    if (isValid) {
                        log.debug("Authentication successful for client: {}", token.getClientId());
                        return token;
                    } else {
                        log.warn("Authentication failed for client: {}", token.getClientId());
                        return null;
                    }
                })
                .cast(Authentication.class)
                .onErrorResume(throwable -> {
                    log.error("Error during authentication", throwable);
                    return Mono.empty();
                });
    }
}

