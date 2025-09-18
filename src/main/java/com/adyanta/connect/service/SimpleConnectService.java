package com.adyanta.connect.service;

import com.adyanta.connect.domain.document.ProcessingRequest;
import com.adyanta.connect.domain.dto.ConnectRequestDto;
import com.adyanta.connect.domain.dto.ConnectResponseDto;
import com.adyanta.connect.domain.enums.ProcessingStatus;
import com.adyanta.connect.processing.annotation.AnnotationBasedProcessorRegistry;
import com.adyanta.connect.processing.annotation.AnnotationBasedProcessorRegistry.ProcessorMethod;
import com.adyanta.connect.repository.ProcessingRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Simple Connect Service for ADD_KYC implementation
 * Uses synchronous Spring Boot with @Async for background processing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimpleConnectService {
    
    private final ProcessingRequestRepository requestRepository;
    private final AnnotationBasedProcessorRegistry processorRegistry;
    
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
            
            // Process using annotation-based processor registry
            ProcessingRequest result = processWithAnnotationBasedRegistry(request);
            
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
     * Process request using annotation-based processor registry
     */
    private ProcessingRequest processWithAnnotationBasedRegistry(ProcessingRequest request) {
        try {
            log.info("Processing request using annotation-based registry: {}", request.getRequestId());
            
            // Find the appropriate processor method for this request type
            Optional<ProcessorMethod> processorMethodOptional = processorRegistry.getProcessorMethod(request.getRequestType());
            
            if (processorMethodOptional.isPresent()) {
                ProcessorMethod processorMethod = processorMethodOptional.get();
                log.debug("Invoking processor method: {} for request type: {}",
                        processorMethod.getMethod().getName(), request.getRequestType());
                
                // Invoke the method using reflection
                ProcessingRequest result = (ProcessingRequest) processorMethod.getMethod().invoke(processorMethod.getBean(), request);
                
                log.info("Annotation-based processing completed for request: {}", request.getRequestId());
                return result;
            } else {
                String errorMessage = "No annotation-based processor found for request type: " + request.getRequestType();
                log.error(errorMessage);
                return request.toBuilder()
                        .status(ProcessingStatus.FAILED)
                        .errorMessage(errorMessage)
                        .processingEndTime(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
            }
            
        } catch (Exception error) {
            log.error("Annotation-based processing failed for request: {}", request.getRequestId(), error);
            return request.toBuilder()
                    .status(ProcessingStatus.FAILED)
                    .errorMessage(error.getMessage())
                    .stackTrace(getStackTrace(error))
                    .processingEndTime(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
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
