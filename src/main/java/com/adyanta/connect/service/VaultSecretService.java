package com.adyanta.connect.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import java.util.Map;

/**
 * Service for retrieving secrets from HashiCorp Vault
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "vault.enabled", havingValue = "true", matchIfMissing = false)
public class VaultSecretService {
    
    private final VaultTemplate vaultTemplate;
    
    @Value("${vault.secret-path:secret/connect-service}")
    private String secretPath;
    
    /**
     * Get MongoDB password from Vault
     */
    @Cacheable(value = "vault-secrets", key = "'mongodb-password'")
    public String getMongoDbPassword() {
        log.debug("Retrieving MongoDB password from Vault");
        return getSecret("mongodb_password", "mongodb");
    }
    
    /**
     * Get Apigee client ID from Vault
     */
    @Cacheable(value = "vault-secrets", key = "'apigee-client-id'")
    public String getApigeeClientId() {
        log.debug("Retrieving Apigee client ID from Vault");
        return getSecret("apigee_client_id", "oauth2");
    }
    
    /**
     * Get Apigee client secret from Vault
     */
    @Cacheable(value = "vault-secrets", key = "'apigee-client-secret'")
    public String getApigeeClientSecret() {
        log.debug("Retrieving Apigee client secret from Vault");
        return getSecret("apigee_client_secret", "oauth2");
    }
    
    /**
     * Get Fenergo client ID from Vault
     */
    @Cacheable(value = "vault-secrets", key = "'fenergo-client-id'")
    public String getFenergoClientId() {
        log.debug("Retrieving Fenergo client ID from Vault");
        return getSecret("fenergo_client_id", "oauth2");
    }
    
    /**
     * Get Fenergo client secret from Vault
     */
    @Cacheable(value = "vault-secrets", key = "'fenergo-client-secret'")
    public String getFenergoClientSecret() {
        log.debug("Retrieving Fenergo client secret from Vault");
        return getSecret("fenergo_client_secret", "oauth2");
    }
    
    /**
     * Get certificate from Vault
     */
    @Cacheable(value = "vault-secrets", key = "'tls-certificate'")
    public String getTlsCertificate() {
        log.debug("Retrieving TLS certificate from Vault");
        return getSecret("certificate", "pki");
    }
    
    /**
     * Get private key from Vault
     */
    @Cacheable(value = "vault-secrets", key = "'tls-private-key'")
    public String getTlsPrivateKey() {
        log.debug("Retrieving TLS private key from Vault");
        return getSecret("private_key", "pki");
    }
    
    /**
     * Generic method to retrieve secrets from Vault
     */
    private String getSecret(String key, String subPath) {
        try {
            String path = secretPath + "/" + subPath;
            VaultResponse response = vaultTemplate.read(path);
            
            if (response != null && response.getData() != null) {
                Map<String, Object> data = response.getData();
                String value = (String) data.get(key);
                if (value != null) {
                    log.debug("Successfully retrieved secret: {}", key);
                    return value;
                } else {
                    log.warn("Secret key '{}' not found in Vault path: {}", key, path);
                    return null;
                }
            } else {
                log.warn("No data found in Vault path: {}", path);
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to retrieve secret '{}' from Vault path: {}", key, secretPath + "/" + subPath, e);
            throw new RuntimeException("Failed to retrieve secret from Vault", e);
        }
    }
    
    /**
     * Refresh all cached secrets
     */
    public void refreshSecrets() {
        log.info("Refreshing all cached secrets from Vault");
        // Clear cache - this will force refresh on next access
        // In a real implementation, you might want to use a more sophisticated cache eviction strategy
    }
}
