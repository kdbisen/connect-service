package com.adyanta.connect.fenergo.domain.dto;

import com.adyanta.connect.fenergo.domain.enums.FenergoFieldType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Represents a field mapping configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldMapping {
    private String fenergoPath;
    private FenergoFieldType fenergoType;
    private String xmlPath;
    private String lookupName;
    private boolean multi;
    private String transform;
    private boolean required;
    private String defaultValue;
    private Map<String, Object> condition;
    private Map<String, Object> validation;
    private Map<String, Object> errorHandling;
}

