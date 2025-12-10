package com.adyanta.connect.fenergo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Batch lookup resolution response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchLookupResponse {
    @Builder.Default
    private List<LookupResolution> resolved = new ArrayList<>();
    @Builder.Default
    private List<LookupRequest> failed = new ArrayList<>();
}

