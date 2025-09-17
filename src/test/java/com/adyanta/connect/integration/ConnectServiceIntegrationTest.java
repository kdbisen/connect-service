package com.adyanta.connect.integration;

import com.adyanta.connect.domain.document.ProcessingRequest;
import com.adyanta.connect.domain.dto.ConnectRequestDto;
import com.adyanta.connect.domain.enums.ProcessingStatus;
import com.adyanta.connect.domain.enums.RequestType;
import com.adyanta.connect.repository.ProcessingRequestRepository;
import com.adyanta.connect.repository.ProcessingAuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Connect Service
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
@ActiveProfiles("test")
class ConnectServiceIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withReuse(true);

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ProcessingRequestRepository requestRepository;

    @Autowired
    private ProcessingAuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Clean up test data
        requestRepository.deleteAll().block();
        auditLogRepository.deleteAll().block();
    }

    @Test
    @WithMockUser
    void processRequest_EndToEnd_ShouldWork() {
        // Given
        ConnectRequestDto request = ConnectRequestDto.builder()
                .requestType(RequestType.CUSTOMER_ONBOARDING)
                .payload("<customer><name>John Doe</name><email>john@example.com</email></customer>")
                .contentType("application/xml")
                .clientId("test-client")
                .requestId("integration-test-123")
                .priority(5)
                .timeoutMs(300000L)
                .metadata(Map.of("test", "integration"))
                .build();

        // When & Then
        webTestClient.post()
                .uri("/connect/process")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isAccepted()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.requestId").isEqualTo("integration-test-123")
                .jsonPath("$.status").isEqualTo("ACCEPTED")
                .jsonPath("$.message").isEqualTo("Request accepted for processing");

        // Verify request was saved to database
        StepVerifier.create(requestRepository.findByRequestId("integration-test-123"))
                .expectNextMatches(savedRequest -> 
                        savedRequest.getRequestId().equals("integration-test-123") &&
                        savedRequest.getRequestType() == RequestType.CUSTOMER_ONBOARDING &&
                        savedRequest.getStatus() == ProcessingStatus.RECEIVED &&
                        savedRequest.getClientId().equals("test-client")
                )
                .verifyComplete();
    }

    @Test
    @WithMockUser
    void getRequestStatus_WithExistingRequest_ShouldReturnStatus() {
        // Given
        ProcessingRequest savedRequest = ProcessingRequest.builder()
                .requestId("status-test-123")
                .requestType(RequestType.CUSTOMER_ONBOARDING)
                .status(ProcessingStatus.COMPLETED)
                .originalPayload("<xml>test data</xml>")
                .convertedJsonPayload("{\"test\": \"data\"}")
                .externalApiResponse("{\"result\": \"success\"}")
                .fenergoResponse("{\"fenergo\": \"response\"}")
                .clientId("test-client")
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        requestRepository.save(savedRequest).block();

        // When & Then
        webTestClient.get()
                .uri("/connect/status/status-test-123")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.requestId").isEqualTo("status-test-123")
                .jsonPath("$.status").isEqualTo("completed")
                .jsonPath("$.message").isEqualTo("Processing completed successfully")
                .jsonPath("$.data.originalPayload").isEqualTo("<xml>test data</xml>")
                .jsonPath("$.data.convertedJsonPayload").isEqualTo("{\"test\": \"data\"}")
                .jsonPath("$.data.externalApiResponse").isEqualTo("{\"result\": \"success\"}")
                .jsonPath("$.data.fenergoResponse").isEqualTo("{\"fenergo\": \"response\"}");
    }

    @Test
    @WithMockUser
    void processRequest_WithInvalidData_ShouldReturnBadRequest() {
        // Given
        ConnectRequestDto invalidRequest = ConnectRequestDto.builder()
                .payload("test")
                .contentType("application/xml")
                .build();

        // When & Then
        webTestClient.post()
                .uri("/connect/process")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();
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

    @Test
    @WithMockUser
    void processRequest_WithDifferentRequestTypes_ShouldWork() {
        // Test different request types
        RequestType[] requestTypes = {
                RequestType.CUSTOMER_ONBOARDING,
                RequestType.KYC_VERIFICATION,
                RequestType.DOCUMENT_PROCESSING,
                RequestType.COMPLIANCE_CHECK,
                RequestType.RISK_ASSESSMENT,
                RequestType.TRANSACTION_MONITORING,
                RequestType.GENERIC_PROCESSING
        };

        for (RequestType requestType : requestTypes) {
            ConnectRequestDto request = ConnectRequestDto.builder()
                    .requestType(requestType)
                    .payload("<xml>test data for " + requestType + "</xml>")
                    .contentType("application/xml")
                    .clientId("test-client")
                    .requestId("test-" + requestType.name().toLowerCase())
                    .build();

            webTestClient.post()
                    .uri("/connect/process")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isAccepted()
                    .expectBody()
                    .jsonPath("$.requestId").isEqualTo("test-" + requestType.name().toLowerCase())
                    .jsonPath("$.status").isEqualTo("ACCEPTED");
        }
    }
}

