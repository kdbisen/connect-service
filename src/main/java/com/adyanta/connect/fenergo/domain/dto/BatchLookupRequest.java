package com.adyanta.connect.fenergo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Batch lookup resolution request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchLookupRequest {
    private List<LookupRequest> lookups;
}

