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
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

/**
 * REST Controller for Connect Service
 * Provides fire-and-forget processing APIs
 */
@Slf4j
@RestController
@RequestMapping("/v1/connect")
@RequiredArgsConstructor
@Validated
@Tag(name = "Connect Service", description = "APIs for processing XML/JSON requests with external API integration")
public class ConnectController {
    
    private final ConnectService connectService;
    
    /**
     * POST /api/v1/connect/process
     * Process a connect request (fire-and-forget)
     * 
     * @param request The connect request
     * @param authentication The authenticated user
     * @return Immediate response with processing ID
     */
    @Operation(
            summary = "Process Connect Request",
            description = "Process a connect request asynchronously with fire-and-forget pattern. Supports both XML and JSON payloads.",
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
                                    name = "Success Response",
                                    summary = "Request Accepted",
                                    value = """
                                            {
                                              "requestId": "req-123456",
                                              "status": "ACCEPTED",
                                              "message": "Request accepted for processing",
                                              "timestamp": "2024-01-01T12:00:00",
                                              "processingId": "proc-789012"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid input data",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Validation Error",
                                    summary = "Invalid Request",
                                    value = """
                                            {
                                              "requestId": null,
                                              "status": "ERROR",
                                              "message": "Validation failed",
                                              "timestamp": "2024-01-01T12:00:00",
                                              "error": {
                                                "code": "VALIDATION_ERROR",
                                                "message": "Request type is required"
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
                                    name = "Authentication Error",
                                    summary = "Unauthorized",
                                    value = """
                                            {
                                              "requestId": null,
                                              "status": "ERROR",
                                              "message": "Authentication required",
                                              "timestamp": "2024-01-01T12:00:00",
                                              "error": {
                                                "code": "AUTH_ERROR",
                                                "message": "Invalid client credentials"
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
    public Mono<ResponseEntity<ConnectResponseDto>> processRequest(
            @Parameter(description = "Connect request details", required = true)
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Connect request with payload and metadata",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConnectRequestDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Customer Onboarding Request",
                                            summary = "XML Customer Onboarding",
                                            value = """
                                                    {
                                                      "requestType": "CUSTOMER_ONBOARDING",
                                                      "payload": "<customer><name>John Doe</name><email>john@example.com</email><phone>+1234567890</phone></customer>",
                                                      "contentType": "application/xml",
                                                      "clientId": "client-123",
                                                      "requestId": "req-123456",
                                                      "priority": 5,
                                                      "timeoutMs": 300000,
                                                      "metadata": {
                                                        "source": "web-portal",
                                                        "userId": "user-456"
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "KYC Verification Request",
                                            summary = "JSON KYC Verification",
                                            value = """
                                                    {
                                                      "requestType": "KYC_VERIFICATION",
                                                      "payload": "{\\"customerId\\": \\"cust-789\\", \\"documents\\": [{\\"type\\": \\"passport\\", \\"number\\": \\"A1234567\\"}]}",
                                                      "contentType": "application/json",
                                                      "clientId": "client-123",
                                                      "requestId": "req-789012",
                                                      "priority": 8,
                                                      "timeoutMs": 600000,
                                                      "metadata": {
                                                        "source": "mobile-app",
                                                        "userId": "user-789"
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Document Processing Request",
                                            summary = "XML Document Processing",
                                            value = """
                                                    {
                                                      "requestType": "DOCUMENT_PROCESSING",
                                                      "payload": "<document><type>invoice</type><content>base64-encoded-content</content><metadata><amount>1000.00</amount><currency>USD</currency></metadata></document>",
                                                      "contentType": "application/xml",
                                                      "clientId": "client-456",
                                                      "requestId": "req-345678",
                                                      "priority": 3,
                                                      "timeoutMs": 180000,
                                                      "metadata": {
                                                        "source": "api-gateway",
                                                        "batchId": "batch-001"
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
        
        log.info("Received connect request from client: {}", request.getClientId());
        
        return connectService.processRequest(request)
                .map(response -> ResponseEntity.accepted().body(response))
                .doOnSuccess(response -> log.info("Request processing initiated: {}", response.getBody().getRequestId()))
                .doOnError(error -> log.error("Failed to process request", error));
    }
    
    /**
     * GET /api/v1/connect/status/{requestId}
     * Get the status of a processing request
     * 
     * @param requestId The request ID
     * @param authentication The authenticated user
     * @return Request status and processing details
     */
    @Operation(
            summary = "Get Request Status",
            description = "Retrieve the current status and processing details of a previously submitted request.",
            security = @SecurityRequirement(name = "apigee-auth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Request status retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConnectResponseDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Completed Request",
                                            summary = "Processing Completed",
                                            value = """
                                                    {
                                                      "requestId": "req-123456",
                                                      "status": "COMPLETED",
                                                      "message": "Processing completed successfully",
                                                      "timestamp": "2024-01-01T12:05:00",
                                                      "processingId": "proc-789012",
                                                      "data": {
                                                        "originalPayload": "<customer><name>John Doe</name></customer>",
                                                        "convertedJsonPayload": "{\\"customer\\": {\\"name\\": \\"John Doe\\"}}",
                                                        "externalApiResponse": "{\\"result\\": \\"success\\", \\"id\\": \\"ext-123\\"}",
                                                        "fenergoResponse": "{\\"status\\": \\"processed\\", \\"reference\\": \\"fen-456\\"}",
                                                        "retryCount": 0,
                                                        "createdAt": "2024-01-01T12:00:00",
                                                        "updatedAt": "2024-01-01T12:05:00"
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Processing Request",
                                            summary = "Still Processing",
                                            value = """
                                                    {
                                                      "requestId": "req-789012",
                                                      "status": "PROCESSING",
                                                      "message": "Request is being processed",
                                                      "timestamp": "2024-01-01T12:02:00",
                                                      "processingId": "proc-345678",
                                                      "data": {
                                                        "originalPayload": "<document><type>invoice</type></document>",
                                                        "convertedJsonPayload": "{\\"document\\": {\\"type\\": \\"invoice\\"}}",
                                                        "externalApiResponse": null,
                                                        "fenergoResponse": null,
                                                        "retryCount": 0,
                                                        "createdAt": "2024-01-01T12:00:00",
                                                        "updatedAt": "2024-01-01T12:02:00"
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Failed Request",
                                            summary = "Processing Failed",
                                            value = """
                                                    {
                                                      "requestId": "req-345678",
                                                      "status": "FAILED",
                                                      "message": "Processing failed",
                                                      "timestamp": "2024-01-01T12:03:00",
                                                      "processingId": "proc-901234",
                                                      "data": {
                                                        "originalPayload": "<invalid>data</invalid>",
                                                        "convertedJsonPayload": null,
                                                        "externalApiResponse": null,
                                                        "fenergoResponse": null,
                                                        "retryCount": 2,
                                                        "createdAt": "2024-01-01T12:00:00",
                                                        "updatedAt": "2024-01-01T12:03:00"
                                                      },
                                                      "error": {
                                                        "code": "PROCESSING_ERROR",
                                                        "message": "Invalid XML format",
                                                        "details": "XML parsing failed at line 1"
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
                                    name = "Not Found",
                                    summary = "Request Not Found",
                                    value = """
                                            {
                                              "requestId": "req-nonexistent",
                                              "status": "ERROR",
                                              "message": "Request not found",
                                              "timestamp": "2024-01-01T12:00:00",
                                              "error": {
                                                "code": "NOT_FOUND",
                                                "message": "Request with ID 'req-nonexistent' not found"
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
    public Mono<ResponseEntity<ConnectResponseDto>> getRequestStatus(
            @Parameter(description = "Unique request identifier", required = true, example = "req-123456")
            @PathVariable String requestId,
            @Parameter(description = "Authentication context", hidden = true)
            @AuthenticationPrincipal Authentication authentication) {
        
        log.debug("Getting status for request: {}", requestId);
        
        return connectService.getRequestStatus(requestId)
                .map(response -> ResponseEntity.ok(response))
                .onErrorResume(error -> {
                    log.error("Failed to get request status: {}", requestId, error);
                    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ConnectResponseDto.builder()
                                    .requestId(requestId)
                                    .status("ERROR")
                                    .message("Request not found or error occurred")
                                    .timestamp(java.time.LocalDateTime.now())
                                    .error(ConnectResponseDto.ErrorDetails.builder()
                                            .message(error.getMessage())
                                            .build())
                                    .build()));
                });
    }
    
    /**
     * GET /api/v1/connect/health
     * Health check endpoint
     * 
     * @return Health status
     */
    @Operation(
            summary = "Health Check",
            description = "Check the health status of the Connect Service"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Service is healthy",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Healthy Service",
                                    summary = "Service Running",
                                    value = """
                                            {
                                              "status": "UP",
                                              "message": "Connect service is running",
                                              "timestamp": "2024-01-01T12:00:00"
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/health")
    public Mono<ResponseEntity<ConnectResponseDto>> health() {
        return Mono.just(ResponseEntity.ok(ConnectResponseDto.builder()
                .status("UP")
                .message("Connect service is running")
                .timestamp(java.time.LocalDateTime.now())
                .build()));
    }
}
