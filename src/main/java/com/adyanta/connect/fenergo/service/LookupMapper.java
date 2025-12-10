package com.adyanta.connect.fenergo.service;

import com.adyanta.connect.fenergo.domain.dto.LookupResolution;
import com.adyanta.connect.fenergo.domain.dto.LookupValue;
import com.adyanta.connect.fenergo.domain.dto.MultiLookupValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for mapping values to Fenergo lookup format
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LookupMapper {
    
    private final LookupResolverService lookupResolver;
    
    public Object mapToLookupFormat(
        Object value,
        String lookupName,
        boolean isMulti,
        Map<String, LookupResolution> lookupCache
    ) {
        if (isMulti) {
            return mapMultiLookup(value, lookupName, lookupCache);
        } else {
            return mapSingleLookup(value, lookupName, lookupCache);
        }
    }
    
    private LookupValue mapSingleLookup(
        Object value,
        String lookupName,
        Map<String, LookupResolution> cache
    ) {
        String cacheKey = lookupName + ":" + value.toString();
        LookupResolution resolution = cache.get(cacheKey);
        
        if (resolution == null) {
            resolution = lookupResolver.resolve(lookupName, value.toString());
            cache.put(cacheKey, resolution);
        }
        
        return LookupValue.builder()
            .lookupId(resolution.getLookupId())
            .lookupName(resolution.getLookupName())
            .build();
    }
    
    private MultiLookupValue mapMultiLookup(
        Object value,
        String lookupName,
        Map<String, LookupResolution> cache
    ) {
        List<String> values = value instanceof List 
            ? (List<String>) value 
            : Collections.singletonList(value.toString());
        
        List<LookupValue> lookupValues = new ArrayList<>();
        Integer lookupId = null;
        
        for (String val : values) {
            LookupValue lv = mapSingleLookup(val, lookupName, cache);
            if (lookupId == null) {
                lookupId = lv.getLookupId();
            }
            lookupValues.add(lv);
        }
        
        if (lookupId == null) {
            throw new RuntimeException("No lookup values resolved");
        }
        
        return MultiLookupValue.builder()
            .lookupId(lookupId)
            .values(lookupValues)
            .build();
    }
}

