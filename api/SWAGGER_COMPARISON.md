# OpenAPI 3.0 vs Swagger 2.0 Comparison

This document explains the differences between the OpenAPI 3.0 and Swagger 2.0 specifications for the Connect Service API.

## Files Available

- `connect-service-openapi.yaml` - OpenAPI 3.0 specification
- `connect-service-swagger2.yaml` - Swagger 2.0 specification
- `SWAGGER_COMPARISON.md` - This comparison document

## Key Differences

### 1. Specification Version

| Aspect | OpenAPI 3.0 | Swagger 2.0 |
|--------|-------------|--------------|
| **Version Header** | `openapi: 3.0.3` | `swagger: '2.0'` |
| **Release Date** | July 2017 | September 2014 |
| **Status** | Current standard | Legacy (still widely used) |

### 2. Server Configuration

#### OpenAPI 3.0
```yaml
servers:
  - url: https://connect-service.adyanta.com
    description: Production server
  - url: https://connect-service-dev.adyanta.com
    description: Development server
  - url: http://localhost:8080
    description: Local development server
```

#### Swagger 2.0
```yaml
host: connect-service.adyanta.com
basePath: /
schemes:
  - https
  - http
```

**Key Difference**: OpenAPI 3.0 supports multiple servers with descriptions, while Swagger 2.0 only supports one host/basePath combination.

### 3. Security Definitions

#### OpenAPI 3.0
```yaml
components:
  securitySchemes:
    ApigeeAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: Authentication via Apigee Gateway using JWT tokens
```

#### Swagger 2.0
```yaml
securityDefinitions:
  ApigeeAuth:
    type: oauth2
    authorizationUrl: https://apigee.adyanta.com/oauth/authorize
    tokenUrl: https://apigee.adyanta.com/oauth/token
    flow: implicit
    scopes:
      connect:process: Access to process requests
      connect:status: Access to check request status
      connect:admin: Access to administrative operations
```

**Key Difference**: OpenAPI 3.0 has more flexible security schemes, while Swagger 2.0 uses OAuth2 flow definitions.

### 4. Request Body Definitions

#### OpenAPI 3.0
```yaml
requestBody:
  required: true
  content:
    application/json:
      schema:
        $ref: '#/components/schemas/ConnectRequest'
      examples:
        customer_onboarding:
          summary: Customer Onboarding Request
          value:
            requestType: "CUSTOMER_ONBOARDING"
            payload: "<?xml version=\"1.0\"?>..."
```

#### Swagger 2.0
```yaml
parameters:
  - name: body
    in: body
    required: true
    schema:
      $ref: '#/definitions/ConnectRequest'
```

**Key Difference**: OpenAPI 3.0 supports multiple content types and rich examples, while Swagger 2.0 uses simpler parameter-based body definitions.

### 5. Response Examples

#### OpenAPI 3.0
```yaml
responses:
  '202':
    description: Request accepted for processing
    headers:
      X-Correlation-Id:
        description: Unique correlation ID for tracking this request
        schema:
          type: string
          example: "550e8400-e29b-41d4-a716-446655440000"
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/ConnectResponse'
        examples:
          accepted:
            summary: Request Accepted
            value:
              requestId: "req_123456789"
              status: "ACCEPTED"
```

#### Swagger 2.0
```yaml
responses:
  202:
    description: Request accepted for processing
    headers:
      X-Correlation-Id:
        type: string
        description: Unique correlation ID for tracking this request
        example: "550e8400-e29b-41d4-a716-446655440000"
    schema:
      $ref: '#/definitions/ConnectResponse'
    examples:
      application/json:
        requestId: "req_123456789"
        status: "ACCEPTED"
```

**Key Difference**: OpenAPI 3.0 supports multiple examples per response with descriptions, while Swagger 2.0 has simpler example structures.

### 6. Schema Definitions

#### OpenAPI 3.0
```yaml
components:
  schemas:
    ConnectRequest:
      type: object
      required:
        - requestType
        - payload
      properties:
        requestType:
          $ref: '#/components/schemas/RequestType'
        payload:
          oneOf:
            - type: string
              description: XML payload as string
            - type: object
              description: JSON payload as object
```

#### Swagger 2.0
```yaml
definitions:
  ConnectRequest:
    type: object
    required:
      - requestType
      - payload
    properties:
      requestType:
        $ref: '#/definitions/RequestType'
      payload:
        description: XML payload as string or JSON payload as object
```

**Key Difference**: OpenAPI 3.0 supports `oneOf`, `anyOf`, `allOf` for complex schemas, while Swagger 2.0 has more limited schema composition.

### 7. Callbacks and Webhooks

#### OpenAPI 3.0
```yaml
callbacks:
  onCompletion:
    '{$request.body#/callbackUrl}':
      post:
        requestBody:
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/CompletionNotification'
```

#### Swagger 2.0
```yaml
# Not supported in Swagger 2.0
```

**Key Difference**: OpenAPI 3.0 supports callbacks and webhooks, while Swagger 2.0 does not.

## Feature Comparison Matrix

| Feature | OpenAPI 3.0 | Swagger 2.0 | Notes |
|---------|-------------|-------------|-------|
| **Multiple Servers** | ✅ | ❌ | OpenAPI 3.0 supports multiple server definitions |
| **Rich Examples** | ✅ | ⚠️ | OpenAPI 3.0 has more detailed example structures |
| **Callbacks/Webhooks** | ✅ | ❌ | Only available in OpenAPI 3.0 |
| **Schema Composition** | ✅ | ⚠️ | OpenAPI 3.0 supports oneOf, anyOf, allOf |
| **Link Objects** | ✅ | ❌ | OpenAPI 3.0 supports hypermedia links |
| **Request Body** | ✅ | ⚠️ | OpenAPI 3.0 has more flexible request body definitions |
| **Security Schemes** | ✅ | ⚠️ | OpenAPI 3.0 has more security scheme types |
| **Parameter Objects** | ✅ | ✅ | Both support parameter objects |
| **Response Objects** | ✅ | ✅ | Both support response objects |
| **Schema Objects** | ✅ | ✅ | Both support schema objects |
| **Tool Support** | ✅ | ✅ | Both have extensive tool support |

## When to Use Which Version

### Use OpenAPI 3.0 When:
- **Modern Development**: Building new APIs or updating existing ones
- **Rich Documentation**: Need detailed examples and multiple content types
- **Multiple Environments**: Have different servers for dev/staging/prod
- **Webhooks**: Need to document callback mechanisms
- **Complex Schemas**: Require advanced schema composition
- **Future-Proofing**: Want to use the current standard

### Use Swagger 2.0 When:
- **Legacy Systems**: Working with existing Swagger 2.0 tooling
- **Simple APIs**: Basic REST APIs without complex requirements
- **Tool Compatibility**: Specific tools only support Swagger 2.0
- **Team Familiarity**: Team is more familiar with Swagger 2.0

## Migration Considerations

### From Swagger 2.0 to OpenAPI 3.0

1. **Update Version**: Change `swagger: '2.0'` to `openapi: 3.0.3`
2. **Server Configuration**: Convert `host`/`basePath`/`schemes` to `servers`
3. **Security Definitions**: Update `securityDefinitions` to `components.securitySchemes`
4. **Schema Definitions**: Move `definitions` to `components.schemas`
5. **Parameter Bodies**: Convert `parameters` with `in: body` to `requestBody`
6. **Response Examples**: Update example structures to use `examples` object

### Tool Compatibility

| Tool | OpenAPI 3.0 | Swagger 2.0 |
|------|-------------|-------------|
| **Swagger UI** | ✅ | ✅ |
| **Swagger Editor** | ✅ | ✅ |
| **Postman** | ✅ | ✅ |
| **Insomnia** | ✅ | ✅ |
| **Thunder Client** | ✅ | ✅ |
| **OpenAPI Generator** | ✅ | ✅ |
| **Swagger Codegen** | ✅ | ✅ |
| **SpringDoc OpenAPI** | ✅ | ✅ |
| **Swagger Parser** | ✅ | ✅ |

## Recommendations

### For New Projects
- **Use OpenAPI 3.0** - It's the current standard with better features
- **Future-proof** - Most tools support OpenAPI 3.0
- **Rich Documentation** - Better support for examples and descriptions

### For Existing Projects
- **Evaluate Migration** - Consider upgrading to OpenAPI 3.0
- **Tool Support** - Ensure your tools support the version you choose
- **Team Training** - Train team on the chosen specification version

### For This Connect Service
- **Both Versions Available** - Use the version that best fits your needs
- **OpenAPI 3.0 Recommended** - For new integrations and documentation
- **Swagger 2.0 Available** - For legacy tool compatibility

## Conclusion

Both specifications are valid and widely supported. Choose based on your specific needs:

- **OpenAPI 3.0**: Modern, feature-rich, current standard
- **Swagger 2.0**: Stable, widely adopted, legacy support

The Connect Service provides both versions to ensure maximum compatibility with different tools and environments.
