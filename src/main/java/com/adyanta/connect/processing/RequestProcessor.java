package com.adyanta.connect.processing;

import com.adyanta.connect.domain.document.ProcessingRequest;
import com.adyanta.connect.domain.enums.RequestType;
import reactor.core.publisher.Mono;

/**
 * Strategy interface for processing different types of requests
 * This follows the Strategy pattern for extensible request handling
 */
public interface RequestProcessor {
    
    /**
     * Check if this processor can handle the given request type
     */
    boolean canHandle(RequestType requestType);
    
    /**
     * Process the request
     */
    Mono<ProcessingRequest> process(ProcessingRequest request);
    
    /**
     * Get the priority of this processor (higher number = higher priority)
     */
    default int getPriority() {
        return 0;
    }
    
    /**
     * Get the name of this processor
     */
    String getName();
}

