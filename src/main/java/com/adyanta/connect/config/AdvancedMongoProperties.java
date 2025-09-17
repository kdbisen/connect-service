package com.adyanta.connect.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for advanced MongoDB connection settings, including TLS/SSL.
 *
 * These are intentionally separate from Spring's default spring.data.mongodb.* to:
 * - Allow secure TLS/SSL setup using keystore/truststore
 * - Keep Vault-provided paths/passwords decoupled from the connection string
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "mongodb")
public class AdvancedMongoProperties {

    /**
     * Full MongoDB connection URI. Username should be present; password may be provided via Vault.
     * Example: mongodb://connect-service@mongodb:27017/connect-service?authSource=admin
     */
    private String uri;

    /**
     * Target database name.
     */
    private String database;

    /**
     * Password to use with the URI's user. Typically injected from Vault.
     */
    private String password;

    private final Ssl ssl = new Ssl();

    @Getter
    @Setter
    public static class Ssl {
        /** Enable TLS/SSL for MongoDB driver. */
        private boolean enabled = false;

        /**
         * Optional: Absolute path to a PKCS12 or JKS keystore containing the client certificate and private key.
         * Example: /vault/secrets/mongo-client-keystore.p12
         */
        private String keyStorePath;

        /** Password for the keystore (from Vault). */
        private String keyStorePassword;

        /**
         * Optional: Absolute path to a truststore (PKCS12 or JKS) containing the CA chain to trust the MongoDB server cert.
         * Example: /vault/secrets/mongo-truststore.p12
         */
        private String trustStorePath;

        /** Password for the truststore (from Vault). */
        private String trustStorePassword;

        /** Keystore type, e.g. PKCS12 or JKS. Defaults to PKCS12. */
        private String keyStoreType = "PKCS12";

        /** Truststore type, e.g. PKCS12 or JKS. Defaults to PKCS12. */
        private String trustStoreType = "PKCS12";

        /**
         * When true, hostname verification is disabled. Use only in dev/test.
         */
        private boolean disableHostNameVerification = false;
    }
}



