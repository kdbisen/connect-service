package com.adyanta.connect.processing;

import com.adyanta.connect.domain.enums.RequestType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Factory for creating request processors based on request type
 * This follows the Factory pattern
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestProcessorFactory {
    
    private final List<RequestProcessor> processors;
    
    /**
     * Get the appropriate processor for the given request type
     */
    public RequestProcessor getProcessor(RequestType requestType) {
        log.debug("Finding processor for request type: {}", requestType);
        
        Optional<RequestProcessor> processor = processors.stream()
                .filter(p -> p.canHandle(requestType))
                .max((p1, p2) -> Integer.compare(p1.getPriority(), p2.getPriority()));
        
        if (processor.isPresent()) {
            log.debug("Found processor: {} for request type: {}", processor.get().getName(), requestType);
            return processor.get();
        }
        
        // Fallback to generic processor
        Optional<RequestProcessor> genericProcessor = processors.stream()
                .filter(p -> p.canHandle(RequestType.GENERIC_PROCESSING))
                .findFirst();
        
        if (genericProcessor.isPresent()) {
            log.debug("Using generic processor for request type: {}", requestType);
            return genericProcessor.get();
        }
        
        throw new IllegalArgumentException("No processor found for request type: " + requestType);
    }
}

