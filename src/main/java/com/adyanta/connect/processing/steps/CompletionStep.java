package com.adyanta.connect.processing.steps;

import com.adyanta.connect.domain.document.ProcessingRequest;
import com.adyanta.connect.domain.enums.ProcessingStatus;
import com.adyanta.connect.processing.ProcessingStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Processing step for marking request as completed
 */
@Slf4j
@Component
public class CompletionStep implements ProcessingStep {
    
    @Override
    public String getStepName() {
        return "COMPLETION";
    }
    
    @Override
    public int getOrder() {
        return 100;
    }
    
    @Override
    public boolean shouldExecute(ProcessingRequest request) {
        return request.getFenergoResponse() != null && 
               request.getStatus() != ProcessingStatus.COMPLETED;
    }
    
    @Override
    public Mono<ProcessingRequest> execute(ProcessingRequest request) {
        log.debug("Marking request as completed: {}", request.getRequestId());
        
        return Mono.just(request.toBuilder()
                .status(ProcessingStatus.COMPLETED)
                .processingEndTime(LocalDateTime.now())
                .build())
                .doOnSuccess(result -> log.info("Request processing completed: {}", request.getRequestId()));
    }
}
