package com.adyanta.connect.repository;

import com.adyanta.connect.domain.document.ProcessingAuditLog;
import com.adyanta.connect.domain.enums.ProcessingStatus;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Reactive MongoDB repository for ProcessingAuditLog documents
 */
@Repository
public interface ProcessingAuditLogRepository extends ReactiveMongoRepository<ProcessingAuditLog, String> {
    
    /**
     * Find audit logs by processing request ID
     */
    Flux<ProcessingAuditLog> findByProcessingRequestId(String processingRequestId);
    
    /**
     * Find audit logs by step name
     */
    Flux<ProcessingAuditLog> findByStepName(String stepName);
    
    /**
     * Find audit logs by status
     */
    Flux<ProcessingAuditLog> findByStatus(ProcessingStatus status);
    
    /**
     * Find audit logs by processing request ID and step name
     */
    Flux<ProcessingAuditLog> findByProcessingRequestIdAndStepName(String processingRequestId, String stepName);
    
    /**
     * Find audit logs created after a specific time
     */
    Flux<ProcessingAuditLog> findByCreatedAtAfter(LocalDateTime dateTime);
    
    /**
     * Find audit logs by processing request ID ordered by creation time
     */
    @Query("{'processingRequestId': ?0}")
    Flux<ProcessingAuditLog> findByProcessingRequestIdOrderByCreatedAtAsc(String processingRequestId);
    
    /**
     * Count audit logs by processing request ID
     */
    Mono<Long> countByProcessingRequestId(String processingRequestId);
    
    /**
     * Find failed audit logs for a processing request
     */
    @Query("{'processingRequestId': ?0, 'status': 'FAILED'}")
    Flux<ProcessingAuditLog> findFailedStepsByProcessingRequestId(String processingRequestId);
}

