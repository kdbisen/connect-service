package com.adyanta.connect.repository;

import com.adyanta.connect.domain.document.ProcessingRequest;
import com.adyanta.connect.domain.enums.ProcessingStatus;
import com.adyanta.connect.domain.enums.RequestType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * MongoDB repository for ProcessingRequest documents
 */
@Repository
public interface ProcessingRequestRepository extends MongoRepository<ProcessingRequest, String> {
    
    /**
     * Find by request ID
     */
    ProcessingRequest findByRequestId(String requestId);
    
    /**
     * Find by processing status
     */
    java.util.List<ProcessingRequest> findByStatus(ProcessingStatus status);
    
    /**
     * Find by request type and status
     */
    java.util.List<ProcessingRequest> findByRequestTypeAndStatus(RequestType requestType, ProcessingStatus status);
    
    /**
     * Find requests that can be retried
     */
    @Query("{'status': {$in: ['FAILED', 'TIMEOUT']}, 'retryCount': {$lt: '$maxRetries'}}")
    java.util.List<ProcessingRequest> findRetryableRequests();
    
    /**
     * Find requests by client ID
     */
    java.util.List<ProcessingRequest> findByClientId(String clientId);
    
    /**
     * Find requests created after a specific time
     */
    java.util.List<ProcessingRequest> findByCreatedAtAfter(LocalDateTime dateTime);
    
    /**
     * Find requests by status and created time range
     */
    java.util.List<ProcessingRequest> findByStatusAndCreatedAtBetween(
            ProcessingStatus status, 
            LocalDateTime startTime, 
            LocalDateTime endTime
    );
    
    /**
     * Count requests by status
     */
    long countByStatus(ProcessingStatus status);
    
    /**
     * Count requests by request type
     */
    long countByRequestType(RequestType requestType);
}

