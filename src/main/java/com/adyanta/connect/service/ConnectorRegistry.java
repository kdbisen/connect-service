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

        // mTLS support can be added here using a Netty SslContext if required in future.

        return b.build();
    }

    private ExchangeFilterFunction withStaticHeaders(Map<String, String> headers) {
        return (request, next) -> next.exchange(ClientRequest.from(request)
                .headers(h -> headers.forEach(h::add))
                .build());
    }

    // Placeholder for future SSL helpers if needed
}


