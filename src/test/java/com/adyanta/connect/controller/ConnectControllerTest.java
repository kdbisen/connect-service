package com.adyanta.connect.controller;

import com.adyanta.connect.domain.dto.ConnectRequestDto;
import com.adyanta.connect.domain.dto.ConnectResponseDto;
import com.adyanta.connect.domain.enums.RequestType;
import com.adyanta.connect.service.ConnectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ConnectController
 */
@WebFluxTest(ConnectController.class)
class ConnectControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ConnectService connectService;

    @Autowired
    private ObjectMapper objectMapper;

    private ConnectRequestDto sampleRequest;
    private ConnectResponseDto sampleResponse;

    @BeforeEach
    void setUp() {
        sampleRequest = ConnectRequestDto.builder()
                .requestType(RequestType.CUSTOMER_ONBOARDING)
                .payload("<xml>test data</xml>")
                .contentType("application/xml")
                .clientId("test-client")
                .requestId("test-request-123")
                .priority(5)
                .timeoutMs(300000L)
                .metadata(Map.of("test", "value"))
                .build();

        sampleResponse = ConnectResponseDto.builder()
                .requestId("test-request-123")
                .status("ACCEPTED")
                .message("Request accepted for processing")
                .timestamp(LocalDateTime.now())
                .processingId("processing-123")
                .build();
    }

    @Test
    @WithMockUser
    void processRequest_ShouldReturnAcceptedResponse() throws Exception {
        when(connectService.processRequest(any(ConnectRequestDto.class)))
                .thenReturn(Mono.just(sampleResponse));

        webTestClient.post()
                .uri("/connect/process")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sampleRequest)
                .exchange()
                .expectStatus().isAccepted()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.requestId").isEqualTo("test-request-123")
                .jsonPath("$.status").isEqualTo("ACCEPTED")
                .jsonPath("$.message").isEqualTo("Request accepted for processing");
    }

    @Test
    @WithMockUser
    void processRequest_WithInvalidData_ShouldReturnBadRequest() {
        ConnectRequestDto invalidRequest = ConnectRequestDto.builder()
                .payload("test")
                .contentType("application/xml")
                .build();

        webTestClient.post()
                .uri("/connect/process")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @WithMockUser
    void getRequestStatus_ShouldReturnRequestStatus() {
        ConnectResponseDto statusResponse = ConnectResponseDto.builder()
                .requestId("test-request-123")
                .status("COMPLETED")
                .message("Processing completed successfully")
                .timestamp(LocalDateTime.now())
                .processingId("processing-123")
                .data(Map.of("result", "success"))
                .build();

        when(connectService.getRequestStatus("test-request-123"))
                .thenReturn(Mono.just(statusResponse));

        webTestClient.get()
                .uri("/connect/status/test-request-123")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.requestId").isEqualTo("test-request-123")
                .jsonPath("$.status").isEqualTo("COMPLETED")
                .jsonPath("$.message").isEqualTo("Processing completed successfully");
    }

    @Test
    @WithMockUser
    void getRequestStatus_WithNonExistentRequest_ShouldReturnNotFound() {
        when(connectService.getRequestStatus("non-existent"))
                .thenReturn(Mono.error(new RuntimeException("Request not found")));

        webTestClient.get()
                .uri("/connect/status/non-existent")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void health_ShouldReturnHealthStatus() {
        webTestClient.get()
                .uri("/connect/health")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP")
                .jsonPath("$.message").isEqualTo("Connect service is running");
    }
}

