package com.adyanta.connect.repository;

import com.adyanta.connect.domain.document.ProcessingAuditLog;
import com.adyanta.connect.domain.enums.ProcessingStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * MongoDB repository for ProcessingAuditLog documents
 */
@Repository
public interface ProcessingAuditLogRepository extends MongoRepository<ProcessingAuditLog, String> {
    
    /**
     * Find audit logs by processing request ID
     */
    java.util.List<ProcessingAuditLog> findByProcessingRequestId(String processingRequestId);
    
    /**
     * Find audit logs by step name
     */
    java.util.List<ProcessingAuditLog> findByStepName(String stepName);
    
    /**
     * Find audit logs by status
     */
    java.util.List<ProcessingAuditLog> findByStatus(ProcessingStatus status);
    
    /**
     * Find audit logs by processing request ID and step name
     */
    java.util.List<ProcessingAuditLog> findByProcessingRequestIdAndStepName(String processingRequestId, String stepName);
    
    /**
     * Find audit logs created after a specific time
     */
    java.util.List<ProcessingAuditLog> findByCreatedAtAfter(LocalDateTime dateTime);
    
    /**
     * Find audit logs by processing request ID ordered by creation time
     */
    @Query("{'processingRequestId': ?0}")
    java.util.List<ProcessingAuditLog> findByProcessingRequestIdOrderByCreatedAtAsc(String processingRequestId);
    
    /**
     * Count audit logs by processing request ID
     */
    long countByProcessingRequestId(String processingRequestId);
    
    /**
     * Find failed audit logs for a processing request
     */
    @Query("{'processingRequestId': ?0, 'status': 'FAILED'}")
    java.util.List<ProcessingAuditLog> findFailedStepsByProcessingRequestId(String processingRequestId);
}

