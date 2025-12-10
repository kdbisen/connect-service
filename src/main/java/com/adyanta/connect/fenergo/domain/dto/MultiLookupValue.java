package com.adyanta.connect.fenergo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a multi-lookup value with multiple options
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiLookupValue {
    private Integer lookupId;
    private List<LookupValue> values;
}

