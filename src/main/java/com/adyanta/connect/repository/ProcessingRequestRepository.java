package com.adyanta.connect.repository;

import com.adyanta.connect.domain.document.ProcessingRequest;
import com.adyanta.connect.domain.enums.ProcessingStatus;
import com.adyanta.connect.domain.enums.RequestType;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Reactive MongoDB repository for ProcessingRequest documents
 */
@Repository
public interface ProcessingRequestRepository extends ReactiveMongoRepository<ProcessingRequest, String> {
    
    /**
     * Find by request ID
     */
    Mono<ProcessingRequest> findByRequestId(String requestId);
    
    /**
     * Find by processing status
     */
    Flux<ProcessingRequest> findByStatus(ProcessingStatus status);
    
    /**
     * Find by request type and status
     */
    Flux<ProcessingRequest> findByRequestTypeAndStatus(RequestType requestType, ProcessingStatus status);
    
    /**
     * Find requests that can be retried
     */
    @Query("{'status': {$in: ['FAILED', 'TIMEOUT']}, 'retryCount': {$lt: '$maxRetries'}}")
    Flux<ProcessingRequest> findRetryableRequests();
    
    /**
     * Find requests by client ID
     */
    Flux<ProcessingRequest> findByClientId(String clientId);
    
    /**
     * Find requests created after a specific time
     */
    Flux<ProcessingRequest> findByCreatedAtAfter(LocalDateTime dateTime);
    
    /**
     * Find requests by status and created time range
     */
    Flux<ProcessingRequest> findByStatusAndCreatedAtBetween(
            ProcessingStatus status, 
            LocalDateTime startTime, 
            LocalDateTime endTime
    );
    
    /**
     * Count requests by status
     */
    Mono<Long> countByStatus(ProcessingStatus status);
    
    /**
     * Count requests by request type
     */
    Mono<Long> countByRequestType(RequestType requestType);
}

