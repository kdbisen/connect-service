package com.adyanta.connect.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.KubernetesAuthentication;
import org.springframework.vault.authentication.KubernetesAuthenticationOptions;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultToken;

import java.net.URI;

/**
 * Configuration for HashiCorp Vault integration
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "vault.enabled", havingValue = "true", matchIfMissing = false)
public class VaultConfig {
    
    @Value("${vault.address}")
    private String vaultAddress;
    
    @Value("${vault.role}")
    private String vaultRole;
    
    @Value("${vault.auth-path:kubernetes}")
    private String authPath;
    
    @Bean
    public VaultEndpoint vaultEndpoint() {
        try {
            return VaultEndpoint.from(URI.create(vaultAddress));
        } catch (Exception e) {
            log.error("Failed to create Vault endpoint", e);
            throw new RuntimeException("Failed to create Vault endpoint", e);
        }
    }
    
    @Bean
    public ClientAuthentication clientAuthentication() {
        KubernetesAuthenticationOptions options = KubernetesAuthenticationOptions.builder()
                .role(vaultRole)
                .path(authPath)
                .build();
        
        return new KubernetesAuthentication(options, vaultEndpoint());
    }
    
    @Bean
    public VaultTemplate vaultTemplate(VaultEndpoint vaultEndpoint, ClientAuthentication clientAuthentication) {
        VaultTemplate vaultTemplate = new VaultTemplate(vaultEndpoint, clientAuthentication);
        log.info("Vault template configured successfully");
        return vaultTemplate;
    }
}

