package com.adyanta.connect.fenergo.service;

import com.adyanta.connect.fenergo.domain.dto.*;
import com.adyanta.connect.fenergo.domain.enums.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrator for the complete transformation pipeline
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransformationOrchestrator {
    
    private final XmlParserService xmlParser;
    private final ConfigLoaderService configLoader;
    private final FieldMappingEngine mappingEngine;
    private final LookupResolverService lookupResolver;
    private final FenergoPayloadBuilder payloadBuilder;
    
    public TransformResult transform(TransformRequest request) {
        TransformResult result = TransformResult.builder()
            .success(false)
            .errors(new ArrayList<>())
            .warnings(new ArrayList<>())
            .build();
        
        try {
            // 1. Parse XML
            Document xmlDoc = xmlParser.parse(request.getXmlContent());
            
            // 2. Load config
            TransformerConfig config = configLoader.loadConfig(
                request.getEntityType(), 
                request.getConfigVersion()
            );
            
            // 3. Extract all lookup values (batch collection)
            Set<LookupRequest> lookupRequests = collectLookupRequests(xmlDoc, config);
            
            // 4. Resolve lookups in batch
            Map<String, LookupResolution> lookupCache = 
                lookupResolver.resolveBatch(new ArrayList<>(lookupRequests));
            
            // 5. Apply mappings
            Map<String, Object> mappedData = mappingEngine.applyMappings(
                xmlDoc, config.getMappings(), lookupCache
            );
            
            // Check for errors
            @SuppressWarnings("unchecked")
            List<String> errors = (List<String>) mappedData.get("_errors");
            if (errors != null && !errors.isEmpty()) {
                for (String error : errors) {
                    result.addError(ErrorType.VALIDATION_ERROR, null, error);
                }
                mappedData.remove("_errors");
            }
            
            // 6. Build payload
            FenergoEntityPayload payload = payloadBuilder.build(
                mappedData, 
                request.getEntityType(),
                request.getTenantId()
            );
            
            // 7. Convert payload to map for response
            Map<String, Object> payloadMap = convertPayloadToMap(payload);
            
            result.setSuccess(result.getErrors().isEmpty());
            result.setData(payloadMap);
            
        } catch (Exception e) {
            log.error("Error in transformation", e);
            result.addError(ErrorType.XML_PARSE_ERROR, null, e.getMessage());
        }
        
        return result;
    }
    
    private Set<LookupRequest> collectLookupRequests(Document xmlDoc, TransformerConfig config) {
        Set<LookupRequest> requests = new HashSet<>();
        
        for (FieldMapping mapping : config.getMappings()) {
            if (mapping.getLookupName() != null) {
                try {
                    Object value = new XPathExtractor().extractValue(
                        xmlDoc, 
                        mapping.getXmlPath(), 
                        mapping.isMulti()
                    );
                    
                    if (value != null) {
                        if (value instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<String> values = (List<String>) value;
                            for (String v : values) {
                                requests.add(LookupRequest.builder()
                                    .lookupName(mapping.getLookupName())
                                    .value(v)
                                    .build());
                            }
                        } else {
                            requests.add(LookupRequest.builder()
                                .lookupName(mapping.getLookupName())
                                .value(value.toString())
                                .build());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error collecting lookup request for: {}", mapping.getFenergoPath(), e);
                }
            }
        }
        
        return requests;
    }
    
    private Map<String, Object> convertPayloadToMap(FenergoEntityPayload payload) {
        Map<String, Object> map = new HashMap<>();
        map.put("TenantId", payload.getTenantId());
        map.put("EntityType", payload.getEntityType());
        map.put("DataGroups", payload.getDataGroups());
        return map;
    }
}

