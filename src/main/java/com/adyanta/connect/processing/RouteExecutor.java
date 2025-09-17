package com.adyanta.connect.processing;

import com.adyanta.connect.service.ConnectorRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Minimal route executor that takes a list of connector names and sequentially calls them,
 * passing the response to the next step. You can replace this with a proper workflow engine later.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RouteExecutor {

    private final ConnectorRegistry registry;

    public Mono<String> execute(List<String> connectorNames, String initialPayload) {
        if (CollectionUtils.isEmpty(connectorNames)) {
            return Mono.just(initialPayload);
        }
        Mono<String> chain = Mono.just(initialPayload);
        for (String name : connectorNames) {
            chain = chain.flatMap(payload -> invokeConnector(name, payload));
        }
        return chain;
    }

    private Mono<String> invokeConnector(String connectorName, String jsonPayload) {
        log.debug("Invoking connector {}", connectorName);
        WebClient client = registry.getClient(connectorName);
        return client.post()
                .uri("")
                .body(BodyInserters.fromValue(jsonPayload))
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> log.error("Connector {} failed", connectorName, e));
    }
}



