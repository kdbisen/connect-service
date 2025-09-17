package com.adyanta.connect.service;

import com.adyanta.connect.domain.document.ProcessingRequest;
import com.adyanta.connect.domain.dto.ConnectRequestDto;
import com.adyanta.connect.domain.dto.ConnectResponseDto;
import com.adyanta.connect.domain.enums.ProcessingStatus;
import com.adyanta.connect.processing.RequestProcessorFactory;
import com.adyanta.connect.repository.ProcessingRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Main service for handling connect requests
 * This follows the Service Layer pattern
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectService {
    
    private final RequestProcessorFactory processorFactory;
    private final ProcessingRequestRepository requestRepository;
    private final AuditService auditService;
    
    /**
     * Process a connect request asynchronously (fire-and-forget)
     */
    public Mono<ConnectResponseDto> processRequest(ConnectRequestDto requestDto) {
        log.info("Processing connect request: {}", requestDto.getRequestId());
        
        // Generate request ID if not provided
        String requestId = requestDto.getRequestId() != null ? 
                requestDto.getRequestId() : 
                UUID.randomUUID().toString();
        
        // Create processing request document
        ProcessingRequest processingRequest = ProcessingRequest.builder()
                .requestId(requestId)
                .requestType(requestDto.getRequestType())
                .originalPayload(requestDto.getPayload())
                .contentType(requestDto.getContentType())
                .clientId(requestDto.getClientId())
                .priority(requestDto.getPriority())
                .timeoutMs(requestDto.getTimeoutMs())
                .metadata(requestDto.getMetadata())
                .status(ProcessingStatus.RECEIVED)
                .createdAt(LocalDateTime.now())
                .build();
        
        // Save initial request
        return requestRepository.save(processingRequest)
                .flatMap(savedRequest -> {
                    // Start async processing
                    processAsync(savedRequest);
                    
                    // Return immediate response
                    return Mono.just(ConnectResponseDto.builder()
                            .requestId(requestId)
                            .status("ACCEPTED")
                            .message("Request accepted for processing")
                            .timestamp(LocalDateTime.now())
                            .processingId(savedRequest.getId())
                            .build());
                })
                .doOnSuccess(response -> log.info("Request accepted for processing: {}", requestId))
                .doOnError(error -> log.error("Failed to accept request: {}", requestId, error));
    }
    
    /**
     * Process request asynchronously
     */
    private void processAsync(ProcessingRequest request) {
        processorFactory.getProcessor(request.getRequestType())
                .process(request)
                .flatMap(requestRepository::save)
                .doOnSuccess(result -> log.info("Async processing completed for request: {}", request.getRequestId()))
                .doOnError(error -> {
                    log.error("Async processing failed for request: {}", request.getRequestId(), error);
                    // Update request with error
                    requestRepository.save(request.toBuilder()
                            .status(ProcessingStatus.FAILED)
                            .errorMessage(error.getMessage())
                            .processingEndTime(LocalDateTime.now())
                            .build())
                            .subscribe();
                })
                .subscribe();
    }
    
    /**
     * Get request status by request ID
     */
    public Mono<ConnectResponseDto> getRequestStatus(String requestId) {
        return requestRepository.findByRequestId(requestId)
                .map(request -> ConnectResponseDto.builder()
                        .requestId(request.getRequestId())
                        .status(request.getStatus().getCode())
                        .message(request.getStatus().getDescription())
                        .timestamp(LocalDateTime.now())
                        .processingId(request.getId())
                        .data(java.util.Map.of(
                                "originalPayload", request.getOriginalPayload(),
                                "convertedJsonPayload", request.getConvertedJsonPayload(),
                                "externalApiResponse", request.getExternalApiResponse(),
                                "fenergoResponse", request.getFenergoResponse(),
                                "retryCount", request.getRetryCount(),
                                "createdAt", request.getCreatedAt(),
                                "updatedAt", request.getUpdatedAt()
                        ))
                        .error(request.getErrorMessage() != null ? 
                                ConnectResponseDto.ErrorDetails.builder()
                                        .message(request.getErrorMessage())
                                        .details(request.getStackTrace())
                                        .build() : null)
                        .build())
                .switchIfEmpty(Mono.error(new RuntimeException("Request not found: " + requestId)));
    }
}

