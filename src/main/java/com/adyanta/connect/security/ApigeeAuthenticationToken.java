package com.adyanta.connect.security;

import lombok.Getter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Custom authentication token for Apigee authentication
 */
@Getter
public class ApigeeAuthenticationToken extends UsernamePasswordAuthenticationToken {
    
    private final String clientId;
    private final String clientSecret;
    private final String apiKey;
    
    public ApigeeAuthenticationToken(String clientId, String clientSecret, String apiKey) {
        super(clientId, clientSecret);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.apiKey = apiKey;
    }
    
    public ApigeeAuthenticationToken(String clientId, String clientSecret, String apiKey, 
                                   Collection<? extends GrantedAuthority> authorities) {
        super(clientId, clientSecret, authorities);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.apiKey = apiKey;
    }
}
