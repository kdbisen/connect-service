package com.adyanta.connect.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for connect service responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectResponseDto {
    
    /**
     * Request ID for tracking
     */
    private String requestId;
    
    /**
     * Processing status
     */
    private String status;
    
    /**
     * Response message
     */
    private String message;
    
    /**
     * Timestamp of response
     */
    private LocalDateTime timestamp;
    
    /**
     * Processing ID for tracking
     */
    private String processingId;
    
    /**
     * Additional response data
     */
    private Map<String, Object> data;
    
    /**
     * Error details if processing failed
     */
    private ErrorDetails error;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetails {
        private String code;
        private String message;
        private String details;
    }
}

