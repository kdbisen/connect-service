package com.adyanta.connect.service;

import com.adyanta.connect.domain.document.ProcessingRequest;
import com.adyanta.connect.domain.dto.ConnectRequestDto;
import com.adyanta.connect.domain.dto.ConnectResponseDto;
import com.adyanta.connect.domain.enums.ProcessingStatus;
import com.adyanta.connect.domain.enums.RequestType;
import com.adyanta.connect.processing.RequestProcessorFactory;
import com.adyanta.connect.processing.RequestProcessor;
import com.adyanta.connect.repository.ProcessingRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConnectService
 */
@ExtendWith(MockitoExtension.class)
class ConnectServiceTest {

    @Mock
    private RequestProcessorFactory processorFactory;

    @Mock
    private ProcessingRequestRepository requestRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private RequestProcessor requestProcessor;

    private ConnectService connectService;

    @BeforeEach
    void setUp() {
        connectService = new ConnectService(processorFactory, requestRepository, auditService);
    }

    @Test
    void processRequest_ShouldReturnAcceptedResponse() {
        // Given
        ConnectRequestDto requestDto = ConnectRequestDto.builder()
                .requestType(RequestType.CUSTOMER_ONBOARDING)
                .payload("<xml>test data</xml>")
                .contentType("application/xml")
                .clientId("test-client")
                .requestId("test-request-123")
                .priority(5)
                .timeoutMs(300000L)
                .metadata(Map.of("test", "value"))
                .build();

        ProcessingRequest savedRequest = ProcessingRequest.builder()
                .id("processing-123")
                .requestId("test-request-123")
                .requestType(RequestType.CUSTOMER_ONBOARDING)
                .originalPayload("<xml>test data</xml>")
                .contentType("application/xml")
                .clientId("test-client")
                .priority(5)
                .timeoutMs(300000L)
                .metadata(Map.of("test", "value"))
                .status(ProcessingStatus.RECEIVED)
                .createdAt(LocalDateTime.now())
                .build();

        when(requestRepository.save(any(ProcessingRequest.class)))
                .thenReturn(Mono.just(savedRequest));
        when(processorFactory.getProcessor(any(RequestType.class)))
                .thenReturn(requestProcessor);
        when(requestProcessor.process(any(ProcessingRequest.class)))
                .thenReturn(Mono.just(savedRequest));

        // When
        Mono<ConnectResponseDto> result = connectService.processRequest(requestDto);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> 
                        response.getRequestId().equals("test-request-123") &&
                        response.getStatus().equals("ACCEPTED") &&
                        response.getMessage().equals("Request accepted for processing") &&
                        response.getProcessingId().equals("processing-123")
                )
                .verifyComplete();

        verify(requestRepository).save(any(ProcessingRequest.class));
        verify(processorFactory).getProcessor(RequestType.CUSTOMER_ONBOARDING);
    }

    @Test
    void processRequest_WithNullRequestId_ShouldGenerateRequestId() {
        // Given
        ConnectRequestDto requestDto = ConnectRequestDto.builder()
                .requestType(RequestType.CUSTOMER_ONBOARDING)
                .payload("<xml>test data</xml>")
                .contentType("application/xml")
                .clientId("test-client")
                .build();

        ProcessingRequest savedRequest = ProcessingRequest.builder()
                .id("processing-123")
                .requestId("generated-request-id")
                .requestType(RequestType.CUSTOMER_ONBOARDING)
                .originalPayload("<xml>test data</xml>")
                .contentType("application/xml")
                .clientId("test-client")
                .status(ProcessingStatus.RECEIVED)
                .createdAt(LocalDateTime.now())
                .build();

        when(requestRepository.save(any(ProcessingRequest.class)))
                .thenReturn(Mono.just(savedRequest));
        when(processorFactory.getProcessor(any(RequestType.class)))
                .thenReturn(requestProcessor);
        when(requestProcessor.process(any(ProcessingRequest.class)))
                .thenReturn(Mono.just(savedRequest));

        // When
        Mono<ConnectResponseDto> result = connectService.processRequest(requestDto);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> 
                        response.getRequestId() != null &&
                        response.getStatus().equals("ACCEPTED")
                )
                .verifyComplete();
    }

    @Test
    void getRequestStatus_ShouldReturnRequestStatus() {
        // Given
        String requestId = "test-request-123";
        ProcessingRequest request = ProcessingRequest.builder()
                .id("processing-123")
                .requestId(requestId)
                .requestType(RequestType.CUSTOMER_ONBOARDING)
                .status(ProcessingStatus.COMPLETED)
                .originalPayload("<xml>test data</xml>")
                .convertedJsonPayload("{\"test\": \"data\"}")
                .externalApiResponse("{\"result\": \"success\"}")
                .fenergoResponse("{\"fenergo\": \"response\"}")
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(requestRepository.findByRequestId(requestId))
                .thenReturn(Mono.just(request));

        // When
        Mono<ConnectResponseDto> result = connectService.getRequestStatus(requestId);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> 
                        response.getRequestId().equals(requestId) &&
                        response.getStatus().equals("completed") &&
                        response.getMessage().equals("Processing completed successfully") &&
                        response.getProcessingId().equals("processing-123") &&
                        response.getData() != null
                )
                .verifyComplete();
    }

    @Test
    void getRequestStatus_WithNonExistentRequest_ShouldReturnError() {
        // Given
        String requestId = "non-existent";
        when(requestRepository.findByRequestId(requestId))
                .thenReturn(Mono.empty());

        // When
        Mono<ConnectResponseDto> result = connectService.getRequestStatus(requestId);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }
}
