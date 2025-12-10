package com.adyanta.connect.fenergo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single lookup value with ID and name
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LookupValue {
    private Integer lookupId;
    private String lookupName;
}

