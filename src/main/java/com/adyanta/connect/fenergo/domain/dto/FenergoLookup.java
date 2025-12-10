package com.adyanta.connect.fenergo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a Fenergo lookup value
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FenergoLookup {
    private Integer lookupId;
    private String lookupName;
    private String value;
    private String description;
}

