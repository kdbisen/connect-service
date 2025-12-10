package com.adyanta.connect.fenergo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;

import java.security.Principal;

/**
 * Service for managing OAuth2 tokens
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthTokenService {
    
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    
    @Cacheable(value = "oauth-tokens", key = "#registrationId")
    public String getAccessToken(String registrationId) {
        try {
            // Create a system principal for client credentials flow
            Principal systemPrincipal = () -> "system";
            
            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(registrationId)
                .principal(systemPrincipal)
                .build();
            
            OAuth2AuthorizedClient authorizedClient = authorizedClientManager
                .authorize(authorizeRequest);
            
            if (authorizedClient == null) {
                throw new RuntimeException("Failed to authorize client: " + registrationId);
            }
            
            OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
            if (accessToken == null) {
                throw new RuntimeException("Access token is null for: " + registrationId);
            }
            
            return accessToken.getTokenValue();
        } catch (Exception e) {
            log.error("Error getting OAuth token for: {}", registrationId, e);
            throw new RuntimeException("Failed to get OAuth token: " + e.getMessage(), e);
        }
    }
}

