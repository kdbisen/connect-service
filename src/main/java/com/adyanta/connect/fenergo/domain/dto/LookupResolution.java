package com.adyanta.connect.fenergo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the resolution result of a lookup value
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LookupResolution {
    private Integer lookupId;
    private String lookupName;
    private String originalValue;
    private boolean exactMatch;
}

