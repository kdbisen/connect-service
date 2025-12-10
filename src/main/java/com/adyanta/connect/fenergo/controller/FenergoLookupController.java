package com.adyanta.connect.fenergo.controller;

import com.adyanta.connect.fenergo.domain.dto.*;
import com.adyanta.connect.fenergo.service.LookupResolverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * REST Controller for Fenergo lookup operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/fenergo/lookups")
@RequiredArgsConstructor
@Tag(name = "Fenergo Lookup", description = "Lookup resolution and caching APIs")
public class FenergoLookupController {
    
    private final LookupResolverService lookupResolver;
    
    @GetMapping("/{lookupName}")
    @Operation(summary = "Get all lookup values", 
               description = "Retrieves all values for a specific lookup type")
    public Mono<ResponseEntity<List<FenergoLookup>>> getLookups(
            @PathVariable String lookupName) {
        // TODO: Implement - fetch from Fenergo API
        return Mono.just(ResponseEntity.ok(List.of()));
    }
    
    @GetMapping("/{lookupName}/{value}")
    @Operation(summary = "Resolve lookup value", 
               description = "Resolves a specific lookup value to LookupId")
    public Mono<ResponseEntity<LookupResolution>> resolveLookup(
            @PathVariable String lookupName,
            @PathVariable String value) {
        try {
            LookupResolution resolution = lookupResolver.resolve(lookupName, value);
            return Mono.just(ResponseEntity.ok(resolution));
        } catch (Exception e) {
            log.error("Error resolving lookup: {} = {}", lookupName, value, e);
            return Mono.just(ResponseEntity.notFound().build());
        }
    }
    
    @PostMapping("/resolve-batch")
    @Operation(summary = "Resolve multiple lookups", 
               description = "Resolves multiple lookup values in a single batch operation")
    public Mono<ResponseEntity<BatchLookupResponse>> resolveBatch(
            @RequestBody BatchLookupRequest request) {
        try {
            List<LookupRequest> lookups = request.getLookups();
            var resolved = lookupResolver.resolveBatch(lookups);
            
            BatchLookupResponse response = BatchLookupResponse.builder()
                .resolved(resolved.values().stream().toList())
                .failed(List.of()) // TODO: Track failed lookups
                .build();
            
            return Mono.just(ResponseEntity.ok(response));
        } catch (Exception e) {
            log.error("Error resolving batch lookups", e);
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }
    
    @PostMapping("/refresh/{lookupName}")
    @Operation(summary = "Refresh lookup cache", 
               description = "Refreshes the cache for a specific lookup type")
    public Mono<ResponseEntity<Void>> refreshLookup(
            @PathVariable String lookupName) {
        // TODO: Implement cache refresh
        return Mono.just(ResponseEntity.ok().build());
    }
}

