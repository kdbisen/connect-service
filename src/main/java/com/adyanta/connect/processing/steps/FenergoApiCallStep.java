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
 * Processing step for calling Fenergo API
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FenergoApiCallStep implements ProcessingStep {
    
    private final ExternalApiClient externalApiClient;
    
    @Override
    public String getStepName() {
        return "FENERGO_API_CALL";
    }
    
    @Override
    public int getOrder() {
        return 30;
    }
    
    @Override
    public boolean shouldExecute(ProcessingRequest request) {
        return request.getExternalApiResponse() != null && 
               request.getFenergoResponse() == null;
    }
    
    @Override
    public Mono<ProcessingRequest> execute(ProcessingRequest request) {
        log.debug("Calling Fenergo API for request: {}", request.getRequestId());
        
        String payload = request.getExternalApiResponse() != null ? 
                request.getExternalApiResponse() : 
                request.getConvertedJsonPayload();
        
        return externalApiClient.callFenergoApi(payload, request.getClientId())
                .map(response -> request.toBuilder()
                        .fenergoResponse(response)
                        .status(ProcessingStatus.FENERGO_CALLED)
                        .build())
                .doOnSuccess(result -> log.debug("Fenergo API call completed for request: {}", request.getRequestId()))
                .doOnError(error -> log.error("Fenergo API call failed for request: {}", request.getRequestId(), error));
    }
}
