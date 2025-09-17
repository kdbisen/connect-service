package com.adyanta.connect.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.UuidRepresentation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;
import org.springframework.util.StringUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.time.Duration;

/**
 * Advanced MongoDB configuration that supports TLS/SSL using keystore/truststore
 * and credentials provided at runtime (e.g., via Vault). This configuration
 * takes precedence when property `mongodb.ssl.enabled=true`.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mongodb.ssl", name = "enabled", havingValue = "true")
public class AdvancedMongoConfig extends AbstractReactiveMongoConfiguration {

    private final AdvancedMongoProperties mongoProps;

    @Override
    protected String getDatabaseName() {
        return mongoProps.getDatabase();
    }

    @Bean
    @Override
    public MongoClient reactiveMongoClient() {
        String uri = buildUriWithPassword(mongoProps.getUri(), mongoProps.getPassword());
        log.info("Configuring Reactive MongoClient with SSL. URI (sanitized): {}", sanitize(uri));

        MongoClientSettings.Builder builder = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .applyToClusterSettings(cs -> cs.serverSelectionTimeout(Duration.ofSeconds(10)))
                .applyToSocketSettings(ss -> ss.connectTimeout(10_000))
                .readPreference(ReadPreference.primary())
                .readConcern(ReadConcern.MAJORITY)
                .writeConcern(WriteConcern.MAJORITY.withWTimeout(Duration.ofSeconds(10)));

        if (mongoProps.getSsl().isEnabled()) {
            builder.applyToSslSettings(ssl -> {
                ssl.enabled(true);
                SSLContext sslContext = buildSslContext();
                ssl.context(sslContext);
                ssl.invalidHostNameAllowed(mongoProps.getSsl().isDisableHostNameVerification());
            });
        }

        return MongoClients.create(builder.build());
    }

    @Bean
    @Override
    public ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory(MongoClient client) {
        return new SimpleReactiveMongoDatabaseFactory(client, getDatabaseName());
    }

    private SSLContext buildSslContext() {
        try {
            KeyManagerFactory kmf = null;
            TrustManagerFactory tmf = null;

            // Keystore (client cert)
            if (StringUtils.hasText(mongoProps.getSsl().getKeyStorePath())) {
                KeyStore keyStore = KeyStore.getInstance(mongoProps.getSsl().getKeyStoreType());
                try (FileInputStream fis = new FileInputStream(mongoProps.getSsl().getKeyStorePath())) {
                    keyStore.load(fis, toCharArray(mongoProps.getSsl().getKeyStorePassword()));
                }
                kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(keyStore, toCharArray(mongoProps.getSsl().getKeyStorePassword()));
            }

            // Truststore (server CA)
            if (StringUtils.hasText(mongoProps.getSsl().getTrustStorePath())) {
                KeyStore trustStore = KeyStore.getInstance(mongoProps.getSsl().getTrustStoreType());
                try (FileInputStream fis = new FileInputStream(mongoProps.getSsl().getTrustStorePath())) {
                    trustStore.load(fis, toCharArray(mongoProps.getSsl().getTrustStorePassword()));
                }
                tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(trustStore);
            }

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf != null ? kmf.getKeyManagers() : null, tmf != null ? tmf.getTrustManagers() : null, null);
            return sslContext;
        } catch (Exception e) {
            log.error("Failed building SSLContext for MongoDB", e);
            throw new IllegalStateException("Unable to configure Mongo SSL", e);
        }
    }

    private char[] toCharArray(String value) {
        return value == null ? new char[0] : value.toCharArray();
    }

    private String buildUriWithPassword(String baseUri, String password) {
        if (!StringUtils.hasText(password)) {
            return baseUri;
        }
        // Inject password if URI has user without password: mongodb://user@host/db -> mongodb://user:pwd@host/db
        // If password already present, return as is.
        if (baseUri.matches("mongodb(\\+srv)?:\\/\\/[^:]+:[^@]+@.*")) {
            return baseUri; // already has password
        }
        return baseUri.replaceFirst("(mongodb(\\+srv)?:\\/\\/)([^:@/]+)(@)", "$1$3:" + java.util.regex.Matcher.quoteReplacement(password) + "$4");
    }

    private String sanitize(String uri) {
        return uri.replaceAll(":([^@]+)@", ":***@");
    }
}


