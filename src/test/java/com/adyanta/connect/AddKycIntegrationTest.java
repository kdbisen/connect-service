package com.adyanta.connect;

import com.adyanta.connect.domain.document.ProcessingRequest;
import com.adyanta.connect.domain.dto.ConnectRequestDto;
import com.adyanta.connect.domain.dto.ConnectResponseDto;
import com.adyanta.connect.domain.enums.RequestType;
import com.adyanta.connect.repository.ProcessingRequestRepository;
import com.adyanta.connect.service.SimpleConnectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for ADD_KYC functionality
 */
@SpringBootTest(classes = SimpleConnectServiceApplication.class)
@ActiveProfiles("test")
public class AddKycIntegrationTest {

    @Autowired
    private SimpleConnectService connectService;

    @Autowired
    private ProcessingRequestRepository requestRepository;

    @Test
    void shouldProcessAddKycRequest() throws Exception {
        // Given
        ConnectRequestDto request = ConnectRequestDto.builder()
                .requestType(RequestType.ADD_KYC)
                .payload("<kyc><customer><name>John Doe</name><id>12345</id></customer></kyc>")
                .contentType("application/xml")
                .clientId("client-123")
                .requestId("req-kyc-001")
                .metadata(createKycMetadata())
                .build();

        // When
        CompletableFuture<ConnectResponseDto> responseFuture = connectService.processRequestAsync(request);
        ConnectResponseDto response = responseFuture.get();

        // Then
        assertThat(response.getRequestId()).isEqualTo("req-kyc-001");
        assertThat(response.getStatus()).isEqualTo("ACCEPTED");
        assertThat(response.getMessage()).isEqualTo("Request accepted for processing");

        // Wait a bit for async processing
        Thread.sleep(2000);

        // Check the request was saved
        ProcessingRequest savedRequest = requestRepository.findByRequestId("req-kyc-001");
        assertThat(savedRequest).isNotNull();
        assertThat(savedRequest.getRequestType()).isEqualTo(RequestType.ADD_KYC);
        assertThat(savedRequest.getOriginalPayload()).contains("John Doe");
    }

    @Test
    void shouldGetRequestStatus() throws Exception {
        // Given - create a request first
        ConnectRequestDto request = ConnectRequestDto.builder()
                .requestType(RequestType.ADD_KYC)
                .payload("<kyc><customer><name>Jane Doe</name><id>67890</id></customer></kyc>")
                .contentType("application/xml")
                .clientId("client-456")
                .requestId("req-kyc-002")
                .metadata(createKycMetadata())
                .build();

        CompletableFuture<ConnectResponseDto> responseFuture = connectService.processRequestAsync(request);
        ConnectResponseDto response = responseFuture.get();

        // When
        ConnectResponseDto statusResponse = connectService.getRequestStatus("req-kyc-002");

        // Then
        assertThat(statusResponse.getRequestId()).isEqualTo("req-kyc-002");
        assertThat(statusResponse.getStatus()).isIn("PROCESSING", "COMPLETED", "FAILED");
    }

    private Map<String, Object> createKycMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("customerId", "12345");
        metadata.put("kycType", "individual");
        metadata.put("priority", "high");
        return metadata;
    }
}
