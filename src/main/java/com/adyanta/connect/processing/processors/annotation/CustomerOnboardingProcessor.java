package com.adyanta.connect.processing.processors.annotation;

import com.adyanta.connect.domain.document.ProcessingRequest;
import com.adyanta.connect.domain.enums.ProcessingStatus;
import com.adyanta.connect.domain.enums.RequestType;
import com.adyanta.connect.processing.ProcessingPipeline;
import com.adyanta.connect.processing.annotation.ProcessRequestType;
import com.adyanta.connect.processing.annotation.RequestProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Customer onboarding processor using annotation-based routing
 * This demonstrates how to use @ProcessRequestType annotation
 */
@Slf4j
@Component
@RequestProcessor("CustomerOnboardingProcessor")
@RequiredArgsConstructor
public class CustomerOnboardingProcessor {
    
    private final ProcessingPipeline processingPipeline;
    
    /**
     * Process customer onboarding requests
     * This method will be automatically called for CUSTOMER_ONBOARDING requests
     */
    @ProcessRequestType(
        value = {RequestType.CUSTOMER_ONBOARDING},
        priority = 10,
        description = "Handles customer onboarding requests with KYC and compliance checks"
    )
    public ProcessingRequest processCustomerOnboarding(ProcessingRequest request) {
        log.info("Processing customer onboarding request: {}", request.getRequestId());
        
        // Add customer onboarding specific metadata
        ProcessingRequest onboardingRequest = request.toBuilder()
                .status(ProcessingStatus.PROCESSING)
                .processingStartTime(LocalDateTime.now())
                .metadata(addCustomerOnboardingMetadata(request.getMetadata()))
                .build();
        
        // Process through the pipeline
        return processingPipeline.execute(onboardingRequest);
    }
    
    /**
     * Process generic requests as fallback
     * This method will be called for any request type if no specific processor is found
     */
    @ProcessRequestType(
        value = {RequestType.GENERIC_PROCESSING},
        priority = 0,
        description = "Fallback processor for generic requests"
    )
    public ProcessingRequest processGeneric(ProcessingRequest request) {
        log.info("Processing generic request: {}", request.getRequestId());
        
        ProcessingRequest genericRequest = request.toBuilder()
                .status(ProcessingStatus.PROCESSING)
                .processingStartTime(LocalDateTime.now())
                .build();
        
        return processingPipeline.execute(genericRequest);
    }
    
    private Map<String, Object> addCustomerOnboardingMetadata(Map<String, Object> existingMetadata) {
        if (existingMetadata == null) {
            existingMetadata = new HashMap<>();
        }
        existingMetadata.put("processType", "customer_onboarding");
        existingMetadata.put("requiresKyc", true);
        existingMetadata.put("requiresCompliance", true);
        existingMetadata.put("processor", "CustomerOnboardingProcessor");
        return existingMetadata;
    }
}
