package com.adyanta.connect.fenergo.controller;

import com.adyanta.connect.fenergo.domain.dto.TransformRequest;
import com.adyanta.connect.fenergo.domain.dto.TransformResult;
import com.adyanta.connect.fenergo.domain.dto.TransformerConfig;
import com.adyanta.connect.fenergo.service.ConfigLoaderService;
import com.adyanta.connect.fenergo.service.TransformationOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST Controller for Fenergo transformation operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/fenergo/transform")
@RequiredArgsConstructor
@Tag(name = "Fenergo Transformer", description = "XML to Fenergo JSON transformation APIs")
public class FenergoTransformerController {
    
    private final TransformationOrchestrator transformationOrchestrator;
    private final ConfigLoaderService configLoader;
    
    @PostMapping("/xml-to-fenergo")
    @Operation(summary = "Transform XML to Fenergo JSON", 
               description = "Transforms XML input into Fenergo-compliant JSON using config-driven mappings")
    public Mono<ResponseEntity<TransformResult>> transformXmlToFenergo(
            @RequestBody TransformRequest request) {
        log.info("Received transformation request for entityType: {}", request.getEntityType());
        
        try {
            TransformResult result = transformationOrchestrator.transform(request);
            return Mono.just(ResponseEntity.ok(result));
        } catch (Exception e) {
            log.error("Error transforming XML to Fenergo", e);
            TransformResult errorResult = TransformResult.builder()
                .success(false)
                .build();
            errorResult.addError(
                com.adyanta.connect.fenergo.domain.enums.ErrorType.XML_PARSE_ERROR,
                null,
                e.getMessage()
            );
            return Mono.just(ResponseEntity.badRequest().body(errorResult));
        }
    }
    
    @PostMapping("/validate")
    @Operation(summary = "Validate transformation", 
               description = "Validates XML and transformation config without performing transformation")
    public Mono<ResponseEntity<TransformResult>> validate(
            @RequestBody TransformRequest request) {
        // TODO: Implement validation logic
        return Mono.just(ResponseEntity.ok(TransformResult.builder().success(true).build()));
    }
    
    @GetMapping("/configs/{entityType}")
    @Operation(summary = "Get transformation config", 
               description = "Retrieves transformation configuration for a specific entity type")
    public Mono<ResponseEntity<TransformerConfig>> getConfig(
            @PathVariable String entityType,
            @RequestParam(required = false) String version) {
        try {
            TransformerConfig config = configLoader.loadConfig(entityType, version);
            return Mono.just(ResponseEntity.ok(config));
        } catch (Exception e) {
            log.error("Error loading config for entityType: {}", entityType, e);
            return Mono.just(ResponseEntity.notFound().build());
        }
    }
}

