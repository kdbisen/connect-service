package com.adyanta.connect.fenergo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response from Fenergo entity creation/update
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FenergoEntityResponse {
    private String entityId;
    private String status;
    private String message;
    private Map<String, Object> dataGroups;
}

