package com.adyanta.connect.fenergo.service;

import com.adyanta.connect.fenergo.domain.dto.FenergoEntityPayload;
import com.adyanta.connect.fenergo.domain.dto.LookupValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for building Fenergo entity payloads
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FenergoPayloadBuilder {
    
    public FenergoEntityPayload build(
        Map<String, Object> mappedData,
        String entityType,
        String tenantId
    ) {
        FenergoEntityPayload payload = new FenergoEntityPayload();
        payload.setTenantId(tenantId);
        payload.setEntityType(resolveEntityType(entityType));
        payload.setDataGroups(buildDataGroups(mappedData));
        
        return payload;
    }
    
    private Object resolveEntityType(String entityType) {
        // EntityType is also a lookup - for now return as string
        // TODO: Resolve EntityType lookup
        return entityType;
    }
    
    private Map<String, Object> buildDataGroups(Map<String, Object> mappedData) {
        Map<String, Object> dataGroups = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : mappedData.entrySet()) {
            String key = entry.getKey();
            
            // Skip error keys
            if (key.startsWith("_")) {
                continue;
            }
            
            String[] pathParts = key.split("\\.");
            
            if (pathParts.length > 1 && pathParts[0].equals("DataGroups")) {
                String groupName = pathParts[1];
                String fieldPath = String.join(".", 
                    Arrays.copyOfRange(pathParts, 2, pathParts.length));
                
                Map<String, Object> group = (Map<String, Object>) 
                    dataGroups.computeIfAbsent(groupName, k -> new HashMap<>());
                
                setNestedField(group, fieldPath, entry.getValue());
            }
        }
        
        return dataGroups;
    }
    
    private void setNestedField(Map<String, Object> target, String path, Object value) {
        if (path == null || path.isEmpty()) {
            return;
        }
        
        String[] parts = path.split("\\.");
        Map<String, Object> current = target;
        
        for (int i = 0; i < parts.length - 1; i++) {
            current = (Map<String, Object>) current.computeIfAbsent(
                parts[i], 
                k -> new HashMap<>()
            );
        }
        
        current.put(parts[parts.length - 1], value);
    }
}

