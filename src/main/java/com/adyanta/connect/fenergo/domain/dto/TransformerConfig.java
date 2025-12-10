package com.adyanta.connect.fenergo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a transformer configuration for entity type mapping
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransformerConfig {
    private String entityType;
    private String version;
    private List<FieldMapping> mappings;
}

