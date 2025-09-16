package com.adyanta.connect.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for WebClient
 */
@Slf4j
@Configuration
public class WebClientConfig {
    
    @Value("${external.apis.xml-to-json.timeout:30000}")
    private int xmlToJsonTimeout;
    
    @Value("${external.apis.fenergo.timeout:60000}")
    private int fenergoTimeout;
    
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)); // 10MB
    }
    
    private HttpClient createHttpClient() {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .responseTimeout(Duration.ofSeconds(30))
                .doOnConnected(conn -> {
                    conn.addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS));
                    conn.addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS));
                })
                .wiretap(true);
    }
}
