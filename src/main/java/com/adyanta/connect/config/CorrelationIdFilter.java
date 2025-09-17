package com.adyanta.connect.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.util.Optional;
import java.util.UUID;

/**
 * WebFilter that ensures every request has a correlation id.
 *
 * Behaviour:
 * - Reads X-Correlation-Id header if present; otherwise generates a UUID.
 * - Puts the id into Reactor Context and SLF4J MDC for logging.
 * - Adds X-Correlation-Id to the response headers.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements WebFilter {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    public static final String CTX_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String incoming = request.getHeaders().getFirst(HEADER_NAME);
        String correlationId = Optional.ofNullable(incoming).filter(s -> !s.isBlank()).orElseGet(() -> UUID.randomUUID().toString());

        // Add to response header
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add(HEADER_NAME, correlationId);

        // Continue with correlation id in Reactor Context and MDC
        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(CTX_KEY, correlationId))
                .doOnSubscribe(sub -> MDC.put(MDC_KEY, correlationId))
                .doFinally(signalType -> MDC.remove(MDC_KEY));
    }

    /**
     * Helper to fetch correlation id from Reactor Context.
     */
    public static String getCorrelationId(ContextView contextView) {
        return contextView.getOrDefault(CTX_KEY, "");
    }
}


