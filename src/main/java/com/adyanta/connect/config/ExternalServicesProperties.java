package com.adyanta.connect.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Binds configuration under external.services.* for multiple downstream connectors.
 * Example (application-openshift.yml):
 * external:
 *   services:
 *     xmlToJson:
 *       baseUrl: https://apigee.company.com/xml-to-json
 *       timeoutMs: 30000
 *       retry:
 *         attempts: 3
 *         backoffMs: 500
 *       headers:
 *         X-API-KEY: ${XML_TO_JSON_API_KEY}
 *       auth:
 *         provider: apigee
 *         clientId: ${XML_JSON_CLIENT_ID}
 *         clientSecret: ${XML_JSON_CLIENT_SECRET}
 *         tokenUri: https://login.apigee.com/oauth/token
 *         scope: "read write"
 *         mTLS:
 *           enabled: false
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "external")
public class ExternalServicesProperties {

    private Map<String, Service> services = new HashMap<>();

    @Getter
    @Setter
    public static class Service {
        private String baseUrl;
        private Integer timeoutMs = 30000;
        private Retry retry = new Retry();
        private Auth auth = new Auth();
        private Map<String, String> headers = new HashMap<>();
    }

    @Getter
    @Setter
    public static class Retry {
        private Integer attempts = 3;
        private Integer backoffMs = 500;
    }

    @Getter
    @Setter
    public static class Auth {
        private String provider; // logical name (e.g., apigee, fenergo)
        private String clientId;
        private String clientSecret;
        private String tokenUri;
        private String scope;
        private Mtls mTLS = new Mtls();
    }

    @Getter
    @Setter
    public static class Mtls {
        private boolean enabled = false;
        private String keyStorePath;
        private String keyStorePassword;
        private String keyStoreType = "PKCS12";
        private String trustStorePath;
        private String trustStorePassword;
        private String trustStoreType = "PKCS12";
        private boolean disableHostNameVerification = false;
    }
}


