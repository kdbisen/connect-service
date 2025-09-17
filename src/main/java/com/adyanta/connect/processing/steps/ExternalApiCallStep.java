package com.adyanta.connect.processing.steps;

import com.adyanta.connect.domain.document.ProcessingRequest;
import com.adyanta.connect.domain.enums.ProcessingStatus;
import com.adyanta.connect.processing.ProcessingStep;
import com.adyanta.connect.service.ExternalApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Processing step for calling external API
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalApiCallStep implements ProcessingStep {
    
    private final ExternalApiClient externalApiClient;
    
    @Override
    public String getStepName() {
        return "EXTERNAL_API_CALL";
    }
    
    @Override
    public int getOrder() {
        return 20;
    }
    
    @Override
    public boolean shouldExecute(ProcessingRequest request) {
        return request.getConvertedJsonPayload() != null && 
               request.getExternalApiResponse() == null;
    }
    
    @Override
    public Mono<ProcessingRequest> execute(ProcessingRequest request) {
        log.debug("Calling external API for request: {}", request.getRequestId());
        
        String payload = request.getConvertedJsonPayload() != null ? 
                request.getConvertedJsonPayload() : 
                request.getOriginalPayload();
        
        return externalApiClient.convertXmlToJson(payload, request.getClientId())
                .map(response -> request.toBuilder()
                        .externalApiResponse(response)
                        .status(ProcessingStatus.EXTERNAL_API_CALLED)
                        .build())
                .doOnSuccess(result -> log.debug("External API call completed for request: {}", request.getRequestId()))
                .doOnError(error -> log.error("External API call failed for request: {}", request.getRequestId(), error));
    }
}

