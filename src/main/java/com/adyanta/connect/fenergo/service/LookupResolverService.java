package com.adyanta.connect.fenergo.service;

import com.adyanta.connect.fenergo.domain.dto.FenergoLookup;
import com.adyanta.connect.fenergo.domain.dto.LookupRequest;
import com.adyanta.connect.fenergo.domain.dto.LookupResolution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for resolving lookup values to LookupId
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LookupResolverService {
    
    private final FenergoLookupClient fenergoLookupClient;
    private final LookupCacheService lookupCacheService;
    
    public LookupResolution resolve(String lookupName, String value) {
        // 1. Check cache first
        String cacheKey = lookupName + ":" + value;
        LookupResolution cached = lookupCacheService.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // 2. Fetch from Fenergo API
        List<FenergoLookup> lookups = fenergoLookupClient.fetchLookups(lookupName);
        
        // 3. Find match (exact, case-insensitive, fuzzy)
        LookupResolution resolution = findMatch(lookups, value, lookupName);
        
        // 4. Cache result
        lookupCacheService.put(cacheKey, resolution, Duration.ofHours(24));
        
        return resolution;
    }
    
    public Map<String, LookupResolution> resolveBatch(List<LookupRequest> requests) {
        Map<String, LookupResolution> results = new HashMap<>();
        
        // Group by lookup name for batch fetching
        Map<String, List<LookupRequest>> grouped = requests.stream()
            .collect(Collectors.groupingBy(LookupRequest::getLookupName));
        
        for (Map.Entry<String, List<LookupRequest>> entry : grouped.entrySet()) {
            String lookupName = entry.getKey();
            List<FenergoLookup> lookups = fenergoLookupClient.fetchLookups(lookupName);
            
            for (LookupRequest request : entry.getValue()) {
                String cacheKey = lookupName + ":" + request.getValue();
                LookupResolution cached = lookupCacheService.get(cacheKey);
                
                if (cached == null) {
                    cached = findMatch(lookups, request.getValue(), lookupName);
                    lookupCacheService.put(cacheKey, cached, Duration.ofHours(24));
                }
                
                results.put(cacheKey, cached);
            }
        }
        
        return results;
    }
    
    private LookupResolution findMatch(List<FenergoLookup> lookups, String value, String lookupName) {
        // Exact match
        Optional<FenergoLookup> exact = lookups.stream()
            .filter(l -> l.getLookupName() != null && l.getLookupName().equals(value))
            .findFirst();
        if (exact.isPresent()) {
            return LookupResolution.builder()
                .lookupId(exact.get().getLookupId())
                .lookupName(value)
                .originalValue(value)
                .exactMatch(true)
                .build();
        }
        
        // Case-insensitive match
        Optional<FenergoLookup> caseInsensitive = lookups.stream()
            .filter(l -> l.getLookupName() != null && l.getLookupName().equalsIgnoreCase(value))
            .findFirst();
        if (caseInsensitive.isPresent()) {
            return LookupResolution.builder()
                .lookupId(caseInsensitive.get().getLookupId())
                .lookupName(caseInsensitive.get().getLookupName())
                .originalValue(value)
                .exactMatch(false)
                .build();
        }
        
        // Fuzzy match (Levenshtein distance)
        Optional<FenergoLookup> fuzzy = lookups.stream()
            .filter(l -> l.getLookupName() != null && 
                    levenshteinDistance(l.getLookupName(), value) <= 2)
            .findFirst();
        if (fuzzy.isPresent()) {
            return LookupResolution.builder()
                .lookupId(fuzzy.get().getLookupId())
                .lookupName(fuzzy.get().getLookupName())
                .originalValue(value)
                .exactMatch(false)
                .build();
        }
        
        throw new RuntimeException("No match found for " + value + " in " + lookupName);
    }
    
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                        Math.min(
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1
                        ),
                        dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1)
                    );
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
}

