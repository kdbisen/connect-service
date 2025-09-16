package com.adyanta.connect.domain.dto;

import com.adyanta.connect.domain.enums.RequestType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for incoming connect requests
 * Supports both XML and JSON payloads
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectRequestDto {
    
    /**
     * Type of request to determine processing logic
     */
    @NotNull(message = "Request type is required")
    private RequestType requestType;
    
    /**
     * Raw payload data (can be XML or JSON)
     */
    @NotBlank(message = "Payload is required")
    private String payload;
    
    /**
     * Content type of the payload (application/xml or application/json)
     */
    @NotBlank(message = "Content type is required")
    private String contentType;
    
    /**
     * Client ID for authentication
     */
    @NotBlank(message = "Client ID is required")
    private String clientId;
    
    /**
     * Request ID for tracking
     */
    private String requestId;
    
    /**
     * Additional metadata for processing
     */
    private Map<String, Object> metadata;
    
    /**
     * Priority of the request (1-10, 10 being highest)
     */
    @Builder.Default
    private Integer priority = 5;
    
    /**
     * Timeout in milliseconds
     */
    @Builder.Default
    private Long timeoutMs = 300000L; // 5 minutes default
}
