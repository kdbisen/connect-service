# Connect Service API Documentation

This directory contains the complete OpenAPI 3.0 specification for the Connect Service API.

## Files

- `connect-service-openapi.yaml` - Complete OpenAPI 3.0 specification
- `README.md` - This documentation file

## Overview

The Connect Service API is a robust Spring Boot reactive microservice for orchestrating data flow between multiple external services. It acts as a connector hub, processing XML/JSON payloads through configurable pipelines and managing authentication with various external systems.

## Key Features

- **Reactive Processing**: Built with Spring WebFlux and Project Reactor
- **Multi-Service Orchestration**: Connect and orchestrate multiple external services
- **Flexible Authentication**: Support for Apigee Gateway, OAuth2, and mTLS
- **Audit & Monitoring**: Complete request tracking with correlation IDs
- **SSL MongoDB**: Advanced MongoDB connectivity with SSL/TLS
- **Vault Integration**: Secure credential management with HashiCorp Vault

## API Endpoints

### Core Operations

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/connect/process` | POST | Submit a request for processing |
| `/connect/status/{requestId}` | GET | Get request processing status |
| `/connect/health` | GET | Health check endpoint |

### Administrative Operations

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/connect/runs` | GET | List processing runs with filtering |
| `/connect/runs/{requestId}/timeline` | GET | Get detailed processing timeline |
| `/connect/runs/{requestId}/retry` | POST | Retry a failed request |

## Request Types Supported

- **CUSTOMER_ONBOARDING**: Customer onboarding workflow
- **DOCUMENT_VERIFICATION**: Document verification process
- **RISK_ASSESSMENT**: Risk assessment workflow
- **COMPLIANCE_CHECK**: Compliance verification
- **GENERIC**: Generic processing workflow

## Authentication

All requests must be authenticated via Apigee Gateway using JWT tokens. Include the Apigee token in the Authorization header:

```
Authorization: Bearer <apigee_token>
```

## Using the OpenAPI Specification

### 1. View in Swagger UI

The service includes Swagger UI at `/swagger-ui.html` when running locally or in development environments.

### 2. Import into API Testing Tools

#### Postman
1. Open Postman
2. Click "Import" 
3. Select "Link" tab
4. Paste the URL to the OpenAPI file or upload the YAML file
5. Postman will automatically create a collection with all endpoints

#### Insomnia
1. Open Insomnia
2. Click "Create" → "Import from" → "File"
3. Select the `connect-service-openapi.yaml` file
4. Insomnia will create a workspace with all endpoints

#### Thunder Client (VS Code)
1. Install Thunder Client extension in VS Code
2. Click "Import" → "OpenAPI"
3. Select the `connect-service-openapi.yaml` file
4. Thunder Client will create a collection

### 3. Generate Client SDKs

#### Using OpenAPI Generator

```bash
# Generate Java client
openapi-generator generate -i connect-service-openapi.yaml -g java -o ./java-client

# Generate TypeScript client
openapi-generator generate -i connect-service-openapi.yaml -g typescript-fetch -o ./typescript-client

# Generate Python client
openapi-generator generate -i connect-service-openapi.yaml -g python -o ./python-client
```

#### Using Swagger Codegen

```bash
# Generate Java client
swagger-codegen generate -i connect-service-openapi.yaml -l java -o ./java-client

# Generate JavaScript client
swagger-codegen generate -i connect-service-openapi.yaml -l javascript -o ./javascript-client
```

### 4. Validate API Responses

The OpenAPI specification includes JSON schemas for all responses. You can use these schemas to validate API responses:

```javascript
// Example using ajv (JavaScript)
const Ajv = require('ajv');
const ajv = new Ajv();

// Load the schema for ConnectResponse
const connectResponseSchema = {
  // Schema from the OpenAPI spec
};

const validate = ajv.compile(connectResponseSchema);
const isValid = validate(responseData);
```

## Sample Requests

### 1. Customer Onboarding Request

```bash
curl -X POST "https://connect-service.adyanta.com/connect/process" \
  -H "Authorization: Bearer <apigee_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "requestType": "CUSTOMER_ONBOARDING",
    "payload": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><customer><firstName>John</firstName><lastName>Doe</lastName></customer>",
    "metadata": {
      "source": "mobile_app",
      "version": "1.0",
      "clientId": "mobile_client_001"
    }
  }'
```

### 2. Document Verification Request

```bash
curl -X POST "https://connect-service.adyanta.com/connect/process" \
  -H "Authorization: Bearer <apigee_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "requestType": "DOCUMENT_VERIFICATION",
    "payload": {
      "documentType": "PASSPORT",
      "documentNumber": "P123456789",
      "expiryDate": "2030-12-31",
      "issuingCountry": "USA",
      "holderName": "John Doe",
      "verificationLevel": "ENHANCED"
    },
    "metadata": {
      "source": "web_portal",
      "version": "2.1",
      "clientId": "web_client_002"
    }
  }'
```

### 3. Check Request Status

```bash
curl -X GET "https://connect-service.adyanta.com/connect/status/req_123456789" \
  -H "Authorization: Bearer <apigee_token>"
```

### 4. Health Check

```bash
curl -X GET "https://connect-service.adyanta.com/connect/health"
```

## Response Examples

### Successful Processing Response

```json
{
  "requestId": "req_123456789",
  "status": "ACCEPTED",
  "message": "Request accepted for processing",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Processing Status Response

```json
{
  "requestId": "req_123456789",
  "status": "PROCESSING",
  "currentStep": "EXTERNAL_API_CALL",
  "progress": 60,
  "message": "Processing external API call",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:32:00Z",
  "steps": [
    {
      "step": "XML_TO_JSON_CONVERSION",
      "status": "COMPLETED",
      "completedAt": "2024-01-15T10:30:30Z",
      "duration": 500
    },
    {
      "step": "EXTERNAL_API_CALL",
      "status": "IN_PROGRESS",
      "startedAt": "2024-01-15T10:30:30Z"
    },
    {
      "step": "FENERGO_API_CALL",
      "status": "PENDING"
    },
    {
      "step": "COMPLETION",
      "status": "PENDING"
    }
  ]
}
```

### Health Check Response

```json
{
  "status": "UP",
  "timestamp": "2024-01-15T10:30:00Z",
  "version": "1.0.0",
  "components": {
    "database": {
      "status": "UP",
      "details": {
        "connectionPool": "ACTIVE",
        "activeConnections": 5,
        "maxConnections": 20
      }
    },
    "externalServices": {
      "status": "UP",
      "details": {
        "xmlToJsonService": "UP",
        "fenergoService": "UP",
        "apigeeGateway": "UP"
      }
    },
    "vault": {
      "status": "UP",
      "details": {
        "connection": "ACTIVE",
        "secretsAccessible": true
      }
    }
  },
  "metrics": {
    "requestsPerMinute": 45,
    "averageResponseTime": 250,
    "errorRate": 0.02,
    "activeConnections": 5
  }
}
```

## Error Handling

The API uses standard HTTP status codes and returns detailed error information:

### Common Error Responses

#### 400 Bad Request
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Invalid request type: UNSUPPORTED_TYPE",
  "details": "Request type must be one of: CUSTOMER_ONBOARDING, DOCUMENT_VERIFICATION, RISK_ASSESSMENT, COMPLIANCE_CHECK, GENERIC",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

#### 401 Unauthorized
```json
{
  "error": "AUTHENTICATION_FAILED",
  "message": "Invalid or expired Apigee token",
  "details": "Please ensure you have a valid Apigee token in the Authorization header",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

#### 429 Too Many Requests
```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests. Please try again later.",
  "details": "Rate limit: 100 requests per minute",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

#### 500 Internal Server Error
```json
{
  "error": "INTERNAL_ERROR",
  "message": "An unexpected error occurred while processing the request",
  "details": "Please contact support if this issue persists",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## Rate Limiting

The API implements rate limiting to ensure fair usage:

- **Rate Limit**: 100 requests per minute per client
- **Burst Limit**: 10 requests per second
- **Headers**: Rate limit information is included in response headers:
  - `X-RateLimit-Limit`: Maximum requests per window
  - `X-RateLimit-Remaining`: Remaining requests in current window
  - `X-RateLimit-Reset`: Time when the rate limit resets

## Monitoring and Observability

### Correlation IDs

Every request includes a correlation ID (`X-Correlation-Id`) for tracking:

- **Request Tracking**: Track requests across multiple services
- **Log Correlation**: Link logs from different components
- **Debugging**: Easily trace request flow and identify issues

### Metrics

The service exposes metrics at `/actuator/prometheus`:

- Request count and duration
- Error rates by endpoint
- External service response times
- Database connection pool metrics
- Circuit breaker states

### Health Checks

The `/connect/health` endpoint provides:

- Overall service health status
- Individual component health (database, external services, vault)
- Performance metrics
- Dependency status

## Security Considerations

### Authentication
- All requests must be authenticated via Apigee Gateway
- JWT tokens are validated for each request
- Token expiration is enforced

### Data Protection
- Sensitive data is masked in logs and responses
- PII fields are redacted in audit logs
- SSL/TLS encryption for all communications

### Input Validation
- All input data is validated against schemas
- XML payloads are validated for well-formedness
- JSON payloads are validated against defined schemas

## Deployment Environments

### Development
- URL: `https://connect-service-dev.adyanta.com`
- Swagger UI: Available at `/swagger-ui.html`
- Debug logging: Enabled

### Production
- URL: `https://connect-service.adyanta.com`
- Swagger UI: Disabled for security
- Debug logging: Disabled

## Support

For API support and questions:

- **Email**: support@adyanta.com
- **Documentation**: https://docs.adyanta.com/connect-service
- **Issues**: Create an issue in the project repository

## Version History

- **v1.0.0** (2024-01-15): Initial release with core functionality
  - Basic request processing
  - XML to JSON conversion
  - External service integration
  - Fenergo API integration
  - MongoDB storage
  - Audit logging
  - Health checks
  - Swagger UI integration

