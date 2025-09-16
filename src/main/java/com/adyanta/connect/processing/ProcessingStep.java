package com.adyanta.connect.processing;

import com.adyanta.connect.domain.document.ProcessingRequest;
import reactor.core.publisher.Mono;

/**
 * Interface for individual processing steps in the pipeline
 * This follows the Chain of Responsibility pattern
 */
public interface ProcessingStep {
    
    /**
     * Execute the processing step
     */
    Mono<ProcessingRequest> execute(ProcessingRequest request);
    
    /**
     * Get the step name for logging and auditing
     */
    String getStepName();
    
    /**
     * Check if this step should be executed for the given request
     */
    default boolean shouldExecute(ProcessingRequest request) {
        return true;
    }
    
    /**
     * Get the order of this step in the pipeline (lower number = earlier execution)
     */
    default int getOrder() {
        return 0;
    }
}
