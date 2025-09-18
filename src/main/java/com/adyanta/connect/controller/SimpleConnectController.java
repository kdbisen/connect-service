package com.adyanta.connect.controller;

import com.adyanta.connect.domain.dto.ConnectRequestDto;
import com.adyanta.connect.domain.dto.ConnectResponseDto;
import com.adyanta.connect.service.SimpleConnectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

/**
 * Simple REST Controller for Connect Service
 * Focused on ADD_KYC implementation
 */
@Slf4j
@RestController
@RequestMapping("/v1/connect")
@RequiredArgsConstructor
@Validated
@Tag(name = "Connect Service", description = "APIs for processing XML/JSON requests")
public class SimpleConnectController {

    private final SimpleConnectService connectService;

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
                    description = "Bad Request - Invalid input",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Validation Error",
                                    summary = "Invalid Request",
                                    value = """
                                            {
                                              "requestId": "req-123456",
                                              "status": "ERROR",
                                              "message": "Validation failed",
                                              "timestamp": "2024-01-01T12:00:00",
                                              "error": {
                                                "message": "Request type is required"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication failed",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Authentication Error",
                                    summary = "Unauthorized",
                                    value = """
                                            {
                                              "requestId": "req-123456",
                                              "status": "ERROR",
                                              "message": "Authentication failed",
                                              "timestamp": "2024-01-01T12:00:00",
                                              "error": {
                                                "message": "Invalid or missing authentication token"
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
    public CompletableFuture<ResponseEntity<ConnectResponseDto>> processRequest(
            @Parameter(description = "Connect request details", required = true)
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Connect request with payload and metadata",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConnectRequestDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "ADD_KYC Request",
                                            summary = "XML KYC Request",
                                            value = """
                                                    {
                                                      "requestType": "ADD_KYC",
                                                      "payload": "<kyc><customer><name>John Doe</name><id>12345</id></customer></kyc>",
                                                      "contentType": "application/xml",
                                                      "clientId": "client-123",
                                                      "requestId": "req-kyc-001",
                                                      "metadata": {
                                                        "customerId": "12345",
                                                        "kycType": "individual"
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

        return connectService.processRequestAsync(request)
                .thenApply(response -> ResponseEntity.accepted().body(response))
                .exceptionally(error -> {
                    log.error("Failed to process request", error);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ConnectResponseDto.builder()
                                    .requestId(request.getRequestId())
                                    .status("ERROR")
                                    .message("Failed to process request")
                                    .timestamp(java.time.LocalDateTime.now())
                                    .error(ConnectResponseDto.ErrorDetails.builder()
                                            .message(error.getMessage())
                                            .build())
                                    .build());
                });
    }

    @Operation(
            summary = "Get Request Status",
            description = "Get the status of a processing request by request ID"
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
                                                        "originalPayload": "<kyc>...</kyc>",
                                                        "convertedJsonPayload": "{\"customer\":...}",
                                                        "fenergoResponse": "{\"status\":\"success\"...}"
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Processing Request",
                                            summary = "Still Processing",
                                            value = """
                                                    {
                                                      "requestId": "req-123456",
                                                      "status": "PROCESSING",
                                                      "message": "Request is being processed",
                                                      "timestamp": "2024-01-01T12:02:00",
                                                      "processingId": "proc-789012"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Failed Request",
                                            summary = "Processing Failed",
                                            value = """
                                                    {
                                                      "requestId": "req-123456",
                                                      "status": "FAILED",
                                                      "message": "Processing failed",
                                                      "timestamp": "2024-01-01T12:03:00",
                                                      "processingId": "proc-789012",
                                                      "error": {
                                                        "message": "Misc service converter API call failed"
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
                                              "requestId": "req-123456",
                                              "status": "ERROR",
                                              "message": "Request not found",
                                              "timestamp": "2024-01-01T12:00:00",
                                              "error": {
                                                "message": "Request with ID req-123456 not found"
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
    public ResponseEntity<ConnectResponseDto> getRequestStatus(
            @Parameter(description = "Unique request identifier", required = true, example = "req-123456")
            @PathVariable String requestId,
            @Parameter(description = "Authentication context", hidden = true)
            @AuthenticationPrincipal Authentication authentication) {

        log.debug("Getting status for request: {}", requestId);

        try {
            ConnectResponseDto response = connectService.getRequestStatus(requestId);
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            log.error("Failed to get request status: {}", requestId, error);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ConnectResponseDto.builder()
                            .requestId(requestId)
                            .status("ERROR")
                            .message("Request not found or error occurred")
                            .timestamp(java.time.LocalDateTime.now())
                            .error(ConnectResponseDto.ErrorDetails.builder()
                                    .message(error.getMessage())
                                    .build())
                            .build());
        }
    }

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
    public ResponseEntity<ConnectResponseDto> health() {
        return ResponseEntity.ok(ConnectResponseDto.builder()
                .status("UP")
                .message("Connect service is running")
                .timestamp(java.time.LocalDateTime.now())
                .build());
    }
}
