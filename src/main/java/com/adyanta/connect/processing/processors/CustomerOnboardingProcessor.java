package com.adyanta.connect.processing.processors;

import com.adyanta.connect.domain.document.ProcessingRequest;
import com.adyanta.connect.domain.enums.ProcessingStatus;
import com.adyanta.connect.domain.enums.RequestType;
import com.adyanta.connect.processing.ProcessingPipeline;
import com.adyanta.connect.processing.RequestProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Specialized processor for customer onboarding requests
 * This demonstrates how to create specialized processors for specific request types
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerOnboardingProcessor implements RequestProcessor {
    
    private final ProcessingPipeline processingPipeline;
    
    @Override
    public boolean canHandle(RequestType requestType) {
        return requestType == RequestType.CUSTOMER_ONBOARDING;
    }
    
    @Override
    public String getName() {
        return "CustomerOnboardingProcessor";
    }
    
    @Override
    public int getPriority() {
        return 10; // Higher priority than generic processor
    }
    
    @Override
    public Mono<ProcessingRequest> process(ProcessingRequest request) {
        log.info("Processing customer onboarding request: {}", request.getRequestId());
        
        // Add customer onboarding specific metadata
        ProcessingRequest onboardingRequest = request.toBuilder()
                .status(ProcessingStatus.PROCESSING)
                .processingStartTime(LocalDateTime.now())
                .metadata(addCustomerOnboardingMetadata(request.getMetadata()))
                .build();
        
        return processingPipeline.execute(onboardingRequest)
                .doOnSuccess(result -> log.info("Customer onboarding processing completed for request: {}", request.getRequestId()))
                .doOnError(error -> {
                    log.error("Customer onboarding processing failed for request: {}", request.getRequestId(), error);
                    // Add customer onboarding specific error handling
                });
    }
    
    private java.util.Map<String, Object> addCustomerOnboardingMetadata(java.util.Map<String, Object> existingMetadata) {
        if (existingMetadata == null) {
            existingMetadata = new java.util.HashMap<>();
        }
        existingMetadata.put("processType", "customer_onboarding");
        existingMetadata.put("requiresKyc", true);
        existingMetadata.put("requiresCompliance", true);
        return existingMetadata;
    }
}

