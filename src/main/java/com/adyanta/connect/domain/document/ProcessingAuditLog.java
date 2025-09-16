package com.adyanta.connect.domain.document;

import com.adyanta.connect.domain.enums.ProcessingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MongoDB document for auditing processing steps
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "processing_audit_logs")
public class ProcessingAuditLog {
    
    @Id
    private String id;
    
    /**
     * Reference to processing request
     */
    private String processingRequestId;
    
    /**
     * Step name in the processing pipeline
     */
    private String stepName;
    
    /**
     * Status of this step
     */
    private ProcessingStatus status;
    
    /**
     * Step start time
     */
    private LocalDateTime startTime;
    
    /**
     * Step end time
     */
    private LocalDateTime endTime;
    
    /**
     * Duration in milliseconds
     */
    private Long durationMs;
    
    /**
     * Input data for this step
     */
    private String inputData;
    
    /**
     * Output data from this step
     */
    private String outputData;
    
    /**
     * Error message if step failed
     */
    private String errorMessage;
    
    /**
     * Additional metadata
     */
    private Map<String, Object> metadata;
    
    /**
     * Creation timestamp
     */
    @CreatedDate
    private LocalDateTime createdAt;
}
