package com.adyanta.connect.fenergo.domain.dto;

import com.adyanta.connect.fenergo.domain.enums.ErrorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a transformation error
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransformError {
    private ErrorType type;
    private String field;
    private String message;
}

