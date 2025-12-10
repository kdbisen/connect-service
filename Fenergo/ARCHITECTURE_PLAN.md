# Fenergo Integration Microservice Architecture Plan

## System Overview

A microservice-based system for integrating legacy banking XML data with Fenergo CLM/KYB APIs. The system transforms XML input into Fenergo-compliant JSON using config-driven mappings, resolves lookups, and manages entity creation workflows.

## Architecture Principles

- **Microservice Decomposition**: Separate concerns into dedicated services
- **Config-Driven**: Business Analysts configure mappings without code changes
- **Resilient**: Circuit breakers, retries, and graceful degradation
- **Scalable**: Stateless services with horizontal scaling
- **Observable**: Comprehensive logging, metrics, and tracing

## Microservice Breakdown

### 1. Fenergo Transformer Service (`adyanta-fenergo-transformer`)

**Purpose**: Core XML-to-JSON transformation engine with config-driven field mapping

**Responsibilities**:
- Parse XML input (various formats)
- Apply field mappings from configuration
- Transform data types and apply transformations
- Resolve lookup values via Lookup Service
- Generate Fenergo-compliant JSON payloads
- Validate transformed data

**Key Components**:
- `TransformerController` - REST endpoints for transformation
- `XmlParserService` - XML parsing and XPath extraction
- `FieldMappingService` - Apply config-driven mappings
- `DataTransformService` - Apply transformations (trim, upper, date format, etc.)
- `FenergoPayloadBuilder` - Construct Fenergo entity payloads
- `TransformerConfigRepository` - Load and cache mapping configs

**API Endpoints**:
```
POST /api/v1/transform/xml-to-fenergo
POST /api/v1/transform/validate
GET  /api/v1/transform/configs/{entityType}
POST /api/v1/transform/configs/{entityType}
```

**Technology Stack**:
- Spring Boot 3.x
- Jackson XML for parsing
- Apache POI for Excel processing
- Caffeine cache for config caching

---

### 2. Fenergo Lookup Service (`adyanta-fenergo-lookup`)

**Purpose**: Manage Fenergo lookup resolution and caching

**Responsibilities**:
- Fetch lookups from Fenergo Reference Data API
- Cache lookup tables (Redis + in-memory)
- Resolve LookupId from LookupName
- Support batch lookup resolution
- Handle lookup refresh/eviction

**Key Components**:
- `LookupController` - REST endpoints for lookup operations
- `FenergoLookupClient` - HTTP client for Fenergo Reference Data API
- `LookupCacheService` - Multi-tier caching (Redis + Caffeine)
- `LookupResolverService` - Resolve LookupId from values
- `LookupRefreshScheduler` - Periodic cache refresh

**API Endpoints**:
```
GET  /api/v1/lookups/{lookupName}
GET  /api/v1/lookups/{lookupName}/{value}
POST /api/v1/lookups/resolve-batch
POST /api/v1/lookups/refresh/{lookupName}
GET  /api/v1/lookups/cache/stats
```

**Technology Stack**:
- Spring Boot 3.x
- Spring Data Redis
- Caffeine cache
- WebClient for Fenergo API calls

---

### 3. Fenergo Entity Service (`adyanta-fenergo-entity`)

**Purpose**: Manage Fenergo entity lifecycle (create, update, query)

**Responsibilities**:
- Create entities in Fenergo via API
- Update existing entities
- Query entity status and data
- Handle entity workflows and status tracking
- Manage OAuth authentication with Fenergo

**Key Components**:
- `EntityController` - REST endpoints for entity operations
- `FenergoApiClient` - HTTP client for Fenergo Entity APIs
- `OAuthTokenService` - Manage OAuth client credentials flow
- `EntityStatusService` - Track entity creation/update status
- `EntityValidationService` - Validate payloads before submission

**API Endpoints**:
```
POST /api/v1/entities
PUT  /api/v1/entities/{entityId}
GET  /api/v1/entities/{entityId}
GET  /api/v1/entities/{entityId}/status
POST /api/v1/entities/search
```

**Technology Stack**:
- Spring Boot 3.x
- WebClient for Fenergo API
- Spring Security OAuth2 Client
- Resilience4j for circuit breakers

---

### 4. Fenergo Config Service (`adyanta-fenergo-config`)

**Purpose**: Manage transformation configuration (Excel → JSON, CRUD operations)

**Responsibilities**:
- Parse Excel mapping templates
- Generate JSON configs from Excel
- Store and version mapping configurations
- Provide config CRUD operations
- Validate mapping configurations

**Key Components**:
- `ConfigController` - REST endpoints for config management
- `ExcelParserService` - Parse Excel templates
- `ConfigGeneratorService` - Generate JSON from Excel
- `ConfigRepository` - Store configurations (database)
- `ConfigValidatorService` - Validate mapping rules

**API Endpoints**:
```
POST /api/v1/configs/generate-from-excel
GET  /api/v1/configs/{entityType}
POST /api/v1/configs/{entityType}
PUT  /api/v1/configs/{entityType}/{version}
GET  /api/v1/configs/{entityType}/versions
POST /api/v1/configs/{entityType}/validate
```

**Technology Stack**:
- Spring Boot 3.x
- Apache POI for Excel
- JPA/Hibernate for config storage
- PostgreSQL/MySQL for config database

---

### 5. Fenergo Orchestration Service (`adyanta-fenergo-orchestrator`)

**Purpose**: Orchestrate end-to-end workflows (XML → Transform → Lookup → Entity Creation)

**Responsibilities**:
- Coordinate multi-step transformation workflows
- Handle async processing and retries
- Manage workflow state
- Integrate with message queues for async processing
- Provide workflow monitoring

**Key Components**:
- `OrchestrationController` - REST endpoints for workflows
- `WorkflowEngine` - Execute multi-step workflows
- `WorkflowStateRepository` - Track workflow state
- `MessageQueueService` - Publish/consume async tasks
- `WorkflowMonitorService` - Monitor and report workflow status

**API Endpoints**:
```
POST /api/v1/workflows/transform-and-create
GET  /api/v1/workflows/{workflowId}
GET  /api/v1/workflows/{workflowId}/status
POST /api/v1/workflows/{workflowId}/retry
```

**Technology Stack**:
- Spring Boot 3.x
- Spring Integration for workflow orchestration
- RabbitMQ/Kafka for async processing
- PostgreSQL for workflow state

---

## Low-Level Design: Mapping System Architecture

### Mapping System Component Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    Transformer Service                          │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              TransformerController                        │  │
│  │  - POST /transform/xml-to-fenergo                         │  │
│  │  - POST /transform/validate                                │  │
│  │  - GET  /transform/configs/{entityType}                  │  │
│  └──────────────┬───────────────────────────────────────────┘  │
│                 │                                               │
│                 ▼                                               │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │         TransformationOrchestrator                         │  │
│  │  - Coordinates transformation pipeline                     │  │
│  │  - Manages transformation context                          │  │
│  │  - Handles error aggregation                               │  │
│  └──────┬──────────────┬──────────────┬──────────────────────┘  │
│         │              │              │                          │
│         ▼              ▼              ▼                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                     │
│  │   XML    │  │  Config  │  │  Lookup  │                     │
│  │  Parser  │  │  Loader  │  │ Resolver │                     │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘                     │
│       │             │              │                          │
│       └─────────────┼──────────────┘                          │
│                     │                                           │
│                     ▼                                           │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │         FieldMappingEngine                                  │  │
│  │  - Applies field mappings sequentially                     │  │
│  │  - Handles nested path resolution                          │  │
│  │  - Manages conditional mappings                           │  │
│  └──────┬──────────────┬──────────────┬──────────────────────┘  │
│         │              │              │                          │
│         ▼              ▼              ▼                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                     │
│  │   XPath  │  │ Transform │  │  Lookup │                     │
│  │ Extractor│  │  Service  │  │  Mapper  │                     │
│  └──────────┘  └──────────┘  └──────────┘                     │
│                                                                 │
│                     ▼                                           │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │      FenergoPayloadBuilder                                │  │
│  │  - Constructs nested DataGroups structure                 │  │
│  │  - Formats lookup fields                                  │  │
│  │  - Validates final payload                                │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Detailed Component Design

#### 1. TransformationOrchestrator

**Class Structure**:
```java
@Service
public class TransformationOrchestrator {
    private final XmlParserService xmlParser;
    private final ConfigLoaderService configLoader;
    private final FieldMappingEngine mappingEngine;
    private final LookupResolverService lookupResolver;
    private final FenergoPayloadBuilder payloadBuilder;
    private final TransformationValidator validator;
    
    public TransformResult transform(TransformRequest request) {
        // 1. Parse XML
        Document xmlDoc = xmlParser.parse(request.getXmlContent());
        
        // 2. Load config
        TransformerConfig config = configLoader.loadConfig(
            request.getEntityType(), 
            request.getConfigVersion()
        );
        
        // 3. Extract all lookup values (batch collection)
        Set<LookupRequest> lookupRequests = collectLookupRequests(xmlDoc, config);
        
        // 4. Resolve lookups in batch
        Map<String, LookupResolution> lookupCache = 
            lookupResolver.resolveBatch(lookupRequests);
        
        // 5. Apply mappings
        Map<String, Object> mappedData = mappingEngine.applyMappings(
            xmlDoc, config, lookupCache
        );
        
        // 6. Build payload
        FenergoEntityPayload payload = payloadBuilder.build(
            mappedData, 
            request.getEntityType(),
            request.getTenantId()
        );
        
        // 7. Validate
        ValidationResult validation = validator.validate(payload);
        
        return TransformResult.builder()
            .success(validation.isValid())
            .data(payload)
            .errors(validation.getErrors())
            .warnings(validation.getWarnings())
            .build();
    }
}
```

**Transformation Pipeline**:
```
Input XML → Parse → Extract Values → Load Config → Resolve Lookups 
→ Apply Mappings → Transform Data → Build Payload → Validate → Output
```

#### 2. FieldMappingEngine

**Class Structure**:
```java
@Service
public class FieldMappingEngine {
    private final XPathExtractor xpathExtractor;
    private final DataTransformService transformService;
    private final LookupMapper lookupMapper;
    private final ConditionalEvaluator conditionalEvaluator;
    
    public Map<String, Object> applyMappings(
        Document xmlDoc,
        TransformerConfig config,
        Map<String, LookupResolution> lookupCache
    ) {
        Map<String, Object> result = new HashMap<>();
        
        for (FieldMapping mapping : config.getMappings()) {
            try {
                // 1. Evaluate condition (if present)
                if (mapping.getCondition() != null) {
                    if (!conditionalEvaluator.evaluate(mapping.getCondition(), xmlDoc)) {
                        continue; // Skip this mapping
                    }
                }
                
                // 2. Extract value from XML
                Object value = xpathExtractor.extractValue(
                    xmlDoc, 
                    mapping.getXmlPath(), 
                    mapping.isMulti()
                );
                
                // 3. Handle missing values
                if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                    if (mapping.isRequired()) {
                        result.put("_errors", 
                            result.getOrDefault("_errors", new ArrayList<>()) + 
                            "Required field missing: " + mapping.getFenergoPath()
                        );
                        continue;
                    } else if (mapping.getDefaultValue() != null) {
                        value = mapping.getDefaultValue();
                    } else {
                        continue; // Skip optional fields with no default
                    }
                }
                
                // 4. Apply transformation
                if (mapping.getTransform() != null) {
                    value = transformService.applyTransform(value, mapping.getTransform());
                }
                
                // 5. Resolve lookup (if needed)
                if (mapping.getLookupName() != null) {
                    value = lookupMapper.mapToLookupFormat(
                        value,
                        mapping.getLookupName(),
                        mapping.isMulti(),
                        lookupCache
                    );
                }
                
                // 6. Set nested path in result
                setNestedPath(result, mapping.getFenergoPath(), value);
                
            } catch (Exception e) {
                // Log error and continue with next mapping
                log.error("Error processing mapping: {}", mapping.getFenergoPath(), e);
                result.put("_errors", 
                    result.getOrDefault("_errors", new ArrayList<>()) + 
                    "Error in " + mapping.getFenergoPath() + ": " + e.getMessage()
                );
            }
        }
        
        return result;
    }
    
    private void setNestedPath(Map<String, Object> result, String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = result;
        
        for (int i = 0; i < parts.length - 1; i++) {
            current = (Map<String, Object>) current.computeIfAbsent(parts[i], k -> new HashMap<>());
        }
        
        current.put(parts[parts.length - 1], value);
    }
}
```

**Mapping Algorithm**:
```
FOR each mapping in config.mappings:
    IF mapping.condition EXISTS:
        IF NOT evaluateCondition(mapping.condition, xmlDoc):
            CONTINUE
    
    value = extractFromXml(xmlDoc, mapping.xmlPath, mapping.multi)
    
    IF value IS NULL OR EMPTY:
        IF mapping.required:
            ADD error
            CONTINUE
        ELSE IF mapping.defaultValue EXISTS:
            value = mapping.defaultValue
        ELSE:
            CONTINUE
    
    IF mapping.transform EXISTS:
        value = applyTransform(value, mapping.transform)
    
    IF mapping.lookupName EXISTS:
        value = resolveLookup(value, mapping.lookupName, mapping.multi, lookupCache)
    
    SET nestedPath(result, mapping.fenergoPath, value)
END FOR
```

#### 3. XPathExtractor

**Class Structure**:
```java
@Service
public class XPathExtractor {
    private final XPathFactory xpathFactory;
    
    public Object extractValue(Document xmlDoc, String xpath, boolean isMulti) {
        try {
            XPath xpathObj = xpathFactory.newXPath();
            XPathExpression expr = xpathObj.compile(xpath);
            
            if (isMulti) {
                NodeList nodes = (NodeList) expr.evaluate(xmlDoc, XPathConstants.NODESET);
                return extractNodeList(nodes);
            } else {
                Node node = (Node) expr.evaluate(xmlDoc, XPathConstants.NODE);
                if (node == null) {
                    // Try as string value
                    String value = (String) expr.evaluate(xmlDoc, XPathConstants.STRING);
                    return value != null && !value.trim().isEmpty() ? value.trim() : null;
                }
                return extractNodeValue(node);
            }
        } catch (XPathExpressionException e) {
            throw new XPathExtractionException("Invalid XPath: " + xpath, e);
        }
    }
    
    private List<String> extractNodeList(NodeList nodes) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            String value = extractNodeValue(nodes.item(i));
            if (value != null && !value.isEmpty()) {
                values.add(value);
            }
        }
        return values.isEmpty() ? null : values;
    }
    
    private String extractNodeValue(Node node) {
        if (node.getNodeType() == Node.TEXT_NODE) {
            return node.getTextContent().trim();
        } else if (node.getNodeType() == Node.ELEMENT_NODE) {
            return node.getTextContent().trim();
        } else if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
            return node.getNodeValue().trim();
        }
        return null;
    }
}
```

**XPath Extraction Strategy**:
- Support absolute paths: `/root/entity/legalName`
- Support relative paths: `./address/country`
- Support attribute access: `/root/entity/@id`
- Support array indexing: `/root/entities/entity[0]/name`
- Support wildcards: `/root/*/name`
- Support namespaces: `/ns:root/ns:entity`

#### 4. DataTransformService

**Class Structure**:
```java
@Service
public class DataTransformService {
    
    public Object applyTransform(Object value, String transformType) {
        if (value == null) return null;
        
        String transform = transformType.toLowerCase().trim();
        
        // Handle parameterized transforms: "date:yyyy-MM-dd"
        if (transform.contains(":")) {
            String[] parts = transform.split(":", 2);
            String transformName = parts[0];
            String param = parts[1];
            
            switch (transformName) {
                case "date":
                    return parseDate(value.toString(), param);
                case "regex":
                    return applyRegex(value.toString(), param);
                default:
                    return applySimpleTransform(value, transform);
            }
        }
        
        return applySimpleTransform(value, transform);
    }
    
    private Object applySimpleTransform(Object value, String transform) {
        String strValue = value.toString();
        
        switch (transform) {
            case "trim":
                return strValue.trim();
            case "upper":
                return strValue.toUpperCase();
            case "lower":
                return strValue.toLowerCase();
            case "capitalize":
                return capitalize(strValue);
            case "number":
                return parseNumber(strValue);
            case "boolean":
                return parseBoolean(strValue);
            default:
                return value;
        }
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + 
               str.substring(1).toLowerCase();
    }
    
    private LocalDate parseDate(String dateStr, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return LocalDate.parse(dateStr, formatter);
    }
    
    private Number parseNumber(String numStr) {
        try {
            if (numStr.contains(".")) {
                return Double.parseDouble(numStr);
            } else {
                return Long.parseLong(numStr);
            }
        } catch (NumberFormatException e) {
            throw new TransformException("Invalid number: " + numStr);
        }
    }
}
```

**Transformation Types**:
- `trim` - Remove leading/trailing whitespace
- `upper` - Convert to uppercase
- `lower` - Convert to lowercase
- `capitalize` - First letter uppercase, rest lowercase
- `date:FORMAT` - Parse date string (e.g., `date:yyyy-MM-dd`)
- `number` - Parse numeric string
- `boolean` - Parse boolean string
- `regex:pattern:replacement` - Regex replace

#### 5. LookupMapper

**Class Structure**:
```java
@Service
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
            throw new LookupNotFoundException(
                "Lookup not found in cache: " + cacheKey
            );
        }
        
        return new LookupValue(
            resolution.getLookupId(), 
            value.toString()
        );
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
            throw new LookupNotFoundException("No lookup values resolved");
        }
        
        return new MultiLookupValue(lookupId, lookupValues);
    }
}
```

#### 6. FenergoPayloadBuilder

**Class Structure**:
```java
@Service
public class FenergoPayloadBuilder {
    private final PayloadValidator validator;
    
    public FenergoEntityPayload build(
        Map<String, Object> mappedData,
        String entityType,
        String tenantId
    ) {
        FenergoEntityPayload payload = new FenergoEntityPayload();
        payload.setTenantId(tenantId);
        payload.setEntityType(resolveEntityType(entityType));
        payload.setDataGroups(buildDataGroups(mappedData));
        
        validator.validate(payload);
        return payload;
    }
    
    private LookupValue resolveEntityType(String entityType) {
        // EntityType is also a lookup
        return new LookupValue(
            getEntityTypeLookupId(entityType),
            entityType
        );
    }
    
    private Map<String, Object> buildDataGroups(Map<String, Object> mappedData) {
        Map<String, Object> dataGroups = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : mappedData.entrySet()) {
            String key = entry.getKey();
            
            // Skip error keys
            if (key.startsWith("_")) {
                continue;
            }
            
            String[] pathParts = key.split("\\.");
            
            if (pathParts.length > 1 && pathParts[0].equals("DataGroups")) {
                String groupName = pathParts[1];
                String fieldPath = String.join(".", 
                    Arrays.copyOfRange(pathParts, 2, pathParts.length));
                
                Map<String, Object> group = (Map<String, Object>) 
                    dataGroups.computeIfAbsent(groupName, k -> new HashMap<>());
                
                setNestedField(group, fieldPath, entry.getValue());
            }
        }
        
        return dataGroups;
    }
    
    private void setNestedField(Map<String, Object> target, String path, Object value) {
        if (path == null || path.isEmpty()) {
            return;
        }
        
        String[] parts = path.split("\\.");
        Map<String, Object> current = target;
        
        for (int i = 0; i < parts.length - 1; i++) {
            current = (Map<String, Object>) current.computeIfAbsent(
                parts[i], 
                k -> new HashMap<>()
            );
        }
        
        current.put(parts[parts.length - 1], value);
    }
}
```

### Mapping Configuration Structure

#### Detailed Field Mapping Schema

```json
{
  "fenergoPath": "DataGroups.Address.Country",
  "fenergoType": "Lookup",
  "xmlPath": "/root/entity/address/countryCode",
  "lookupName": "Country",
  "multi": false,
  "transform": "upper",
  "required": true,
  "defaultValue": "IN",
  "condition": {
    "type": "xpath",
    "expression": "/root/entity/@type = 'LegalEntity'"
  },
  "validation": {
    "minLength": 2,
    "maxLength": 2,
    "pattern": "^[A-Z]{2}$"
  },
  "errorHandling": {
    "onMissing": "useDefault",
    "onInvalid": "reject"
  }
}
```

#### Conditional Mapping Logic

**Condition Types**:
1. **XPath Condition**: Evaluate XPath expression on XML
   ```json
   {
     "condition": {
       "type": "xpath",
       "expression": "/root/entity/@type = 'LegalEntity'"
     }
   }
   ```

2. **Value Condition**: Check extracted value
   ```json
   {
     "condition": {
       "type": "value",
       "field": "/root/entity/country",
       "operator": "equals",
       "value": "IN"
     }
   }
   ```

3. **Composite Condition**: AND/OR logic
   ```json
   {
     "condition": {
       "type": "and",
       "conditions": [
         {"type": "xpath", "expression": "/root/entity/@type = 'LegalEntity'"},
         {"type": "value", "field": "/root/entity/risk", "operator": ">", "value": "3"}
       ]
     }
   }
   ```

### Sequence Diagram: Complete Transformation Flow

```
Client                    Transformer          Config Service    Lookup Service    Fenergo API
  │                            │                      │                 │                │
  │ POST /transform            │                      │                 │                │
  │───────────────────────────>│                      │                 │                │
  │                            │                      │                 │                │
  │                            │ GET /configs/{type}  │                 │                │
  │                            │─────────────────────>│                 │                │
  │                            │<─────────────────────│                 │                │
  │                            │                      │                 │                │
  │                            │ Parse XML            │                 │                │
  │                            │──────────────────────┐                │                │
  │                            │<──────────────────────┘                │                │
  │                            │                      │                 │                │
  │                            │ Extract all lookup values              │                │
  │                            │──────────────────────┐                │                │
  │                            │<──────────────────────┘                │                │
  │                            │                      │                 │                │
  │                            │ POST /lookups/resolve-batch            │                │
  │                            │───────────────────────────────────────>│                │
  │                            │                      │                 │                │
  │                            │                      │                 │ GET /lookups    │
  │                            │                      │                 │───────────────>│
  │                            │                      │                 │<───────────────│
  │                            │<───────────────────────────────────────│                │
  │                            │                      │                 │                │
  │                            │ Apply mappings       │                 │                │
  │                            │──────────────────────┐                │                │
  │                            │<──────────────────────┘                │                │
  │                            │                      │                 │                │
  │                            │ Build payload        │                 │                │
  │                            │──────────────────────┐                │                │
  │                            │<──────────────────────┘                │                │
  │                            │                      │                 │                │
  │                            │ Validate payload     │                 │                │
  │                            │──────────────────────┐                │                │
  │                            │<──────────────────────┘                │                │
  │                            │                      │                 │                │
  │<───────────────────────────│                      │                 │                │
  │                            │                      │                 │                │
```

### Data Flow: Nested Path Resolution

**Input XML**:
```xml
<root>
  <entity type="LegalEntity">
    <name>
      <legalName>TechIoT Solutions</legalName>
      <shortName>TechIoT</shortName>
    </name>
    <address>
      <country>IN</country>
      <city>Bangalore</city>
    </address>
  </entity>
</root>
```

**Mapping Config**:
```json
[
  {"fenergoPath": "DataGroups.Name.LegalName", "xmlPath": "/root/entity/name/legalName"},
  {"fenergoPath": "DataGroups.Address.Country", "xmlPath": "/root/entity/address/country", "lookupName": "Country"}
]
```

**Transformation Steps**:

1. **Parse XML** → DOM Document
2. **Extract Values**:
   - `/root/entity/name/legalName` → `"TechIoT Solutions"`
   - `/root/entity/address/country` → `"IN"`
3. **Resolve Lookups**:
   - `Country:IN` → `{LookupId: 123, LookupName: "IN"}`
4. **Build Intermediate Map**:
   ```json
   {
     "DataGroups.Name.LegalName": "TechIoT Solutions",
     "DataGroups.Address.Country": {"LookupId": 123, "LookupName": "IN"}
   }
   ```
5. **Build Nested Structure**:
   ```json
   {
     "DataGroups": {
       "Name": {
         "LegalName": "TechIoT Solutions"
       },
       "Address": {
         "Country": {"LookupId": 123, "LookupName": "IN"}
       }
     }
   }
   ```

### Lookup Resolution Algorithm

```java
public class LookupResolverService {
    
    public LookupResolution resolve(String lookupName, String value) {
        // 1. Check L1 cache (Caffeine)
        String cacheKey = lookupName + ":" + value;
        LookupResolution cached = l1Cache.get(cacheKey);
        if (cached != null) return cached;
        
        // 2. Check L2 cache (Redis)
        cached = redisCache.get(cacheKey);
        if (cached != null) {
            l1Cache.put(cacheKey, cached);
            return cached;
        }
        
        // 3. Fetch from Fenergo API
        List<FenergoLookup> lookups = fenergoClient.fetchLookups(lookupName);
        
        // 4. Find match (exact, case-insensitive, fuzzy)
        LookupResolution resolution = findMatch(lookups, value);
        
        // 5. Cache result
        l1Cache.put(cacheKey, resolution);
        redisCache.put(cacheKey, resolution, Duration.ofHours(24));
        
        return resolution;
    }
    
    private LookupResolution findMatch(List<FenergoLookup> lookups, String value) {
        // Exact match
        Optional<FenergoLookup> exact = lookups.stream()
            .filter(l -> l.getLookupName().equals(value))
            .findFirst();
        if (exact.isPresent()) {
            return new LookupResolution(exact.get().getLookupId(), value);
        }
        
        // Case-insensitive match
        Optional<FenergoLookup> caseInsensitive = lookups.stream()
            .filter(l -> l.getLookupName().equalsIgnoreCase(value))
            .findFirst();
        if (caseInsensitive.isPresent()) {
            return new LookupResolution(caseInsensitive.get().getLookupId(), value);
        }
        
        // Fuzzy match (Levenshtein distance)
        Optional<FenergoLookup> fuzzy = lookups.stream()
            .filter(l -> levenshteinDistance(l.getLookupName(), value) <= 2)
            .findFirst();
        if (fuzzy.isPresent()) {
            return new LookupResolution(fuzzy.get().getLookupId(), value);
        }
        
        throw new LookupNotFoundException("No match found for " + value + " in " + lookupName);
    }
    
    private int levenshteinDistance(String s1, String s2) {
        // Levenshtein distance implementation
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
```

### Error Handling Strategy

**Error Types**:
1. **XML Parse Error**: Invalid XML structure
2. **XPath Error**: Invalid XPath expression or no match
3. **Lookup Error**: Lookup value not found
4. **Validation Error**: Transformed value fails validation
5. **Required Field Missing**: Required field has no value and no default

**Error Aggregation**:
```java
public class TransformResult {
    private boolean success;
    private FenergoEntityPayload data;
    private List<TransformError> errors;
    private List<TransformWarning> warnings;
    
    public void addError(ErrorType type, String field, String message) {
        errors.add(new TransformError(type, field, message));
    }
    
    public void addWarning(String field, String message) {
        warnings.add(new TransformWarning(field, message));
    }
}

public enum ErrorType {
    XML_PARSE_ERROR,
    XPATH_EXTRACTION_ERROR,
    LOOKUP_NOT_FOUND,
    VALIDATION_ERROR,
    REQUIRED_FIELD_MISSING,
    TRANSFORM_ERROR
}
```

### Performance Optimization

**Batch Lookup Resolution**:
- Collect all lookup values during mapping phase
- Resolve all lookups in single batch API call
- Cache results for subsequent mappings

**Parallel Processing**:
- Parse XML and load config in parallel
- Resolve multiple lookups concurrently
- Apply independent mappings in parallel

**Caching Strategy**:
- L1: Caffeine (in-memory, 1000 entries, 1 hour TTL)
- L2: Redis (distributed, 24 hour TTL)
- Config cache: 1 hour TTL, refresh on version change

## Data Flow Architecture

### Synchronous Flow (Simple Transform)
```
XML Input → Transformer Service → Lookup Service (resolve lookups) 
→ Transformer Service (build payload) → Entity Service → Fenergo API
```

### Asynchronous Flow (Complex Workflows)
```
XML Input → Orchestrator → [Queue] → Transformer → [Queue] 
→ Lookup Resolution → [Queue] → Entity Creation → [Queue] 
→ Status Update → Webhook/Notification
```

## Database Schema Design

### Config Service Database
```sql
-- Mapping Configurations
CREATE TABLE fenergo_mapping_configs (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    version VARCHAR(20) NOT NULL,
    config_json JSONB NOT NULL,
    created_at TIMESTAMP,
    created_by VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    UNIQUE(entity_type, version)
);

-- Excel Templates Metadata
CREATE TABLE excel_templates (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50),
    template_name VARCHAR(200),
    file_path VARCHAR(500),
    uploaded_at TIMESTAMP,
    uploaded_by VARCHAR(100)
);
```

### Orchestrator Service Database
```sql
-- Workflow State
CREATE TABLE fenergo_workflows (
    id BIGSERIAL PRIMARY KEY,
    workflow_id VARCHAR(100) UNIQUE,
    entity_type VARCHAR(50),
    status VARCHAR(50),
    input_xml TEXT,
    transformed_json JSONB,
    entity_id VARCHAR(100),
    error_message TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

## API Contracts

### Transformer Service API

**POST /api/v1/transform/xml-to-fenergo**
```json
Request:
{
  "xmlContent": "<root>...</root>",
  "entityType": "LegalEntity",
  "configVersion": "1.0",
  "tenantId": "tenant-123"
}

Response:
{
  "success": true,
  "data": {
    "TenantId": "tenant-123",
    "EntityType": {"LookupId": 1, "LookupName": "LegalEntity"},
    "DataGroups": { ... }
  },
  "errors": [],
  "warnings": []
}
```

### Lookup Service API

**POST /api/v1/lookups/resolve-batch**
```json
Request:
{
  "lookups": [
    {"lookupName": "Country", "value": "IN"},
    {"lookupName": "RiskRating", "value": "Medium"}
  ]
}

Response:
{
  "resolved": [
    {"lookupName": "Country", "value": "IN", "lookupId": 123, "lookupName": "IN"},
    {"lookupName": "RiskRating", "value": "Medium", "lookupId": 345, "lookupName": "Medium"}
  ],
  "failed": []
}
```

### Entity Service API

**POST /api/v1/entities**
```json
Request:
{
  "entityPayload": { ... },
  "async": false
}

Response:
{
  "entityId": "ENT-12345",
  "status": "CREATED",
  "message": "Entity created successfully"
}
```

## Integration Patterns

### Service-to-Service Communication
- **Synchronous**: REST APIs with circuit breakers
- **Asynchronous**: Message queues (RabbitMQ/Kafka)
- **Service Discovery**: Eureka/Consul (if needed)

### Frontend Integration
- React components in `adyanta-iot-ui/src/components/Fenergo/`
- API client in `adyanta-iot-ui/src/services/fenergo/`
- Route in `adyanta-iot-ui/src/App.tsx`

### External Integration
- **Fenergo APIs**: OAuth2 client credentials
- **Legacy Systems**: XML webhooks, file uploads, SFTP

## Configuration Management

### Application Configuration
- `application.yml` per service
- Environment-specific configs (dev, staging, prod)
- Fenergo credentials via environment variables/secrets

### Mapping Configuration
- Excel templates stored in Config Service
- JSON configs generated and versioned
- Config validation before activation

## Deployment Architecture

### Container Structure
```
adyanta-fenergo-transformer/
├── Dockerfile
├── pom.xml
├── src/main/java/...
└── k8s/
    ├── deployment.yaml
    ├── service.yaml
    └── configmap.yaml
```

### Kubernetes Deployment
- Each service as separate deployment
- ConfigMaps for application config
- Secrets for Fenergo credentials
- Service mesh (Istio) for inter-service communication

## Monitoring & Observability

### Logging
- Structured logging (JSON format)
- Correlation IDs for request tracing
- Log aggregation (ELK stack)

### Metrics
- Prometheus metrics endpoints
- Custom metrics: transformation success rate, lookup cache hit rate
- Grafana dashboards

### Tracing
- Distributed tracing (Jaeger/Zipkin)
- Request flow visualization

## Security Considerations

- OAuth2 for Fenergo API authentication
- JWT for inter-service authentication
- Secrets management (Vault/K8s secrets)
- Input validation and sanitization
- Rate limiting on public endpoints

## Testing Strategy

- Unit tests for transformation logic
- Integration tests for API endpoints
- Contract tests for service interfaces
- End-to-end tests for workflows
- Performance tests for high-volume scenarios

