package com.adyanta.connect.controller;

import com.adyanta.connect.domain.dto.ConnectRequestDto;
import com.adyanta.connect.domain.dto.ConnectResponseDto;
import com.adyanta.connect.service.ConnectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.CompletableFuture;

import jakarta.validation.Valid;

/**
 * REST Controller for Connect Service API v2
 * Provides enhanced fire-and-forget processing APIs with additional features
 * 
 * API Version: 2.0
 * Base Path: /v2/connect
 * 
 * New Features in v2:
 * - Enhanced request validation
 * - Improved error handling
 * - Additional metadata support
 * - Batch processing capabilities
 * - Advanced monitoring
 */
@Slf4j
@RestController
@RequestMapping("/v2/connect")
@RequiredArgsConstructor
@Validated
@Tag(name = "Connect Service v2", description = "Enhanced APIs for processing XML/JSON requests with advanced features")
public class ConnectControllerV2 {
    
    private final ConnectService connectService;
    
    /**
     * POST /api/v2/connect/process
     * Process a connect request (fire-and-forget) - Enhanced version
     * 
     * @param request The connect request
     * @param authentication The authenticated user
     * @return Immediate response with processing ID
     */
    @Operation(
            summary = "Process Connect Request v2",
            description = "Enhanced processing of connect requests with improved validation, error handling, and monitoring capabilities.",
            security = @SecurityRequirement(name = "apigee-auth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "202",
                    description = "Request accepted for processing",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConnectResponseDto.class),
                            examples = @ExampleObject(
                                    name = "Success Response v2",
                                    summary = "Request Accepted with Enhanced Metadata",
                                    value = """
                                            {
                                              "requestId": "req-123456-v2",
                                              "status": "ACCEPTED",
                                              "message": "Request accepted for processing with enhanced validation",
                                              "timestamp": "2024-01-01T12:00:00",
                                              "processingId": "proc-789012-v2",
                                              "version": "2.0",
                                              "features": {
                                                "enhancedValidation": true,
                                                "batchProcessing": false,
                                                "advancedMonitoring": true
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Enhanced validation failed",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Enhanced Validation Error",
                                    summary = "Validation Failed with Details",
                                    value = """
                                            {
                                              "requestId": null,
                                              "status": "ERROR",
                                              "message": "Enhanced validation failed",
                                              "timestamp": "2024-01-01T12:00:00",
                                              "version": "2.0",
                                              "error": {
                                                "code": "VALIDATION_ERROR_V2",
                                                "message": "Request validation failed with enhanced rules",
                                                "details": [
                                                  "Request type is required",
                                                  "Payload must be valid XML or JSON",
                                                  "Client ID must be provided"
                                                ]
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid credentials",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Authentication Error v2",
                                    summary = "Unauthorized with Enhanced Security",
                                    value = """
                                            {
                                              "requestId": null,
                                              "status": "ERROR",
                                              "message": "Authentication required with enhanced security",
                                              "timestamp": "2024-01-01T12:00:00",
                                              "version": "2.0",
                                              "error": {
                                                "code": "AUTH_ERROR_V2",
                                                "message": "Invalid client credentials or insufficient permissions"
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping(
            value = "/process",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public CompletableFuture<ResponseEntity<ConnectResponseDto>> processRequestV2(
            @Parameter(description = "Enhanced connect request details", required = true)
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Enhanced connect request with additional validation and metadata",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConnectRequestDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Enhanced Customer Onboarding Request",
                                            summary = "XML Customer Onboarding v2",
                                            value = """
                                                    {
                                                      "requestType": "CUSTOMER_ONBOARDING",
                                                      "payload": "<customer><name>John Doe</name><email>john@example.com</email><phone>+1234567890</phone></customer>",
                                                      "contentType": "application/xml",
                                                      "clientId": "client-123",
                                                      "requestId": "req-123456-v2",
                                                      "priority": 5,
                                                      "timeoutMs": 300000,
                                                      "metadata": {
                                                        "source": "web-portal",
                                                        "userId": "user-456",
                                                        "version": "2.0",
                                                        "features": {
                                                          "enhancedValidation": true,
                                                          "batchProcessing": false
                                                        }
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Batch Processing Request",
                                            summary = "JSON Batch Processing v2",
                                            value = """
                                                    {
                                                      "requestType": "BATCH_PROCESSING",
                                                      "payload": "{\\"batchId\\": \\"batch-001\\", \\"items\\": [{\\"id\\": \\"item-1\\"}, {\\"id\\": \\"item-2\\"}]}",
                                                      "contentType": "application/json",
                                                      "clientId": "client-123",
                                                      "requestId": "req-batch-001",
                                                      "priority": 8,
                                                      "timeoutMs": 600000,
                                                      "metadata": {
                                                        "source": "batch-processor",
                                                        "userId": "system",
                                                        "version": "2.0",
                                                        "features": {
                                                          "enhancedValidation": true,
                                                          "batchProcessing": true,
                                                          "batchSize": 100
                                                        }
                                                      }
                                                    }
                                                    """
                                    )
                            }
                    )
            )
            @Valid @RequestBody ConnectRequestDto request,
            @Parameter(description = "Authentication context", hidden = true)
            @AuthenticationPrincipal Authentication authentication) {
        
        log.info("Received enhanced connect request v2 from client: {}", request.getClientId());
        
        // Add version information to the request
        if (request.getMetadata() != null) {
            request.getMetadata().put("apiVersion", "2.0");
        }
        
        return connectService.processRequestAsync(request)
                .thenApply(response -> {
                    // Enhance response with v2 features
                    response.setVersion("2.0");
                    return ResponseEntity.accepted().body(response);
                })
                .exceptionally(error -> {
                    log.error("Failed to process enhanced request v2", error);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ConnectResponseDto.builder()
                                    .requestId(request.getRequestId())
                                    .status("ERROR")
                                    .message("Failed to process enhanced request")
                                    .timestamp(java.time.LocalDateTime.now())
                                    .version("2.0")
                                    .error(ConnectResponseDto.ErrorDetails.builder()
                                            .message(error.getMessage())
                                            .build())
                                    .build());
                });
    }
    
    /**
     * GET /api/v2/connect/status/{requestId}
     * Get the status of a processing request - Enhanced version
     * 
     * @param requestId The request ID
     * @param authentication The authenticated user
     * @return Request status and processing details with enhanced information
     */
    @Operation(
            summary = "Get Request Status v2",
            description = "Retrieve enhanced status and processing details of a previously submitted request with additional monitoring information.",
            security = @SecurityRequirement(name = "apigee-auth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Request status retrieved successfully with enhanced details",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConnectResponseDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Enhanced Completed Request",
                                            summary = "Processing Completed v2",
                                            value = """
                                                    {
                                                      "requestId": "req-123456-v2",
                                                      "status": "COMPLETED",
                                                      "message": "Processing completed successfully with enhanced monitoring",
                                                      "timestamp": "2024-01-01T12:05:00",
                                                      "processingId": "proc-789012-v2",
                                                      "version": "2.0",
                                                      "data": {
                                                        "originalPayload": "<customer><name>John Doe</name></customer>",
                                                        "convertedJsonPayload": "{\\"customer\\": {\\"name\\": \\"John Doe\\"}}",
                                                        "externalApiResponse": "{\\"result\\": \\"success\\", \\"id\\": \\"ext-123\\"}",
                                                        "fenergoResponse": "{\\"status\\": \\"processed\\", \\"reference\\": \\"fen-456\\"}",
                                                        "retryCount": 0,
                                                        "createdAt": "2024-01-01T12:00:00",
                                                        "updatedAt": "2024-01-01T12:05:00",
                                                        "enhancedMetrics": {
                                                          "processingTimeMs": 300000,
                                                          "memoryUsageMB": 128,
                                                          "cpuUsagePercent": 15.5,
                                                          "externalApiCalls": 2,
                                                          "validationPassed": true
                                                        }
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Enhanced Processing Request",
                                            summary = "Still Processing v2",
                                            value = """
                                                    {
                                                      "requestId": "req-789012-v2",
                                                      "status": "PROCESSING",
                                                      "message": "Request is being processed with enhanced monitoring",
                                                      "timestamp": "2024-01-01T12:02:00",
                                                      "processingId": "proc-345678-v2",
                                                      "version": "2.0",
                                                      "data": {
                                                        "originalPayload": "<document><type>invoice</type></document>",
                                                        "convertedJsonPayload": "{\\"document\\": {\\"type\\": \\"invoice\\"}}",
                                                        "externalApiResponse": null,
                                                        "fenergoResponse": null,
                                                        "retryCount": 0,
                                                        "createdAt": "2024-01-01T12:00:00",
                                                        "updatedAt": "2024-01-01T12:02:00",
                                                        "enhancedMetrics": {
                                                          "processingTimeMs": 120000,
                                                          "memoryUsageMB": 96,
                                                          "cpuUsagePercent": 12.3,
                                                          "externalApiCalls": 1,
                                                          "validationPassed": true
                                                        }
                                                      }
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Request not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found v2",
                                    summary = "Request Not Found with Enhanced Error Info",
                                    value = """
                                            {
                                              "requestId": "req-nonexistent",
                                              "status": "ERROR",
                                              "message": "Request not found with enhanced search",
                                              "timestamp": "2024-01-01T12:00:00",
                                              "version": "2.0",
                                              "error": {
                                                "code": "NOT_FOUND_V2",
                                                "message": "Request with ID 'req-nonexistent' not found in enhanced search",
                                                "suggestions": [
                                                  "Check if request ID is correct",
                                                  "Verify request was submitted to v2 API",
                                                  "Check if request has expired"
                                                ]
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping(
            value = "/status/{requestId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ConnectResponseDto> getRequestStatusV2(
            @Parameter(description = "Unique request identifier", required = true, example = "req-123456-v2")
            @PathVariable String requestId,
            @Parameter(description = "Authentication context", hidden = true)
            @AuthenticationPrincipal Authentication authentication) {
        
        log.debug("Getting enhanced status for request v2: {}", requestId);
        
        try {
            ConnectResponseDto response = connectService.getRequestStatus(requestId);
            // Enhance response with v2 features
            response.setVersion("2.0");
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            log.error("Failed to get enhanced request status v2: {}", requestId, error);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ConnectResponseDto.builder()
                            .requestId(requestId)
                            .status("ERROR")
                            .message("Request not found or error occurred with enhanced search")
                            .timestamp(java.time.LocalDateTime.now())
                            .version("2.0")
                            .error(ConnectResponseDto.ErrorDetails.builder()
                                    .message(error.getMessage())
                                    .build())
                            .build());
        }
    }
    
    /**
     * GET /api/v2/connect/health
     * Enhanced health check endpoint
     * 
     * @return Enhanced health status with detailed metrics
     */
    @Operation(
            summary = "Enhanced Health Check v2",
            description = "Check the enhanced health status of the Connect Service with detailed metrics and monitoring information"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Service is healthy with enhanced metrics",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Enhanced Healthy Service",
                                    summary = "Service Running with Enhanced Metrics",
                                    value = """
                                            {
                                              "status": "UP",
                                              "message": "Connect service v2 is running with enhanced monitoring",
                                              "timestamp": "2024-01-01T12:00:00",
                                              "version": "2.0",
                                              "enhancedMetrics": {
                                                "uptime": "72h 15m 30s",
                                                "memoryUsage": "512MB",
                                                "cpuUsage": "25.5%",
                                                "activeConnections": 15,
                                                "requestsPerMinute": 120,
                                                "averageResponseTime": "250ms",
                                                "errorRate": "0.5%"
                                              },
                                              "features": {
                                                "enhancedValidation": true,
                                                "batchProcessing": true,
                                                "advancedMonitoring": true,
                                                "realTimeMetrics": true
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/health")
    public ResponseEntity<ConnectResponseDto> healthV2() {
        return ResponseEntity.ok(ConnectResponseDto.builder()
                .status("UP")
                .message("Connect service v2 is running with enhanced monitoring")
                .timestamp(java.time.LocalDateTime.now())
                .version("2.0")
                .build());
    }
    
    /**
     * GET /api/v2/connect/version
     * Get API version information
     * 
     * @return API version details
     */
    @Operation(
            summary = "Get API Version",
            description = "Retrieve detailed information about the current API version and available features"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "API version information retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "API Version Info",
                                    summary = "Version Information",
                                    value = """
                                            {
                                              "version": "2.0",
                                              "apiName": "Connect Service API",
                                              "description": "Enhanced API for processing XML/JSON requests",
                                              "features": {
                                                "enhancedValidation": true,
                                                "batchProcessing": true,
                                                "advancedMonitoring": true,
                                                "realTimeMetrics": true,
                                                "improvedErrorHandling": true
                                              },
                                              "deprecationInfo": {
                                                "v1Deprecated": false,
                                                "v1SupportedUntil": "2025-12-31",
                                                "migrationGuide": "https://docs.adyanta.com/connect-service/migration/v1-to-v2"
                                              },
                                              "changelog": [
                                                "Enhanced request validation",
                                                "Added batch processing capabilities",
                                                "Improved error handling and reporting",
                                                "Added real-time metrics",
                                                "Enhanced monitoring and observability"
                                              ]
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/version")
    public ResponseEntity<Object> getVersion() {
        return ResponseEntity.ok(new Object() {
            public final String version = "2.0";
            public final String apiName = "Connect Service API";
            public final String description = "Enhanced API for processing XML/JSON requests";
            public final Object features = new Object() {
                public final boolean enhancedValidation = true;
                public final boolean batchProcessing = true;
                public final boolean advancedMonitoring = true;
                public final boolean realTimeMetrics = true;
                public final boolean improvedErrorHandling = true;
            };
            public final Object deprecationInfo = new Object() {
                public final boolean v1Deprecated = false;
                public final String v1SupportedUntil = "2025-12-31";
                public final String migrationGuide = "https://docs.adyanta.com/connect-service/migration/v1-to-v2";
            };
        });
    }
}
