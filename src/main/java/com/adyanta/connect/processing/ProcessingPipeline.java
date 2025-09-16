package com.adyanta.connect.processing;

import com.adyanta.connect.domain.document.ProcessingRequest;
import com.adyanta.connect.domain.enums.ProcessingStatus;
import com.adyanta.connect.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Processing pipeline that executes steps in order
 * This follows the Chain of Responsibility pattern
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessingPipeline {
    
    private final List<ProcessingStep> processingSteps;
    private final AuditService auditService;
    
    /**
     * Execute the processing pipeline
     */
    public Mono<ProcessingRequest> execute(ProcessingRequest request) {
        log.debug("Starting processing pipeline for request: {}", request.getRequestId());
        
        // Sort steps by order
        List<ProcessingStep> sortedSteps = processingSteps.stream()
                .filter(step -> step.shouldExecute(request))
                .sorted((s1, s2) -> Integer.compare(s1.getOrder(), s2.getOrder()))
                .collect(Collectors.toList());
        
        return executeSteps(request, sortedSteps, 0)
                .doOnSuccess(result -> log.debug("Processing pipeline completed for request: {}", request.getRequestId()))
                .doOnError(error -> log.error("Processing pipeline failed for request: {}", request.getRequestId(), error));
    }
    
    private Mono<ProcessingRequest> executeSteps(ProcessingRequest request, List<ProcessingStep> steps, int index) {
        if (index >= steps.size()) {
            return Mono.just(request);
        }
        
        ProcessingStep step = steps.get(index);
        log.debug("Executing step: {} for request: {}", step.getStepName(), request.getRequestId());
        
        return auditService.logStepStart(request.getId(), step.getStepName())
                .then(step.execute(request))
                .flatMap(updatedRequest -> auditService.logStepSuccess(request.getId(), step.getStepName(), updatedRequest))
                .onErrorResume(error -> {
                    log.error("Step {} failed for request: {}", step.getStepName(), request.getRequestId(), error);
                    return auditService.logStepFailure(request.getId(), step.getStepName(), error.getMessage())
                            .then(Mono.just(request.toBuilder()
                                    .status(ProcessingStatus.FAILED)
                                    .errorMessage(error.getMessage())
                                    .processingEndTime(LocalDateTime.now())
                                    .build()));
                })
                .flatMap(updatedRequest -> executeSteps(updatedRequest, steps, index + 1));
    }
}
