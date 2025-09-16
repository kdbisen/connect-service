package com.adyanta.connect.domain.document;

import com.adyanta.connect.domain.enums.ProcessingStatus;
import com.adyanta.connect.domain.enums.RequestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MongoDB document representing a processing request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "processing_requests")
public class ProcessingRequest {
    
    @Id
    private String id;
    
    /**
     * External request ID
     */
    private String requestId;
    
    /**
     * Type of request
     */
    private RequestType requestType;
    
    /**
     * Current processing status
     */
    private ProcessingStatus status;
    
    /**
     * Original payload (XML or JSON)
     */
    private String originalPayload;
    
    /**
     * Content type of original payload
     */
    private String contentType;
    
    /**
     * Converted JSON payload
     */
    private String convertedJsonPayload;
    
    /**
     * Response from external API
     */
    private String externalApiResponse;
    
    /**
     * Response from Fenergo API
     */
    private String fenergoResponse;
    
    /**
     * Client ID
     */
    private String clientId;
    
    /**
     * Processing priority
     */
    private Integer priority;
    
    /**
     * Request timeout
     */
    private Long timeoutMs;
    
    /**
     * Additional metadata
     */
    private Map<String, Object> metadata;
    
    /**
     * Error details if processing failed
     */
    private String errorMessage;
    
    /**
     * Stack trace if error occurred
     */
    private String stackTrace;
    
    /**
     * Number of retry attempts
     */
    @Builder.Default
    private Integer retryCount = 0;
    
    /**
     * Maximum retry attempts
     */
    @Builder.Default
    private Integer maxRetries = 3;
    
    /**
     * Processing start time
     */
    private LocalDateTime processingStartTime;
    
    /**
     * Processing end time
     */
    private LocalDateTime processingEndTime;
    
    /**
     * Creation timestamp
     */
    @CreatedDate
    private LocalDateTime createdAt;
    
    /**
     * Last modification timestamp
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    /**
     * Check if request is completed (success or failure)
     */
    public boolean isCompleted() {
        return status == ProcessingStatus.COMPLETED || 
               status == ProcessingStatus.FAILED || 
               status == ProcessingStatus.TIMEOUT;
    }
    
    /**
     * Check if request can be retried
     */
    public boolean canRetry() {
        return retryCount < maxRetries && 
               (status == ProcessingStatus.FAILED || status == ProcessingStatus.TIMEOUT);
    }
}
