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
 * Payment processing processor using annotation-based routing
 * This demonstrates how to handle multiple request types with different priorities
 */
@Slf4j
@Component
@RequestProcessor("PaymentProcessor")
@RequiredArgsConstructor
public class PaymentProcessor {
    
    private final ProcessingPipeline processingPipeline;
    
    /**
     * Process payment requests
     * This method will be automatically called for PAYMENT_PROCESSING requests
     */
    @ProcessRequestType(
        value = {RequestType.PAYMENT_PROCESSING},
        priority = 20,
        description = "Handles payment processing requests with fraud checks and compliance"
    )
    public ProcessingRequest processPayment(ProcessingRequest request) {
        log.info("Processing payment request: {}", request.getRequestId());
        
        // Validate payment request
        validatePaymentRequest(request);
        
        // Add payment-specific metadata
        ProcessingRequest paymentRequest = request.toBuilder()
                .status(ProcessingStatus.PROCESSING)
                .processingStartTime(LocalDateTime.now())
                .metadata(addPaymentMetadata(request.getMetadata()))
                .build();
        
        return processingPipeline.execute(paymentRequest);
    }
    
    /**
     * Process refund requests
     * This method will be automatically called for REFUND_PROCESSING requests
     */
    @ProcessRequestType(
        value = {RequestType.REFUND_PROCESSING},
        priority = 15,
        description = "Handles refund processing requests"
    )
    public ProcessingRequest processRefund(ProcessingRequest request) {
        log.info("Processing refund request: {}", request.getRequestId());
        
        // Add refund-specific metadata
        ProcessingRequest refundRequest = request.toBuilder()
                .status(ProcessingStatus.PROCESSING)
                .processingStartTime(LocalDateTime.now())
                .metadata(addRefundMetadata(request.getMetadata()))
                .build();
        
        return processingPipeline.execute(refundRequest);
    }
    
    /**
     * Process high-value payment requests with additional checks
     * This method will be called for HIGH_VALUE_PAYMENT requests
     */
    @ProcessRequestType(
        value = {RequestType.HIGH_VALUE_PAYMENT},
        priority = 30,
        description = "Handles high-value payment requests with enhanced security checks"
    )
    public ProcessingRequest processHighValuePayment(ProcessingRequest request) {
        log.info("Processing high-value payment request: {}", request.getRequestId());
        
        // Add high-value payment specific metadata
        ProcessingRequest highValueRequest = request.toBuilder()
                .status(ProcessingStatus.PROCESSING)
                .processingStartTime(LocalDateTime.now())
                .metadata(addHighValuePaymentMetadata(request.getMetadata()))
                .build();
        
        return processingPipeline.execute(highValueRequest);
    }
    
    private void validatePaymentRequest(ProcessingRequest request) {
        if (request.getMetadata() == null || 
            !request.getMetadata().containsKey("paymentAmount")) {
            throw new IllegalArgumentException("Payment amount is required for payment processing");
        }
        
        // Additional payment validation logic
        Object amount = request.getMetadata().get("paymentAmount");
        if (amount instanceof Number) {
            double paymentAmount = ((Number) amount).doubleValue();
            if (paymentAmount <= 0) {
                throw new IllegalArgumentException("Payment amount must be positive");
            }
        }
    }
    
    private Map<String, Object> addPaymentMetadata(Map<String, Object> existingMetadata) {
        if (existingMetadata == null) {
            existingMetadata = new HashMap<>();
        }
        existingMetadata.put("processType", "payment_processing");
        existingMetadata.put("requiresFraudCheck", true);
        existingMetadata.put("requiresCompliance", true);
        existingMetadata.put("processor", "PaymentProcessor");
        return existingMetadata;
    }
    
    private Map<String, Object> addRefundMetadata(Map<String, Object> existingMetadata) {
        if (existingMetadata == null) {
            existingMetadata = new HashMap<>();
        }
        existingMetadata.put("processType", "refund_processing");
        existingMetadata.put("requiresApproval", true);
        existingMetadata.put("processor", "PaymentProcessor");
        return existingMetadata;
    }
    
    private Map<String, Object> addHighValuePaymentMetadata(Map<String, Object> existingMetadata) {
        if (existingMetadata == null) {
            existingMetadata = new HashMap<>();
        }
        existingMetadata.put("processType", "high_value_payment");
        existingMetadata.put("requiresFraudCheck", true);
        existingMetadata.put("requiresCompliance", true);
        existingMetadata.put("requiresApproval", true);
        existingMetadata.put("requiresEnhancedSecurity", true);
        existingMetadata.put("processor", "PaymentProcessor");
        return existingMetadata;
    }
}
