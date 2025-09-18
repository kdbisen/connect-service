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
import org.springframework.scheduling.annotation.Async;
import java.util.concurrent.CompletableFuture;

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
    public CompletableFuture<ConnectResponseDto> processRequestAsync(ConnectRequestDto requestDto) {
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
        
        try {
            // Save initial request
            ProcessingRequest savedRequest = requestRepository.save(processingRequest);
            
            // Start async processing
            processAsync(savedRequest);
            
            // Return immediate response
            ConnectResponseDto response = ConnectResponseDto.builder()
                    .requestId(requestId)
                    .status("ACCEPTED")
                    .message("Request accepted for processing")
                    .timestamp(LocalDateTime.now())
                    .processingId(savedRequest.getId())
                    .build();
            
            log.info("Request accepted for processing: {}", requestId);
            return CompletableFuture.completedFuture(response);
            
        } catch (Exception error) {
            log.error("Failed to accept request: {}", requestId, error);
            return CompletableFuture.failedFuture(error);
        }
    }
    
    /**
     * Process request asynchronously
     */
    @Async("processingExecutor")
    public void processAsync(ProcessingRequest request) {
        try {
            log.info("Starting async processing for request: {}", request.getRequestId());
            
            // Update status to processing
            request.setStatus(ProcessingStatus.PROCESSING);
            request.setUpdatedAt(LocalDateTime.now());
            requestRepository.save(request);
            
            // Process the request
            ProcessingRequest result = processorFactory.getProcessor(request.getRequestType())
                    .process(request);
            
            // Save the result
            requestRepository.save(result);
            
            log.info("Async processing completed for request: {}", request.getRequestId());
            
        } catch (Exception error) {
            log.error("Async processing failed for request: {}", request.getRequestId(), error);
            
            // Update request with error
            request.setStatus(ProcessingStatus.FAILED);
            request.setErrorMessage(error.getMessage());
            request.setStackTrace(getStackTrace(error));
            request.setProcessingEndTime(LocalDateTime.now());
            request.setUpdatedAt(LocalDateTime.now());
            requestRepository.save(request);
        }
    }
    
    /**
     * Get stack trace as string
     */
    private String getStackTrace(Exception error) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        error.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * Get request status by request ID
     */
    public ConnectResponseDto getRequestStatus(String requestId) {
        ProcessingRequest request = requestRepository.findByRequestId(requestId);
        
        if (request == null) {
            throw new RuntimeException("Request not found: " + requestId);
        }
        
        return ConnectResponseDto.builder()
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
                .build();
    }
}

