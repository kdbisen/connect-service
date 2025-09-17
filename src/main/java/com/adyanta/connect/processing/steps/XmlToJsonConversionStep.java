package com.adyanta.connect.processing.steps;

import com.adyanta.connect.domain.document.ProcessingRequest;
import com.adyanta.connect.domain.enums.ProcessingStatus;
import com.adyanta.connect.processing.ProcessingStep;
import com.adyanta.connect.service.ExternalApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Processing step for XML to JSON conversion
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class XmlToJsonConversionStep implements ProcessingStep {
    
    private final ExternalApiClient externalApiClient;
    
    @Override
    public String getStepName() {
        return "XML_TO_JSON_CONVERSION";
    }
    
    @Override
    public int getOrder() {
        return 10;
    }
    
    @Override
    public boolean shouldExecute(ProcessingRequest request) {
        return "application/xml".equals(request.getContentType()) && 
               request.getConvertedJsonPayload() == null;
    }
    
    @Override
    public Mono<ProcessingRequest> execute(ProcessingRequest request) {
        log.debug("Converting XML to JSON for request: {}", request.getRequestId());
        
        return externalApiClient.convertXmlToJson(request.getOriginalPayload(), request.getClientId())
                .map(convertedJson -> request.toBuilder()
                        .convertedJsonPayload(convertedJson)
                        .status(ProcessingStatus.XML_CONVERTED)
                        .build())
                .doOnSuccess(result -> log.debug("XML to JSON conversion completed for request: {}", request.getRequestId()))
                .doOnError(error -> log.error("XML to JSON conversion failed for request: {}", request.getRequestId(), error));
    }
}

