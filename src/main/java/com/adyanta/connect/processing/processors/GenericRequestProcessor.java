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
 * Generic request processor that handles all request types
 * This follows the Template Method pattern
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GenericRequestProcessor implements RequestProcessor {
    
    private final ProcessingPipeline processingPipeline;
    
    @Override
    public boolean canHandle(RequestType requestType) {
        return true; // Generic processor can handle all types
    }
    
    @Override
    public String getName() {
        return "GenericRequestProcessor";
    }
    
    @Override
    public int getPriority() {
        return 0; // Lowest priority, used as fallback
    }
    
    @Override
    public Mono<ProcessingRequest> process(ProcessingRequest request) {
        log.info("Processing request with generic processor: {}", request.getRequestId());
        
        // Mark as processing
        ProcessingRequest processingRequest = request.toBuilder()
                .status(ProcessingStatus.PROCESSING)
                .processingStartTime(LocalDateTime.now())
                .build();
        
        return processingPipeline.execute(processingRequest)
                .doOnSuccess(result -> log.info("Generic processing completed for request: {}", request.getRequestId()))
                .doOnError(error -> {
                    log.error("Generic processing failed for request: {}", request.getRequestId(), error);
                    // Mark as failed
                    result.toBuilder()
                            .status(ProcessingStatus.FAILED)
                            .errorMessage(error.getMessage())
                            .processingEndTime(LocalDateTime.now())
                            .build();
                });
    }
}
