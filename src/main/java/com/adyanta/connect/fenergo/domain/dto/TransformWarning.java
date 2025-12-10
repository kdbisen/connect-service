package com.adyanta.connect.fenergo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a transformation warning
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransformWarning {
    private String field;
    private String message;
}

