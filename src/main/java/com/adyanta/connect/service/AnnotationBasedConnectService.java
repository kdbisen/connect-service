package com.adyanta.connect.service;

import com.adyanta.connect.domain.document.ProcessingRequest;
import com.adyanta.connect.domain.dto.ConnectRequestDto;
import com.adyanta.connect.domain.dto.ConnectResponseDto;
import com.adyanta.connect.domain.enums.ProcessingStatus;
import com.adyanta.connect.processing.annotation.AnnotationBasedProcessorRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Connect Service using annotation-based request processing
 * This replaces the factory pattern with annotation-based routing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnotationBasedConnectService {
    
    private final AnnotationBasedProcessorRegistry processorRegistry;
    private final ProcessingRequestRepository requestRepository;
    private final AuditService auditService;
    
    /**
     * Process a connect request asynchronously using annotation-based routing
     */
    public CompletableFuture<ConnectResponseDto> processRequestAsync(ConnectRequestDto requestDto) {
        log.info("Processing connect request with annotation-based routing: {}", requestDto.getRequestId());
        
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
            
            // Start async processing using annotation-based routing
            processAsync(savedRequest);
            
            // Return immediate response
            ConnectResponseDto response = ConnectResponseDto.builder()
                    .requestId(requestId)
                    .status("ACCEPTED")
                    .message("Request accepted for processing (annotation-based)")
                    .timestamp(LocalDateTime.now())
                    .processingId(savedRequest.getId())
                    .build();
            
            log.info("Request accepted for annotation-based processing: {}", requestId);
            return CompletableFuture.completedFuture(response);
            
        } catch (Exception error) {
            log.error("Failed to accept request: {}", requestId, error);
            return CompletableFuture.failedFuture(error);
        }
    }
    
    /**
     * Process request asynchronously using annotation-based routing
     */
    @Async("processingExecutor")
    public void processAsync(ProcessingRequest request) {
        try {
            log.info("Starting annotation-based async processing for request: {}", request.getRequestId());
            
            // Update status to processing
            request.setStatus(ProcessingStatus.PROCESSING);
            request.setUpdatedAt(LocalDateTime.now());
            requestRepository.save(request);
            
            // Process the request using annotation-based routing
            ProcessingRequest result = processorRegistry.processRequest(request);
            
            // Save the result
            requestRepository.save(result);
            
            log.info("Annotation-based async processing completed for request: {}", request.getRequestId());
            
        } catch (Exception error) {
            log.error("Annotation-based async processing failed for request: {}", request.getRequestId(), error);
            
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
    
    /**
     * Get stack trace as string
     */
    private String getStackTrace(Exception error) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        error.printStackTrace(pw);
        return sw.toString();
    }
}
