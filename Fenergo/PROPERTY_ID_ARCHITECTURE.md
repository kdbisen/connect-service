# Property ID Architecture - Central Field Catalog System

## Overview

This document describes the **Property ID system** where `propertyId` serves as the **primary key** that ties together mapping, field definition, lookup, policy, and validation throughout the XML→JSON transformation pipeline.

## Core Principle

**Single Source of Truth**: A central `field_catalog` stores all field definitions with `propertyId` as the stable identifier. All other components (mappings, policies, validation reports) reference this `propertyId` instead of duplicating field paths.

## What is PropertyId?

The `propertyId` is the **fully qualified Fenergo field path** that uniquely identifies a field in the Fenergo data model:

- `DataGroups.Name.LegalName`
- `DataGroups.Address.Country`
- `DataGroups.Regulatory.TaxId`
- `DataGroups.Identification.IdType`

This path is stored **once** in `field_catalog` and **reused everywhere** as the canonical identifier.

## Data Structures

### 1. Field Catalog (MongoDB Collection: `field_catalog`)

The field catalog is the **source of truth** for all field definitions.

```json
{
  "_id": "DataGroups.Address.Country",        // propertyId (fully qualified path)
  "env": "UAT",
  "entityType": "LegalEntity",
  "dataGroup": "Address",
  "fieldName": "Country",
  "fenergoType": "Lookup",
  "lookupName": "Country",
  "maxLength": 2,
  "pattern": "^[A-Z]{2}$",
  "required": false,                           // Required by Fenergo schema
  "description": "Country code (ISO 3166-1 alpha-2)",
  "metadata": {
    "displayLabel": "Country",
    "helpText": "Select the country code"
  }
}
```

**Key Fields**:
- `_id`: propertyId (fully qualified Fenergo path) - **PRIMARY KEY**
- `env`: Environment identifier (UAT, PROD)
- `entityType`: LegalEntity, Individual, Trust, etc.
- `dataGroup`: Name, Address, Identification, Risk, Business, etc.
- `fieldName`: LegalName, Country, IdType, RiskRating, etc.
- `fenergoType`: Text, Lookup, MultiLookup, Date, Number, Boolean, LongText, Reference
- `lookupName`: If lookup field, which lookup table (Country, RiskRating, IdTypes, etc.)
- **Validation Constraints**: `maxLength`, `pattern`, `min`, `max`, `required`
- `description`: Human-readable description
- `metadata`: Additional metadata (display labels, help text, etc.)

**Indexes**:
```javascript
db.field_catalog.createIndex({ "env": 1, "entityType": 1 })
db.field_catalog.createIndex({ "dataGroup": 1, "fieldName": 1 })
db.field_catalog.createIndex({ "lookupName": 1 })
```

### 2. Mapping Config (Updated Structure)

Mapping configurations reference `propertyId` from the field catalog.

```json
{
  "entityType": "LegalEntity",
  "version": "1.0",
  "env": "UAT",
  "sourceSystem": "PartnerA",
  "operationType": "onboarding",
  "fields": [
    {
      "propertyId": "DataGroups.Address.Country",  // References field_catalog
      "xmlPaths": [
        "/Customer/Address/CountryCode",
        "/root/entity/address/country",
        "/root/customer/country"
      ],
      "transform": "upper",
      "requiredByMapping": true,                    // Mapping-level requirement
      "defaultValue": "IN",
      "condition": {
        "type": "xpath",
        "expression": "/root/entity/@type = 'LegalEntity'"
      }
    },
    {
      "propertyId": "DataGroups.Name.LegalName",
      "xmlPaths": ["/root/entity/name/legalName"],
      "transform": "trim",
      "requiredByMapping": true,
      "defaultValue": null
    }
  ]
}
```

**Key Changes from Previous Design**:
- ✅ `propertyId` replaces `fenergoPath` as primary identifier
- ✅ `xmlPaths` is now an **array** (multiple XPath options for flexibility)
- ✅ `requiredByMapping` (mapping-level requirement, separate from schema `required`)
- ❌ Removed: `fenergoType`, `lookupName` (now retrieved from field_catalog via propertyId)

### 3. Validation Report Structure

Validation results are keyed by `propertyId` for easy tracking and reporting.

```json
{
  "requestId": "REQ-123",
  "entityType": "LegalEntity",
  "env": "UAT",
  "sourceSystem": "PartnerA",
  "timestamp": "2025-01-15T10:30:00Z",
  "overallStatus": "FAIL",
  "fieldResults": [
    {
      "propertyId": "DataGroups.Address.Country",
      "status": "PASS",
      "value": "IN",
      "resolvedLookup": {
        "lookupId": 123,
        "lookupName": "IN"
      },
      "errors": [],
      "warnings": []
    },
    {
      "propertyId": "DataGroups.Name.LegalName",
      "status": "FAIL",
      "value": null,
      "errors": [
        {
          "code": "REQUIRED_FIELD_MISSING",
          "message": "Missing required field",
          "severity": "ERROR"
        }
      ],
      "warnings": []
    },
    {
      "propertyId": "DataGroups.Regulatory.TaxId",
      "status": "WARNING",
      "value": "TAX123",
      "errors": [],
      "warnings": [
        {
          "code": "PATTERN_MISMATCH",
          "message": "Tax ID format may be invalid",
          "severity": "WARNING"
        }
      ]
    }
  ]
}
```

## Pipeline Flow with PropertyId

### Step A: Load Mapping Config

```java
// Load mapping config for env, sourceSystem, entityType, operationType
TransformerConfig config = configLoader.loadConfig(
    request.getEnv(),
    request.getSourceSystem(),
    request.getEntityType(),
    request.getOperationType()
);

// Each field has propertyId that references field_catalog
for (FieldMapping mappingField : config.getFields()) {
    String propertyId = mappingField.getPropertyId();  // "DataGroups.Address.Country"
    List<String> xmlPaths = mappingField.getXmlPaths();
    // Process each mapping...
}
```

### Step B: Extract & Transform (Canonical JSON by PropertyId)

Extract values from XML and store in a canonical JSON structure **keyed by propertyId**.

```java
Map<String, Object> canonicalJson = new HashMap<>();  // Keyed by propertyId

for (FieldMapping mappingField : config.getFields()) {
    String propertyId = mappingField.getPropertyId();
    
    // Try each xmlPath until one returns a value
    Object value = null;
    for (String xmlPath : mappingField.getXmlPaths()) {
        value = xpathExtractor.extractValue(xmlDoc, xmlPath, mappingField.isMulti());
        if (value != null && !isEmpty(value)) {
            break;  // Found a value, stop trying other paths
        }
    }
    
    // Apply default if no value found
    if (value == null && mappingField.getDefaultValue() != null) {
        value = mappingField.getDefaultValue();
    }
    
    // Apply transformation
    if (value != null && mappingField.getTransform() != null) {
        value = transformService.applyTransform(value, mappingField.getTransform());
    }
    
    // Store in canonical JSON keyed by propertyId
    canonicalJson.put(propertyId, value);
}
```

**Result**: 
```json
{
  "DataGroups.Address.Country": "IN",
  "DataGroups.Name.LegalName": "TechIoT Solutions",
  "DataGroups.Identification.IdType": "GSTIN",
  "DataGroups.Risk.RiskRating": "Medium"
}
```

### Step C: Look Up Field Definition by PropertyId

For each propertyId in canonical JSON, look up the field definition from the catalog.

```java
Map<String, FieldCatalogEntry> fieldDefinitions = new HashMap<>();

for (String propertyId : canonicalJson.keySet()) {
    // Look up field definition from catalog
    FieldCatalogEntry fieldDef = fieldCatalogService.getFieldDefinition(
        propertyId, 
        request.getEnv(), 
        request.getEntityType()
    );
    
    if (fieldDef == null) {
        // Field not in catalog - log warning or create entry
        log.warn("Field not found in catalog: {}", propertyId);
        continue;
    }
    
    fieldDefinitions.put(propertyId, fieldDef);
    
    // fieldDef tells you:
    // - fenergoType (Text, Lookup, Date, etc.)
    // - lookupName (if lookup field)
    // - maxLength, pattern (validation constraints)
    // - required (by Fenergo schema)
}
```

### Step D: Validate Using PropertyId

Validate each field using its propertyId to look up constraints and requirements.

```java
ValidationReport validationReport = new ValidationReport();
validationReport.setRequestId(request.getRequestId());

for (String propertyId : canonicalJson.keySet()) {
    FieldCatalogEntry fieldDef = fieldDefinitions.get(propertyId);
    Object value = canonicalJson.get(propertyId);
    
    FieldValidationResult fieldResult = new FieldValidationResult();
    fieldResult.setPropertyId(propertyId);
    fieldResult.setValue(value);
    
    // Check if required (from policy or mapping)
    boolean isRequired = isFieldRequired(
        propertyId, 
        activePolicies, 
        mappingConfig
    );
    
    if (isRequired && isEmpty(value)) {
        fieldResult.addError(
            "REQUIRED_FIELD_MISSING",
            "Missing required field",
            "ERROR"
        );
    }
    
    // Validate type/constraints from fieldDef
    if (fieldDef != null) {
        // Validate lookup fields
        if (fieldDef.getFenergoType() == FenergoFieldType.LOOKUP) {
            LookupResolution lookup = resolveLookup(
                fieldDef.getLookupName(), 
                value
            );
            if (lookup == null) {
                fieldResult.addError(
                    "INVALID_LOOKUP_VALUE",
                    "Invalid lookup value: " + value,
                    "ERROR"
                );
            } else {
                fieldResult.setResolvedLookup(lookup);
            }
        }
        
        // Validate constraints
        if (value != null) {
            String strValue = value.toString();
            
            if (fieldDef.getMaxLength() != null && 
                strValue.length() > fieldDef.getMaxLength()) {
                fieldResult.addError(
                    "MAX_LENGTH_EXCEEDED",
                    "Value exceeds maxLength of " + fieldDef.getMaxLength(),
                    "ERROR"
                );
            }
            
            if (fieldDef.getPattern() != null && 
                !strValue.matches(fieldDef.getPattern())) {
                fieldResult.addWarning(
                    "PATTERN_MISMATCH",
                    "Value does not match expected pattern",
                    "WARNING"
                );
            }
        }
    }
    
    validationReport.addFieldResult(fieldResult);
}
```

### Step E: Build Fenergo JSON Using PropertyId

Convert canonical JSON (keyed by propertyId) to Fenergo JSON structure.

```java
Map<String, Object> fenergoDataGroups = new HashMap<>();

for (String propertyId : canonicalJson.keySet()) {
    FieldCatalogEntry fieldDef = fieldDefinitions.get(propertyId);
    Object value = canonicalJson.get(propertyId);
    
    if (value == null) {
        continue;  // Skip null values
    }
    
    // Convert to Fenergo format based on field type
    Object fenergoValue;
    if (fieldDef.getFenergoType() == FenergoFieldType.LOOKUP) {
        LookupResolution lookup = resolveLookup(fieldDef.getLookupName(), value);
        fenergoValue = new LookupValue(lookup.getLookupId(), lookup.getLookupName());
    } else if (fieldDef.getFenergoType() == FenergoFieldType.MULTI_LOOKUP) {
        // Handle multi-lookup
        List<LookupValue> lookupValues = resolveMultiLookup(fieldDef.getLookupName(), value);
        fenergoValue = new MultiLookupValue(lookupValues);
    } else {
        fenergoValue = convertType(fieldDef, value);
    }
    
    // Set in Fenergo JSON structure (split propertyId on dots)
    setInFenergoJson(fenergoDataGroups, propertyId, fenergoValue);
}

// Helper method: "DataGroups.Address.Country" → fenergoDataGroups["Address"]["Country"]
private void setInFenergoJson(
    Map<String, Object> dataGroups, 
    String propertyId, 
    Object value
) {
    String[] parts = propertyId.split("\\.");
    // parts[0] = "DataGroups"
    // parts[1] = "Address" (dataGroup name)
    // parts[2] = "Country" (field name)
    
    if (parts.length < 3 || !parts[0].equals("DataGroups")) {
        log.warn("Invalid propertyId format: {}", propertyId);
        return;
    }
    
    String dataGroupName = parts[1];
    String fieldName = parts[2];
    
    Map<String, Object> group = (Map<String, Object>) 
        dataGroups.computeIfAbsent(dataGroupName, k -> new HashMap<>());
    group.put(fieldName, value);
}
```

**Result**:
```json
{
  "TenantId": "tenant-123",
  "EntityType": {"LookupId": 1, "LookupName": "LegalEntity"},
  "DataGroups": {
    "Address": {
      "Country": {"LookupId": 123, "LookupName": "IN"}
    },
    "Name": {
      "LegalName": "TechIoT Solutions"
    }
  }
}
```

## Implementation Components

### 1. FieldCatalogEntry Entity

```java
package com.adyanta.connect.fenergo.domain.document;

import com.adyanta.connect.fenergo.domain.enums.FenergoFieldType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

/**
 * Field catalog entry - source of truth for field definitions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "field_catalog")
public class FieldCatalogEntry {
    @Id
    private String propertyId;        // "DataGroups.Address.Country" (PRIMARY KEY)
    
    private String env;               // "UAT", "PROD"
    private String entityType;        // "LegalEntity", "Individual", "Trust"
    private String dataGroup;         // "Address", "Name", "Identification"
    private String fieldName;         // "Country", "LegalName", "IdType"
    private FenergoFieldType fenergoType;
    private String lookupName;        // If lookup field, which lookup table
    private Integer maxLength;
    private String pattern;           // Regex validation pattern
    private Integer min;              // For Number fields
    private Integer max;              // For Number fields
    private boolean required;         // Required by Fenergo schema
    private String description;
    private Map<String, Object> metadata;  // Display labels, help text, etc.
}
```

### 2. FieldCatalogRepository

```java
package com.adyanta.connect.fenergo.repository;

import com.adyanta.connect.fenergo.domain.document.FieldCatalogEntry;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FieldCatalogRepository extends ReactiveMongoRepository<FieldCatalogEntry, String> {
    
    Mono<FieldCatalogEntry> findByPropertyIdAndEnvAndEntityType(
        String propertyId, 
        String env, 
        String entityType
    );
    
    Flux<FieldCatalogEntry> findByEnvAndEntityType(
        String env, 
        String entityType
    );
    
    Flux<FieldCatalogEntry> findByEnvAndEntityTypeAndDataGroup(
        String env, 
        String entityType,
        String dataGroup
    );
    
    Mono<Boolean> existsByPropertyIdAndEnvAndEntityType(
        String propertyId,
        String env,
        String entityType
    );
}
```

### 3. FieldCatalogService

```java
package com.adyanta.connect.fenergo.service;

import com.adyanta.connect.fenergo.domain.document.FieldCatalogEntry;
import com.adyanta.connect.fenergo.repository.FieldCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing field catalog entries
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FieldCatalogService {
    
    private final FieldCatalogRepository repository;
    
    /**
     * Get field definition by propertyId
     */
    public FieldCatalogEntry getFieldDefinition(
        String propertyId, 
        String env, 
        String entityType
    ) {
        return repository.findByPropertyIdAndEnvAndEntityType(propertyId, env, entityType)
            .block();
    }
    
    /**
     * Get or create field definition
     * If not exists, creates entry by parsing propertyId
     */
    public FieldCatalogEntry getOrCreate(
        String propertyId, 
        String env, 
        String entityType
    ) {
        FieldCatalogEntry existing = getFieldDefinition(propertyId, env, entityType);
        if (existing != null) {
            return existing;
        }
        
        // Parse propertyId to extract dataGroup and fieldName
        // Format: "DataGroups.{dataGroup}.{fieldName}"
        String[] parts = propertyId.split("\\.");
        if (parts.length < 3 || !parts[0].equals("DataGroups")) {
            throw new IllegalArgumentException("Invalid propertyId format: " + propertyId);
        }
        
        String dataGroup = parts[1];
        String fieldName = parts[2];
        
        // Create new entry (with defaults - should be populated from Fenergo API)
        FieldCatalogEntry newEntry = FieldCatalogEntry.builder()
            .propertyId(propertyId)
            .env(env)
            .entityType(entityType)
            .dataGroup(dataGroup)
            .fieldName(fieldName)
            .fenergoType(FenergoFieldType.TEXT)  // Default, should be updated
            .required(false)
            .build();
        
        return repository.save(newEntry).block();
    }
    
    /**
     * Batch load field definitions
     */
    public Map<String, FieldCatalogEntry> loadFieldDefinitions(
        List<String> propertyIds,
        String env,
        String entityType
    ) {
        Map<String, FieldCatalogEntry> result = new HashMap<>();
        
        for (String propertyId : propertyIds) {
            FieldCatalogEntry fieldDef = getFieldDefinition(propertyId, env, entityType);
            if (fieldDef != null) {
                result.put(propertyId, fieldDef);
            }
        }
        
        return result;
    }
    
    /**
     * Get all fields for an entity type
     */
    public List<FieldCatalogEntry> getAllFields(String env, String entityType) {
        return repository.findByEnvAndEntityType(env, entityType)
            .collectList()
            .block();
    }
}
```

### 4. Updated FieldMapping

```java
package com.adyanta.connect.fenergo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents a field mapping configuration
 * Uses propertyId as primary key (references field_catalog)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldMapping {
    /**
     * PRIMARY KEY - References field_catalog._id
     * Format: "DataGroups.{dataGroup}.{fieldName}"
     */
    private String propertyId;
    
    /**
     * Array of XPath expressions to try (in order)
     * First non-null value wins
     */
    private List<String> xmlPaths;
    
    private String transform;                    // trim, upper, lower, capitalize, date:format
    private boolean requiredByMapping;            // Mapping-level requirement
    private String defaultValue;
    private Map<String, Object> condition;      // Conditional mapping
    private Map<String, Object> validation;      // Additional validation rules
    private Map<String, Object> errorHandling;   // Error handling strategy
    
    /**
     * @deprecated Use propertyId instead. Kept for backward compatibility.
     */
    @Deprecated
    private String fenergoPath;
    
    /**
     * @deprecated Retrieved from field_catalog via propertyId
     */
    @Deprecated
    private FenergoFieldType fenergoType;
    
    /**
     * @deprecated Retrieved from field_catalog via propertyId
     */
    @Deprecated
    private String lookupName;
}
```

### 5. Updated TransformerConfig

```java
package com.adyanta.connect.fenergo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Transformer configuration for entity type mapping
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransformerConfig {
    private String entityType;
    private String version;
    private String env;                    // NEW: Environment
    private String sourceSystem;           // NEW: Source system identifier
    private String operationType;          // NEW: onboarding, update, etc.
    private List<FieldMapping> fields;     // Updated: uses propertyId
}
```

### 6. Updated TransformationOrchestrator

```java
package com.adyanta.connect.fenergo.service;

import com.adyanta.connect.fenergo.domain.dto.*;
import com.adyanta.connect.fenergo.domain.document.FieldCatalogEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import java.util.*;

/**
 * Orchestrator for the complete transformation pipeline
 * Uses propertyId as primary key throughout
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransformationOrchestrator {
    
    private final XmlParserService xmlParser;
    private final ConfigLoaderService configLoader;
    private final FieldCatalogService fieldCatalogService;
    private final XPathExtractor xpathExtractor;
    private final DataTransformService transformService;
    private final LookupResolverService lookupResolver;
    private final FenergoPayloadBuilder payloadBuilder;
    
    public TransformResult transform(TransformRequest request) {
        TransformResult result = TransformResult.builder()
            .success(false)
            .errors(new ArrayList<>())
            .warnings(new ArrayList<>())
            .build();
        
        try {
            // Step A: Parse XML
            Document xmlDoc = xmlParser.parse(request.getXmlContent());
            
            // Step B: Load mapping config
            TransformerConfig config = configLoader.loadConfig(
                request.getEnv(),
                request.getSourceSystem(),
                request.getEntityType(),
                request.getOperationType()
            );
            
            // Step C: Extract to canonical JSON (keyed by propertyId)
            Map<String, Object> canonicalJson = extractToCanonicalJson(xmlDoc, config);
            
            // Step D: Load field definitions from catalog
            Map<String, FieldCatalogEntry> fieldDefinitions = fieldCatalogService
                .loadFieldDefinitions(
                    new ArrayList<>(canonicalJson.keySet()),
                    request.getEnv(),
                    request.getEntityType()
                );
            
            // Step E: Validate
            ValidationReport validationReport = validate(
                canonicalJson,
                fieldDefinitions,
                config,
                request.getActivePolicies()
            );
            
            // Step F: Build Fenergo JSON
            FenergoEntityPayload payload = buildFenergoPayload(
                canonicalJson,
                fieldDefinitions,
                request
            );
            
            result.setSuccess(validationReport.isValid());
            result.setData(convertPayloadToMap(payload));
            result.setErrors(validationReport.getErrors());
            result.setWarnings(validationReport.getWarnings());
            
        } catch (Exception e) {
            log.error("Error in transformation", e);
            result.addError(ErrorType.XML_PARSE_ERROR, null, e.getMessage());
        }
        
        return result;
    }
    
    private Map<String, Object> extractToCanonicalJson(
        Document xmlDoc,
        TransformerConfig config
    ) {
        Map<String, Object> canonicalJson = new HashMap<>();
        
        for (FieldMapping mappingField : config.getFields()) {
            String propertyId = mappingField.getPropertyId();
            
            // Try each xmlPath until one returns a value
            Object value = null;
            for (String xmlPath : mappingField.getXmlPaths()) {
                value = xpathExtractor.extractValue(xmlDoc, xmlPath, false);
                if (value != null && !isEmpty(value)) {
                    break;
                }
            }
            
            // Apply default if no value found
            if (value == null && mappingField.getDefaultValue() != null) {
                value = mappingField.getDefaultValue();
            }
            
            // Apply transformation
            if (value != null && mappingField.getTransform() != null) {
                value = transformService.applyTransform(value, mappingField.getTransform());
            }
            
            // Store in canonical JSON keyed by propertyId
            canonicalJson.put(propertyId, value);
        }
        
        return canonicalJson;
    }
    
    private ValidationReport validate(
        Map<String, Object> canonicalJson,
        Map<String, FieldCatalogEntry> fieldDefinitions,
        TransformerConfig config,
        List<String> activePolicies
    ) {
        ValidationReport report = new ValidationReport();
        // Implementation as described in Step D
        return report;
    }
    
    private FenergoEntityPayload buildFenergoPayload(
        Map<String, Object> canonicalJson,
        Map<String, FieldCatalogEntry> fieldDefinitions,
        TransformRequest request
    ) {
        // Implementation as described in Step E
        return payloadBuilder.build(canonicalJson, fieldDefinitions, request);
    }
    
    private boolean isEmpty(Object value) {
        if (value == null) return true;
        if (value instanceof String) return ((String) value).trim().isEmpty();
        return false;
    }
    
    private Map<String, Object> convertPayloadToMap(FenergoEntityPayload payload) {
        Map<String, Object> map = new HashMap<>();
        map.put("TenantId", payload.getTenantId());
        map.put("EntityType", payload.getEntityType());
        map.put("DataGroups", payload.getDataGroups());
        return map;
    }
}
```

## Benefits

### 1. Single Join Key
- `propertyId` links mapping → catalog → validation → Fenergo JSON
- No string parsing or path manipulation needed
- Easy to query and join across collections

### 2. No Duplication
- Field path stored **once** in `field_catalog`
- Referenced everywhere by `propertyId`
- Reduces maintenance overhead

### 3. Easy Refactoring
- If Fenergo path changes, update `field_catalog` only
- All mappings, policies, and validation automatically use new path
- Internal `propertyId` can remain stable

### 4. Clear Validation Reports
- Errors keyed by `propertyId`
- Easy to trace to UI labels, policies, and partner mappings
- Can aggregate errors by `propertyId` across requests

### 5. Policy Integration
- Policies reference `propertyId` for field rules
- Easy to find "which policies touch this field?"
- Policy requirements can be cached by `propertyId`

### 6. Reporting & Analytics
- Query validation errors by `propertyId`
- Track field-level metrics across partners
- "All failures for TaxId across all partners"

## Migration Strategy

### Phase 1: Create Field Catalog
1. Generate `propertyId` from existing `fenergoPath` values in configs
2. Populate `field_catalog` with field definitions
3. Seed catalog with common fields (Name, Address, Identification, etc.)

### Phase 2: Update Mapping Configs
1. Add `propertyId` to each field mapping
2. Convert `xmlPath` (single) to `xmlPaths` (array)
3. Keep `fenergoPath` for backward compatibility

### Phase 3: Update Transformation Pipeline
1. Modify `TransformationOrchestrator` to use `propertyId`
2. Implement canonical JSON structure (keyed by propertyId)
3. Update validation to use field catalog lookups
4. Update payload builder to use propertyId

### Phase 4: Remove Deprecated Fields
1. Remove `fenergoPath`, `fenergoType`, `lookupName` from mappings
2. All field metadata retrieved from `field_catalog` via `propertyId`

## Example: Complete Flow

### Input XML
```xml
<root>
  <entity type="LegalEntity">
    <name>
      <legalName>TechIoT Solutions Pvt Ltd</legalName>
    </name>
    <address>
      <country>IN</country>
    </address>
  </entity>
</root>
```

### Mapping Config
```json
{
  "fields": [
    {
      "propertyId": "DataGroups.Name.LegalName",
      "xmlPaths": ["/root/entity/name/legalName"],
      "transform": "trim",
      "requiredByMapping": true
    },
    {
      "propertyId": "DataGroups.Address.Country",
      "xmlPaths": ["/root/entity/address/country"],
      "transform": "upper",
      "requiredByMapping": true,
      "defaultValue": "IN"
    }
  ]
}
```

### Field Catalog Lookups
```json
{
  "DataGroups.Name.LegalName": {
    "fenergoType": "Text",
    "required": true,
    "maxLength": 200
  },
  "DataGroups.Address.Country": {
    "fenergoType": "Lookup",
    "lookupName": "Country",
    "required": true,
    "maxLength": 2
  }
}
```

### Canonical JSON (Step B)
```json
{
  "DataGroups.Name.LegalName": "TechIoT Solutions Pvt Ltd",
  "DataGroups.Address.Country": "IN"
}
```

### Validation Results (Step D)
```json
{
  "DataGroups.Name.LegalName": {
    "status": "PASS",
    "value": "TechIoT Solutions Pvt Ltd"
  },
  "DataGroups.Address.Country": {
    "status": "PASS",
    "value": "IN",
    "resolvedLookup": {"lookupId": 123, "lookupName": "IN"}
  }
}
```

### Final Fenergo JSON (Step E)
```json
{
  "TenantId": "tenant-123",
  "EntityType": {"LookupId": 1, "LookupName": "LegalEntity"},
  "DataGroups": {
    "Name": {
      "LegalName": "TechIoT Solutions Pvt Ltd"
    },
    "Address": {
      "Country": {"LookupId": 123, "LookupName": "IN"}
    }
  }
}
```

## API Endpoints for Field Catalog

### Get Field Definition
```
GET /api/v1/fenergo/catalog/{propertyId}?env=UAT&entityType=LegalEntity
```

### List All Fields
```
GET /api/v1/fenergo/catalog?env=UAT&entityType=LegalEntity
GET /api/v1/fenergo/catalog?env=UAT&entityType=LegalEntity&dataGroup=Address
```

### Create/Update Field
```
POST /api/v1/fenergo/catalog
PUT /api/v1/fenergo/catalog/{propertyId}
```

### Sync from Fenergo
```
POST /api/v1/fenergo/catalog/sync?env=UAT&entityType=LegalEntity
```
(Syncs field definitions from Fenergo API)

## Summary

The Property ID system provides:

- ✅ **Stable identifiers** for all fields
- ✅ **Single source of truth** in field_catalog
- ✅ **Clean separation** between mapping (xmlPaths) and field definition (catalog)
- ✅ **Easy validation** using catalog constraints
- ✅ **Better reporting** with propertyId-keyed results
- ✅ **Policy integration** via propertyId references
- ✅ **Maintainable** - change field definition once, affects all references

This architecture makes the system more robust, maintainable, and scalable for handling multiple partners, environments, and entity types.

