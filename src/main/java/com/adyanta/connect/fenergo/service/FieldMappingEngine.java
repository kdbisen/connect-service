package com.adyanta.connect.fenergo.service;

import com.adyanta.connect.fenergo.domain.dto.FieldMapping;
import com.adyanta.connect.fenergo.domain.dto.LookupResolution;
import com.adyanta.connect.fenergo.domain.enums.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import java.util.*;

/**
 * Engine for applying field mappings to XML data
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FieldMappingEngine {
    
    private final XPathExtractor xpathExtractor;
    private final DataTransformService transformService;
    private final LookupMapper lookupMapper;
    private final ConditionalEvaluator conditionalEvaluator;
    
    public Map<String, Object> applyMappings(
        Document xmlDoc,
        List<FieldMapping> mappings,
        Map<String, LookupResolution> lookupCache
    ) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        
        for (FieldMapping mapping : mappings) {
            try {
                // 1. Evaluate condition (if present)
                if (mapping.getCondition() != null) {
                    if (!conditionalEvaluator.evaluate(mapping.getCondition(), xmlDoc)) {
                        continue; // Skip this mapping
                    }
                }
                
                // 2. Extract value from XML
                Object value = xpathExtractor.extractValue(
                    xmlDoc, 
                    mapping.getXmlPath(), 
                    mapping.isMulti()
                );
                
                // 3. Handle missing values
                if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                    if (mapping.isRequired()) {
                        errors.add("Required field missing: " + mapping.getFenergoPath());
                        continue;
                    } else if (mapping.getDefaultValue() != null) {
                        value = mapping.getDefaultValue();
                    } else {
                        continue; // Skip optional fields with no default
                    }
                }
                
                // 4. Apply transformation
                if (mapping.getTransform() != null) {
                    value = transformService.applyTransform(value, mapping.getTransform());
                }
                
                // 5. Resolve lookup (if needed)
                if (mapping.getLookupName() != null) {
                    value = lookupMapper.mapToLookupFormat(
                        value,
                        mapping.getLookupName(),
                        mapping.isMulti(),
                        lookupCache
                    );
                }
                
                // 6. Set nested path in result
                setNestedPath(result, mapping.getFenergoPath(), value);
                
            } catch (Exception e) {
                log.error("Error processing mapping: {}", mapping.getFenergoPath(), e);
                errors.add("Error in " + mapping.getFenergoPath() + ": " + e.getMessage());
            }
        }
        
        if (!errors.isEmpty()) {
            result.put("_errors", errors);
        }
        
        return result;
    }
    
    private void setNestedPath(Map<String, Object> result, String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = result;
        
        for (int i = 0; i < parts.length - 1; i++) {
            current = (Map<String, Object>) current.computeIfAbsent(parts[i], k -> new HashMap<>());
        }
        
        current.put(parts[parts.length - 1], value);
    }
}

