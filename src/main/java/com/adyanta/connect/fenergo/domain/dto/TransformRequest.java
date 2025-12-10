package com.adyanta.connect.fenergo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for transformation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransformRequest {
    private String xmlContent;
    private String entityType;
    private String configVersion;
    private String tenantId;
}

