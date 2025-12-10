package com.adyanta.connect.fenergo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Result of transformation operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransformResult {
    private boolean success;
    private Map<String, Object> data;
    @Builder.Default
    private List<TransformError> errors = new ArrayList<>();
    @Builder.Default
    private List<TransformWarning> warnings = new ArrayList<>();
    
    public void addError(ErrorType type, String field, String message) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(TransformError.builder()
            .type(type)
            .field(field)
            .message(message)
            .build());
    }
    
    public void addWarning(String field, String message) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(TransformWarning.builder()
            .field(field)
            .message(message)
            .build());
    }
}

