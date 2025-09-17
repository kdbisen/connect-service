# API Versioning Guide

This document explains the API versioning strategy for the Connect Service and how to use different versions.

## Overview

The Connect Service implements URL path versioning to provide backward compatibility while allowing for API evolution. Each version is maintained independently with its own set of features and capabilities.

## Supported Versions

| Version | Status | Base Path | Features | Deprecation Date |
|---------|--------|-----------|----------|------------------|
| **v1.0** | ✅ Active | `/v1/connect` | Basic processing, XML/JSON conversion, External API integration | Not deprecated |
| **v2.0** | ✅ Active | `/v2/connect` | Enhanced validation, Batch processing, Advanced monitoring | Not deprecated |

## Version-Specific Features

### v1.0 Features
- ✅ Basic request processing
- ✅ XML to JSON conversion
- ✅ External API integration
- ✅ Fenergo API integration
- ✅ MongoDB storage
- ✅ Audit logging
- ✅ Health checks
- ✅ Swagger documentation
- ✅ Correlation ID tracking
- ✅ Vault integration
- ✅ SSL MongoDB support
- ✅ Reactive processing

### v2.0 Features
- ✅ All v1.0 features
- ✅ Enhanced request validation
- ✅ Batch processing capabilities
- ✅ Advanced monitoring and metrics
- ✅ Real-time metrics
- ✅ Improved error handling
- ✅ Multi-connector support
- ✅ Route executor
- ✅ Enhanced health checks
- ✅ Version information endpoint
- 🔄 Visual UI console (coming soon)

## API Endpoints by Version

### v1.0 Endpoints
```
POST   /v1/connect/process
GET    /v1/connect/status/{requestId}
GET    /v1/connect/health
GET    /v1/connect/runs
GET    /v1/connect/runs/{requestId}/timeline
POST   /v1/connect/runs/{requestId}/retry
```

### v2.0 Endpoints
```
POST   /v2/connect/process
GET    /v2/connect/status/{requestId}
GET    /v2/connect/health
GET    /v2/connect/version
```

## Request Examples

### v1.0 Request
```bash
curl -X POST "https://connect-service.adyanta.com/v1/connect/process" \
  -H "Authorization: Bearer <apigee_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "requestType": "CUSTOMER_ONBOARDING",
    "payload": "<customer><name>John Doe</name></customer>",
    "metadata": {
      "source": "mobile_app",
      "version": "1.0",
      "clientId": "mobile_client_001"
    }
  }'
```

### v2.0 Request
```bash
curl -X POST "https://connect-service.adyanta.com/v2/connect/process" \
  -H "Authorization: Bearer <apigee_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "requestType": "CUSTOMER_ONBOARDING",
    "payload": "<customer><name>John Doe</name></customer>",
    "metadata": {
      "source": "mobile_app",
      "version": "2.0",
      "clientId": "mobile_client_001",
      "features": {
        "enhancedValidation": true,
        "batchProcessing": false,
        "advancedMonitoring": true
      }
    }
  }'
```

## Response Differences

### v1.0 Response
```json
{
  "requestId": "req_123456789",
  "status": "ACCEPTED",
  "message": "Request accepted for processing",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### v2.0 Response
```json
{
  "requestId": "req_123456789_v2",
  "status": "ACCEPTED",
  "message": "Request accepted for enhanced processing",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2024-01-15T10:30:00Z",
  "version": "2.0",
  "features": {
    "enhancedValidation": true,
    "batchProcessing": false,
    "advancedMonitoring": true
  }
}
```

## Version Headers

The API includes version-specific headers in responses:

### Common Headers
- `X-API-Version`: The API version used
- `X-API-Supported-Versions`: Comma-separated list of supported versions
- `X-Correlation-Id`: Request correlation ID

### v2.0 Specific Headers
- `X-API-Features`: Available features for the version
- `X-API-Version-Used`: Confirms the version used

## Migration Guide

### From v1.0 to v2.0

1. **Update Base URL**: Change from `/v1/connect` to `/v2/connect`
2. **Add Version Information**: Include `version: "2.0"` in metadata
3. **Enable Features**: Add `features` object to enable v2.0 capabilities
4. **Handle Enhanced Responses**: Process additional fields in responses
5. **Use New Endpoints**: Leverage new v2.0 endpoints like `/version`

### Example Migration

#### Before (v1.0)
```javascript
const response = await fetch('/v1/connect/process', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ' + token,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    requestType: 'CUSTOMER_ONBOARDING',
    payload: xmlData,
    metadata: {
      source: 'mobile_app',
      clientId: 'mobile_client_001'
    }
  })
});
```

#### After (v2.0)
```javascript
const response = await fetch('/v2/connect/process', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ' + token,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    requestType: 'CUSTOMER_ONBOARDING',
    payload: xmlData,
    metadata: {
      source: 'mobile_app',
      version: '2.0',
      clientId: 'mobile_client_001',
      features: {
        enhancedValidation: true,
        batchProcessing: false,
        advancedMonitoring: true
      }
    }
  })
});
```

## Version Detection

### Client-Side Detection
```javascript
// Check API version from response headers
const apiVersion = response.headers.get('X-API-Version');
const supportedVersions = response.headers.get('X-API-Supported-Versions');

if (apiVersion === '2.0') {
  // Handle v2.0 specific features
  const features = response.headers.get('X-API-Features');
  console.log('Available features:', features);
}
```

### Server-Side Detection
```java
@GetMapping("/process")
public Mono<ResponseEntity<ConnectResponseDto>> processRequest(
    HttpServletRequest request) {
    
    String apiVersion = (String) request.getAttribute("apiVersion");
    
    if ("2.0".equals(apiVersion)) {
        // Handle v2.0 specific logic
        return processRequestV2(request);
    } else {
        // Handle v1.0 logic
        return processRequestV1(request);
    }
}
```

## Deprecation Policy

### Timeline
- **12 months notice** before deprecation
- **24 months notice** before removal
- **Migration support** during transition period

### Deprecation Headers
When accessing deprecated versions, the API includes:
- `X-API-Deprecated: true`
- `X-API-Deprecated-Version: 1.0`
- `X-API-Current-Version: 2.0`
- `X-API-Migration-Guide: https://docs.adyanta.com/connect-service/migration/v1-to-v2`

## Error Handling

### Unsupported Version
```json
{
  "error": "UNSUPPORTED_VERSION",
  "message": "API version 3.0 is not supported",
  "supportedVersions": ["1.0", "2.0"],
  "currentVersion": "2.0",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Deprecated Version Warning
```json
{
  "requestId": "req_123456789",
  "status": "ACCEPTED",
  "message": "Request accepted for processing",
  "warning": "API version 1.0 is deprecated. Please migrate to version 2.0",
  "migrationGuide": "https://docs.adyanta.com/connect-service/migration/v1-to-v2",
  "deprecationDate": "2025-12-31"
}
```

## Testing Different Versions

### Using Swagger UI
1. **v1.0**: Access `/swagger-ui.html` and select v1.0 endpoints
2. **v2.0**: Access `/swagger-ui.html` and select v2.0 endpoints

### Using Postman
1. Import the appropriate OpenAPI specification
2. Set the base URL to include the version path
3. Use version-specific examples

### Using curl
```bash
# Test v1.0
curl -X GET "https://connect-service.adyanta.com/v1/connect/health"

# Test v2.0
curl -X GET "https://connect-service.adyanta.com/v2/connect/health"

# Get version information
curl -X GET "https://connect-service.adyanta.com/v2/connect/version"
```

## Best Practices

### For API Consumers
1. **Always specify version** in the URL path
2. **Handle version headers** in responses
3. **Plan for migration** when new versions are released
4. **Test thoroughly** when upgrading versions
5. **Monitor deprecation notices** in response headers

### For API Developers
1. **Maintain backward compatibility** within major versions
2. **Provide clear migration guides** for new versions
3. **Include deprecation notices** well in advance
4. **Version all breaking changes** appropriately
5. **Document version-specific features** clearly

## Configuration

### Application Properties
```yaml
api:
  versioning:
    current-version: "2.0"
    default-version: "1.0"
    supported-versions: ["1.0", "2.0"]
    deprecated-versions: []
    features:
      v1:
        basic-processing: true
        # ... other v1 features
      v2:
        basic-processing: true
        enhanced-validation: true
        batch-processing: true
        # ... other v2 features
```

### Environment Variables
```bash
export API_CURRENT_VERSION=2.0
export API_DEFAULT_VERSION=1.0
export API_SUPPORTED_VERSIONS=1.0,2.0
```

## Support

For questions about API versioning:

- **Documentation**: https://docs.adyanta.com/connect-service/versioning
- **Migration Guide**: https://docs.adyanta.com/connect-service/migration
- **Support Email**: support@adyanta.com
- **GitHub Issues**: Create an issue in the project repository

## Changelog

### v2.0.0 (2024-01-15)
- ✅ Enhanced request validation
- ✅ Added batch processing capabilities
- ✅ Improved error handling and reporting
- ✅ Added real-time metrics
- ✅ Enhanced monitoring and observability
- ✅ Multi-connector support
- ✅ Route executor
- ✅ Version information endpoint

### v1.0.0 (2024-01-01)
- ✅ Initial release
- ✅ Basic request processing
- ✅ XML to JSON conversion
- ✅ External API integration
- ✅ Fenergo API integration
- ✅ MongoDB storage
- ✅ Audit logging
- ✅ Health checks
- ✅ Swagger documentation
- ✅ Correlation ID tracking
- ✅ Vault integration
- ✅ SSL MongoDB support
- ✅ Reactive processing
