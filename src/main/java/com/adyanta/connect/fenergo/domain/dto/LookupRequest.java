package com.adyanta.connect.fenergo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for lookup resolution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LookupRequest {
    private String lookupName;
    private String value;
}

