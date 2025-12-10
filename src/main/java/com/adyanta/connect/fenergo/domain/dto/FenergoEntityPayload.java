package com.adyanta.connect.fenergo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Fenergo entity payload structure
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FenergoEntityPayload {
    private String tenantId;
    private Object entityType; // Can be String or LookupValue
    private Map<String, Object> dataGroups;
}

