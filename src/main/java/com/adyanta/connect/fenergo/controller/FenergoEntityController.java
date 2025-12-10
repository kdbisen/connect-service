package com.adyanta.connect.fenergo.controller;

import com.adyanta.connect.fenergo.domain.dto.FenergoEntityPayload;
import com.adyanta.connect.fenergo.domain.dto.FenergoEntityResponse;
import com.adyanta.connect.fenergo.service.FenergoApiClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST Controller for Fenergo entity operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/fenergo/entities")
@RequiredArgsConstructor
@Tag(name = "Fenergo Entity", description = "Entity lifecycle management APIs")
public class FenergoEntityController {
    
    private final FenergoApiClient fenergoApiClient;
    
    @PostMapping
    @Operation(summary = "Create entity", 
               description = "Creates a new entity in Fenergo")
    public Mono<ResponseEntity<FenergoEntityResponse>> createEntity(
            @RequestBody FenergoEntityPayload payload) {
        try {
            FenergoEntityResponse response = fenergoApiClient.createEntity(payload);
            return Mono.just(ResponseEntity.ok(response));
        } catch (Exception e) {
            log.error("Error creating entity", e);
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }
    
    @PutMapping("/{entityId}")
    @Operation(summary = "Update entity", 
               description = "Updates an existing entity in Fenergo")
    public Mono<ResponseEntity<FenergoEntityResponse>> updateEntity(
            @PathVariable String entityId,
            @RequestBody FenergoEntityPayload payload) {
        try {
            FenergoEntityResponse response = fenergoApiClient.updateEntity(entityId, payload);
            return Mono.just(ResponseEntity.ok(response));
        } catch (Exception e) {
            log.error("Error updating entity: {}", entityId, e);
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }
    
    @GetMapping("/{entityId}")
    @Operation(summary = "Get entity", 
               description = "Retrieves an entity from Fenergo")
    public Mono<ResponseEntity<FenergoEntityResponse>> getEntity(
            @PathVariable String entityId) {
        try {
            FenergoEntityResponse response = fenergoApiClient.getEntity(entityId);
            return Mono.just(ResponseEntity.ok(response));
        } catch (Exception e) {
            log.error("Error getting entity: {}", entityId, e);
            return Mono.just(ResponseEntity.notFound().build());
        }
    }
}

