package com.adyanta.connect.service;

import com.adyanta.connect.config.ExternalServicesProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds and exposes WebClient instances for configured connectors, adding auth headers,
 * static headers, mTLS if configured, and basic Resilience4j wrappers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConnectorRegistry {

    private final ExternalServicesProperties props;
    private final WebClient.Builder baseBuilder;
    private final MultiOAuth2TokenManager tokenManager;

    private final Map<String, WebClient> clients = new ConcurrentHashMap<>();

    public WebClient getClient(String name) {
        return clients.computeIfAbsent(name, this::buildClient);
    }

    private WebClient buildClient(String name) {
        ExternalServicesProperties.Service svc = props.getServices().get(name);
        if (svc == null) {
            throw new IllegalArgumentException("Unknown connector: " + name);
        }

        WebClient.Builder b = baseBuilder.clone()
                .baseUrl(svc.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .filter(withStaticHeaders(svc.getHeaders()));

        // OAuth2 bearer injection
        if (StringUtils.hasText(svc.getAuth().getTokenUri())) {
            b.filter((request, next) -> tokenManager.getToken(svc.getAuth())
                    .flatMap(token -> next.exchange(ClientRequest.from(request)
                            .headers(h -> h.setBearerAuth(token))
                            .build())));
        }

        // mTLS if configured
        if (svc.getAuth().getMTLS().isEnabled()) {
            SSLContext sslContext = buildSslContext(svc.getAuth().getMTLS());
            b.clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(
                    reactor.netty.http.client.HttpClient.create()
                            .secure(sslSpec -> sslSpec.sslContext(new io.netty.handler.ssl.JdkSslContext(sslContext, true)))
                            .responseTimeout(Duration.ofMillis(svc.getTimeoutMs()))
            ));
        }

        return b.build();
    }

    private ExchangeFilterFunction withStaticHeaders(Map<String, String> headers) {
        return (request, next) -> next.exchange(ClientRequest.from(request)
                .headers(h -> headers.forEach(h::add))
                .build());
    }

    private SSLContext buildSslContext(ExternalServicesProperties.Mtls m) {
        try {
            KeyManagerFactory kmf = null;
            TrustManagerFactory tmf = null;
            if (StringUtils.hasText(m.getKeyStorePath())) {
                KeyStore ks = KeyStore.getInstance(m.getKeyStoreType());
                try (FileInputStream fis = new FileInputStream(m.getKeyStorePath())) {
                    ks.load(fis, toChars(m.getKeyStorePassword()));
                }
                kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(ks, toChars(m.getKeyStorePassword()));
            }
            if (StringUtils.hasText(m.getTrustStorePath())) {
                KeyStore ts = KeyStore.getInstance(m.getTrustStoreType());
                try (FileInputStream fis = new FileInputStream(m.getTrustStorePath())) {
                    ts.load(fis, toChars(m.getTrustStorePassword()));
                }
                tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ts);
            }
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(kmf != null ? kmf.getKeyManagers() : null, tmf != null ? tmf.getTrustManagers() : null, null);
            return ctx;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build mTLS context for connector", e);
        }
    }

    private char[] toChars(String s) { return s == null ? new char[0] : s.toCharArray(); }
}


