package com.adyanta.connect.service;

import com.adyanta.connect.domain.document.ProcessingRequest;
import com.adyanta.connect.domain.enums.ProcessingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * KYC Processor for ADD_KYC request type
 * Implements the 4-step processing workflow:
 * 1. Call misc service converter API (synchronous)
 * 2. Store input payload and converted data
 * 3. Call Fenergo API with converted data
 * 4. Store Fenergo response
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KycProcessor_SIMPLE {
    
    private final RestTemplate restTemplate;
    private final ExternalApiClient externalApiClient;
    private final FenergoApiClient fenergoApiClient;
    
    /**
     * Process ADD_KYC requests
     */
    public ProcessingRequest processAddKyc(ProcessingRequest request) {
        log.info("Processing ADD_KYC request: {}", request.getRequestId());
        
        try {
            // Step 1: Call misc service converter API (synchronous)
            log.info("Step 1: Calling misc service converter API for request: {}", request.getRequestId());
            String convertedJsonData = callMiscServiceConverter(request);
            
            // Update request with converted data
            ProcessingRequest updatedRequest = request.toBuilder()
                    .status(ProcessingStatus.PROCESSING)
                    .processingStartTime(LocalDateTime.now())
                    .convertedJsonPayload(convertedJsonData)
                    .metadata(addKycMetadata(request.getMetadata()))
                    .build();
            
            log.info("Step 1 completed: Misc service conversion successful for request: {}", request.getRequestId());
            
            // Step 2: Store input payload and converted data (already done above)
            log.info("Step 2: Storing input payload and converted data for request: {}", request.getRequestId());
            
            // Step 3: Call Fenergo API with converted data
            log.info("Step 3: Calling Fenergo API for request: {}", request.getRequestId());
            String fenergoResponse = callFenergoApi(convertedJsonData, request);
            
            // Step 4: Store Fenergo response
            log.info("Step 4: Storing Fenergo response for request: {}", request.getRequestId());
            ProcessingRequest finalRequest = updatedRequest.toBuilder()
                    .fenergoResponse(fenergoResponse)
                    .status(ProcessingStatus.COMPLETED)
                    .processingEndTime(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            log.info("ADD_KYC processing completed successfully for request: {}", request.getRequestId());
            return finalRequest;
            
        } catch (Exception error) {
            log.error("ADD_KYC processing failed for request: {}", request.getRequestId(), error);
            
            // Store error information
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
     * Step 1: Call misc service converter API synchronously
     */
    private String callMiscServiceConverter(ProcessingRequest request) {
        try {
            log.debug("Calling misc service converter API for request: {}", request.getRequestId());
            
            // Prepare request parameters
            Map<String, Object> requestParams = new HashMap<>();
            requestParams.put("requestType", request.getRequestType().getCode());
            requestParams.put("payload", request.getOriginalPayload());
            requestParams.put("requestId", request.getRequestId());
            requestParams.put("clientId", request.getClientId());
            
            // Add optional metadata
            if (request.getMetadata() != null) {
                requestParams.putAll(request.getMetadata());
            }
            
            // Call misc service converter API
            String miscServiceUrl = externalApiClient.getMiscServiceUrl() + "/api/v1/misc/service/converter";
            
            log.debug("Calling misc service URL: {} with params: {}", miscServiceUrl, requestParams);
            
            // Make synchronous GET request
            String response = restTemplate.getForObject(
                miscServiceUrl + "?requestType={requestType}&payload={payload}&requestId={requestId}&clientId={clientId}",
                String.class,
                request.getRequestType().getCode(),
                request.getOriginalPayload(),
                request.getRequestId(),
                request.getClientId()
            );
            
            if (response == null || response.trim().isEmpty()) {
                throw new RuntimeException("Misc service converter returned empty response");
            }
            
            log.info("Misc service converter API call successful for request: {}", request.getRequestId());
            return response;
            
        } catch (Exception e) {
            log.error("Failed to call misc service converter API for request: {}", request.getRequestId(), e);
            throw new RuntimeException("Misc service converter API call failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Step 3: Call Fenergo API with converted data
     */
    private String callFenergoApi(String convertedJsonData, ProcessingRequest request) {
        try {
            log.debug("Calling Fenergo API for request: {}", request.getRequestId());
            
            // Prepare Fenergo request
            Map<String, Object> fenergoRequest = new HashMap<>();
            fenergoRequest.put("requestId", request.getRequestId());
            fenergoRequest.put("requestType", request.getRequestType().getCode());
            fenergoRequest.put("convertedData", convertedJsonData);
            fenergoRequest.put("clientId", request.getClientId());
            fenergoRequest.put("timestamp", LocalDateTime.now().toString());
            
            // Add metadata if available
            if (request.getMetadata() != null) {
                fenergoRequest.put("metadata", request.getMetadata());
            }
            
            // Call Fenergo API
            String fenergoResponse = fenergoApiClient.callFenergoApi(fenergoRequest);
            
            if (fenergoResponse == null || fenergoResponse.trim().isEmpty()) {
                throw new RuntimeException("Fenergo API returned empty response");
            }
            
            log.info("Fenergo API call successful for request: {}", request.getRequestId());
            return fenergoResponse;
            
        } catch (Exception e) {
            log.error("Failed to call Fenergo API for request: {}", request.getRequestId(), e);
            throw new RuntimeException("Fenergo API call failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Add KYC-specific metadata
     */
    private Map<String, Object> addKycMetadata(Map<String, Object> existingMetadata) {
        if (existingMetadata == null) {
            existingMetadata = new HashMap<>();
        }
        existingMetadata.put("processType", "add_kyc");
        existingMetadata.put("requiresMiscServiceConversion", true);
        existingMetadata.put("requiresFenergoApiCall", true);
        existingMetadata.put("processor", "KycProcessor");
        existingMetadata.put("processingSteps", new String[]{
            "misc_service_conversion",
            "data_storage",
            "fenergo_api_call",
            "response_storage"
        });
        return existingMetadata;
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
