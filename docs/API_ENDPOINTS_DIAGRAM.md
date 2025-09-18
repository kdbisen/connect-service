# Connect Service API Endpoints Diagram

## API Endpoints Overview

This diagram shows all the API endpoints, their relationships, and the internal processing flow.

## Mermaid Diagram

```mermaid
graph TB
    %% External Clients
    Client[Client Applications]
    Admin[Admin Users]
    Monitor[Monitoring Systems]
    
    %% API Gateway
    Apigee[Apigee Gateway<br/>Authentication & Rate Limiting]
    
    %% Load Balancer
    LB[Load Balancer/NGINX<br/>SSL Termination & Routing]
    
    %% Connect Service API Endpoints
    subgraph "Connect Service API Endpoints"
        %% Version 1.0 Endpoints
        subgraph "API v1.0"
            V1Process[POST /v1/connect/process<br/>Process XML/JSON Requests]
            V1Status[GET /v1/connect/status/{requestId}<br/>Get Processing Status]
            V1Health[GET /v1/connect/health<br/>Health Check]
        end
        
        %% Version 2.0 Endpoints
        subgraph "API v2.0"
            V2Process[POST /v2/connect/process<br/>Enhanced Processing with Validation]
            V2Status[GET /v2/connect/status/{requestId}<br/>Enhanced Status with Metadata]
            V2Health[GET /v2/connect/health<br/>Enhanced Health Check]
            V2Version[GET /v2/connect/version<br/>API Version Information]
        end
        
        %% Monitoring Endpoints
        subgraph "Monitoring & Management"
            ActuatorHealth[GET /actuator/health<br/>Spring Boot Health]
            ActuatorMetrics[GET /actuator/metrics<br/>Application Metrics]
            ActuatorInfo[GET /actuator/info<br/>Application Information]
            SwaggerUI[GET /swagger-ui.html<br/>API Documentation]
            OpenAPI[GET /v3/api-docs<br/>OpenAPI Specification]
        end
    end
    
    %% Internal Processing
    subgraph "Internal Processing"
        ConnectService[ConnectService<br/>Main Orchestrator]
        AsyncProcessing[@Async Processing<br/>Background Tasks]
        ProcessorFactory[RequestProcessorFactory<br/>Strategy Pattern]
        ProcessingPipeline[ProcessingPipeline<br/>Chain of Responsibility]
    end
    
    %% External Services
    subgraph "External Services"
        XmlToJsonService[XML-to-JSON Service<br/>via Apigee Gateway]
        FenergoService[Fenergo API<br/>OAuth2 Authentication]
        OtherServices[Other External APIs<br/>Multi-Connector Support]
    end
    
    %% Data Storage
    subgraph "Data Storage"
        MongoDB[(MongoDB<br/>Processing Requests & Audit Logs)]
        Vault[(HashiCorp Vault<br/>Secrets & Certificates)]
    end
    
    %% Monitoring Infrastructure
    subgraph "Monitoring Infrastructure"
        Prometheus[Prometheus<br/>Metrics Collection]
        Grafana[Grafana<br/>Dashboards & Alerts]
        Logs[Centralized Logging<br/>ELK Stack]
    end
    
    %% Client Connections
    Client --> Apigee
    Admin --> Apigee
    Monitor --> LB
    
    %% API Gateway Flow
    Apigee --> LB
    
    %% Load Balancer Routing
    LB --> V1Process
    LB --> V1Status
    LB --> V1Health
    LB --> V2Process
    LB --> V2Status
    LB --> V2Health
    LB --> V2Version
    
    %% Monitoring Access
    Monitor --> ActuatorHealth
    Monitor --> ActuatorMetrics
    Monitor --> ActuatorInfo
    Admin --> SwaggerUI
    Admin --> OpenAPI
    
    %% Internal Processing Flow
    V1Process --> ConnectService
    V2Process --> ConnectService
    V1Status --> ConnectService
    V2Status --> ConnectService
    
    ConnectService --> AsyncProcessing
    AsyncProcessing --> ProcessorFactory
    ProcessorFactory --> ProcessingPipeline
    
    %% External Service Calls
    ProcessingPipeline --> XmlToJsonService
    ProcessingPipeline --> FenergoService
    ProcessingPipeline --> OtherServices
    
    %% Data Persistence
    ConnectService --> MongoDB
    AsyncProcessing --> MongoDB
    ProcessingPipeline --> MongoDB
    
    %% Configuration & Secrets
    ConnectService --> Vault
    ProcessingPipeline --> Vault
    
    %% Monitoring Data Flow
    ConnectService --> Prometheus
    AsyncProcessing --> Prometheus
    ProcessingPipeline --> Prometheus
    Prometheus --> Grafana
    
    ConnectService --> Logs
    AsyncProcessing --> Logs
    ProcessingPipeline --> Logs
    
    %% Styling
    classDef clientLayer fill:#e3f2fd,stroke:#1976d2,stroke-width:2px
    classDef apiLayer fill:#e8f5e8,stroke:#388e3c,stroke-width:2px
    classDef processingLayer fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    classDef externalLayer fill:#ffebee,stroke:#d32f2f,stroke-width:2px
    classDef dataLayer fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    classDef monitoringLayer fill:#e0f2f1,stroke:#00695c,stroke-width:2px
    
    class Client,Admin,Monitor clientLayer
    class V1Process,V1Status,V1Health,V2Process,V2Status,V2Health,V2Version,ActuatorHealth,ActuatorMetrics,ActuatorInfo,SwaggerUI,OpenAPI apiLayer
    class ConnectService,AsyncProcessing,ProcessorFactory,ProcessingPipeline processingLayer
    class XmlToJsonService,FenergoService,OtherServices externalLayer
    class MongoDB,Vault dataLayer
    class Prometheus,Grafana,Logs monitoringLayer
```

## API Endpoints Details

### Version 1.0 Endpoints

#### 1. POST /v1/connect/process
- **Purpose**: Process XML/JSON requests with fire-and-forget pattern
- **Authentication**: Apigee Gateway
- **Request Body**: ConnectRequestDto
- **Response**: 202 Accepted with request ID
- **Processing**: Asynchronous background processing

#### 2. GET /v1/connect/status/{requestId}
- **Purpose**: Get processing status for a specific request
- **Authentication**: Apigee Gateway
- **Path Parameter**: requestId (String)
- **Response**: ConnectResponseDto with current status
- **Data Source**: MongoDB ProcessingRequest collection

#### 3. GET /v1/connect/health
- **Purpose**: Basic health check endpoint
- **Authentication**: None (public endpoint)
- **Response**: Simple health status
- **Usage**: Load balancer health checks

### Version 2.0 Endpoints

#### 1. POST /v2/connect/process
- **Purpose**: Enhanced processing with additional validation and features
- **Authentication**: Apigee Gateway
- **Request Body**: Enhanced ConnectRequestDto with metadata
- **Response**: 202 Accepted with enhanced response
- **Features**: Enhanced validation, batch processing support

#### 2. GET /v2/connect/status/{requestId}
- **Purpose**: Enhanced status with additional metadata
- **Authentication**: Apigee Gateway
- **Path Parameter**: requestId (String)
- **Response**: Enhanced ConnectResponseDto with metadata
- **Features**: Detailed processing information, audit trail

#### 3. GET /v2/connect/health
- **Purpose**: Enhanced health check with detailed information
- **Authentication**: None (public endpoint)
- **Response**: Detailed health status with component information
- **Features**: Database connectivity, external service status

#### 4. GET /v2/connect/version
- **Purpose**: Get API version information and features
- **Authentication**: None (public endpoint)
- **Response**: Version details, feature flags, deprecation info
- **Usage**: API discovery and client integration

### Monitoring Endpoints

#### 1. GET /actuator/health
- **Purpose**: Spring Boot actuator health check
- **Authentication**: None (public endpoint)
- **Response**: Detailed health information
- **Usage**: Kubernetes liveness/readiness probes

#### 2. GET /actuator/metrics
- **Purpose**: Application metrics endpoint
- **Authentication**: None (public endpoint)
- **Response**: Prometheus-compatible metrics
- **Usage**: Monitoring and alerting

#### 3. GET /actuator/info
- **Purpose**: Application information
- **Authentication**: None (public endpoint)
- **Response**: Build information, version details
- **Usage**: Deployment verification

#### 4. GET /swagger-ui.html
- **Purpose**: Interactive API documentation
- **Authentication**: None (public endpoint)
- **Response**: Swagger UI interface
- **Usage**: API testing and documentation

#### 5. GET /v3/api-docs
- **Purpose**: OpenAPI specification
- **Authentication**: None (public endpoint)
- **Response**: OpenAPI 3.0 specification
- **Usage**: API client generation

## Request/Response Flow

### 1. Request Processing Flow
```
Client Request → Apigee Gateway → Load Balancer → API Controller → ConnectService
```

### 2. Immediate Response Flow
```
ConnectService → Immediate 202 Response → Client
```

### 3. Background Processing Flow
```
ConnectService → @Async Processing → ProcessorFactory → ProcessingPipeline → External Services
```

### 4. Status Check Flow
```
Client Status Request → API Controller → ConnectService → MongoDB → Response
```

## Authentication & Authorization

### 1. Apigee Gateway Authentication
- **Incoming Requests**: All API requests must go through Apigee Gateway
- **Token Validation**: Apigee validates client tokens
- **Rate Limiting**: Apigee enforces rate limits
- **CORS**: Apigee handles CORS policies

### 2. Internal Authentication
- **OAuth2 Tokens**: For external service calls
- **Vault Integration**: Secure token storage
- **Token Refresh**: Automatic token renewal

## Error Handling

### 1. API Level Errors
- **400 Bad Request**: Invalid request format
- **401 Unauthorized**: Authentication failure
- **404 Not Found**: Request not found
- **500 Internal Server Error**: Processing failure

### 2. Processing Level Errors
- **Retry Logic**: Automatic retry for transient failures
- **Circuit Breaker**: Protection against external service failures
- **Dead Letter Queue**: Failed request handling

### 3. Monitoring & Alerting
- **Error Metrics**: Track error rates and types
- **Alerting**: Proactive error notification
- **Logging**: Comprehensive error logging

## Performance Characteristics

### 1. Response Times
- **API Response**: < 100ms for immediate response
- **Status Check**: < 50ms for status queries
- **Health Check**: < 10ms for health endpoints

### 2. Throughput
- **Concurrent Requests**: Up to 200 concurrent requests
- **Processing Rate**: Up to 1000 requests per minute
- **Queue Capacity**: 500 pending requests

### 3. Scalability
- **Horizontal Scaling**: Multiple service instances
- **Load Balancing**: Automatic traffic distribution
- **Auto-scaling**: Dynamic scaling based on load

This comprehensive API design provides a robust, scalable, and maintainable solution for processing XML/JSON requests with external service integration while maintaining high performance and reliability.
