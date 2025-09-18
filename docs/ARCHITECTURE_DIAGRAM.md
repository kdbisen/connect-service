# Connect Service Architecture Diagram

## Overview
This document provides a comprehensive architecture diagram showing all service endpoints, internal processing flow, and how the factory and pipeline patterns work in the Connect Service.

## Mermaid Diagram

```mermaid
graph TB
    %% External Clients
    Client[Client Applications]
    Apigee[Apigee Gateway]
    
    %% Load Balancer
    LB[Load Balancer/NGINX]
    
    %% Connect Service
    subgraph "Connect Service (Spring Boot)"
        %% API Layer
        subgraph "API Layer"
            V1Controller[ConnectController v1.0<br/>/v1/connect/*]
            V2Controller[ConnectControllerV2 v2.0<br/>/v2/connect/*]
            HealthController[Health Endpoints<br/>/health, /actuator/*]
        end
        
        %% Security Layer
        subgraph "Security Layer"
            ApigeeFilter[ApigeeAuthenticationFilter]
            SecurityConfig[SecurityConfig]
            OAuth2Service[OAuth2TokenService]
        end
        
        %% Service Layer
        subgraph "Service Layer"
            ConnectService[ConnectService<br/>@Async Processing]
            AuditService[AuditService]
            VaultService[VaultSecretService]
            MultiOAuth2Manager[MultiOAuth2TokenManager]
            ConnectorRegistry[ConnectorRegistry]
            RouteExecutor[RouteExecutor]
        end
        
        %% Processing Layer
        subgraph "Processing Layer"
            ProcessorFactory[RequestProcessorFactory<br/>Strategy Pattern]
            
            subgraph "Request Processors"
                GenericProcessor[GenericRequestProcessor]
                CustomerProcessor[CustomerOnboardingProcessor]
                FutureProcessor[FutureRequestProcessor]
            end
            
            subgraph "Processing Pipeline"
                ProcessingPipeline[ProcessingPipeline<br/>Chain of Responsibility]
                
                subgraph "Processing Steps"
                    XmlToJsonStep[XmlToJsonConversionStep]
                    ExternalApiStep[ExternalApiCallStep]
                    FenergoStep[FenergoApiCallStep]
                    CompletionStep[CompletionStep]
                end
            end
        end
        
        %% Configuration Layer
        subgraph "Configuration Layer"
            AsyncConfig[AsyncConfig<br/>Thread Pools]
            MongoConfig[AdvancedMongoConfig<br/>SSL + Vault]
            WebClientConfig[WebClientConfig]
            ApiVersionConfig[ApiVersionConfig]
        end
        
        %% Data Layer
        subgraph "Data Layer"
            ProcessingRepo[ProcessingRequestRepository]
            AuditRepo[ProcessingAuditLogRepository]
            MongoDB[(MongoDB<br/>SSL Enabled)]
        end
    end
    
    %% External Services
    subgraph "External Services"
        XmlToJsonService[XML-to-JSON Service<br/>Apigee Gateway]
        FenergoService[Fenergo API<br/>OAuth2 Authentication]
        OtherServices[Other External APIs<br/>Multi-Connector Support]
    end
    
    %% Infrastructure
    subgraph "Infrastructure"
        Vault[HashiCorp Vault<br/>Secrets Management]
        Prometheus[Prometheus<br/>Metrics Collection]
        Grafana[Grafana<br/>Monitoring Dashboard]
        Consul[Consul<br/>Service Discovery]
    end
    
    %% Flow Connections
    Client --> Apigee
    Apigee --> LB
    LB --> V1Controller
    LB --> V2Controller
    LB --> HealthController
    
    %% Security Flow
    V1Controller --> ApigeeFilter
    V2Controller --> ApigeeFilter
    ApigeeFilter --> SecurityConfig
    SecurityConfig --> OAuth2Service
    
    %% Service Flow
    V1Controller --> ConnectService
    V2Controller --> ConnectService
    ConnectService --> AuditService
    ConnectService --> ProcessorFactory
    
    %% Processing Flow
    ProcessorFactory --> GenericProcessor
    ProcessorFactory --> CustomerProcessor
    ProcessorFactory --> FutureProcessor
    
    GenericProcessor --> ProcessingPipeline
    CustomerProcessor --> ProcessingPipeline
    FutureProcessor --> ProcessingPipeline
    
    ProcessingPipeline --> XmlToJsonStep
    XmlToJsonStep --> ExternalApiStep
    ExternalApiStep --> FenergoStep
    FenergoStep --> CompletionStep
    
    %% External API Calls
    XmlToJsonStep --> XmlToJsonService
    ExternalApiStep --> OtherServices
    FenergoStep --> FenergoService
    
    %% Data Flow
    ConnectService --> ProcessingRepo
    AuditService --> AuditRepo
    ProcessingRepo --> MongoDB
    AuditRepo --> MongoDB
    
    %% Configuration Dependencies
    ConnectService --> AsyncConfig
    ConnectService --> MongoConfig
    ConnectService --> WebClientConfig
    ConnectService --> ApiVersionConfig
    
    %% External Service Management
    MultiOAuth2Manager --> OAuth2Service
    ConnectorRegistry --> MultiOAuth2Manager
    RouteExecutor --> ConnectorRegistry
    
    %% Infrastructure Dependencies
    MongoConfig --> Vault
    OAuth2Service --> Vault
    ConnectService --> Prometheus
    Prometheus --> Grafana
    
    %% Service Discovery
    LB --> Consul
    ConnectService --> Consul
    
    %% Styling
    classDef apiLayer fill:#e1f5fe
    classDef serviceLayer fill:#f3e5f5
    classDef processingLayer fill:#e8f5e8
    classDef dataLayer fill:#fff3e0
    classDef external fill:#ffebee
    classDef infrastructure fill:#f1f8e9
    
    class V1Controller,V2Controller,HealthController apiLayer
    class ConnectService,AuditService,VaultService,MultiOAuth2Manager,ConnectorRegistry,RouteExecutor serviceLayer
    class ProcessorFactory,GenericProcessor,CustomerProcessor,FutureProcessor,ProcessingPipeline,XmlToJsonStep,ExternalApiStep,FenergoStep,CompletionStep processingLayer
    class ProcessingRepo,AuditRepo,MongoDB dataLayer
    class XmlToJsonService,FenergoService,OtherServices external
    class Vault,Prometheus,Grafana,Consul infrastructure
```

## Detailed Component Descriptions

### 1. API Layer
- **ConnectController v1.0**: Handles `/v1/connect/*` endpoints
- **ConnectControllerV2 v2.0**: Handles `/v2/connect/*` endpoints with enhanced features
- **Health Endpoints**: Provides health checks and monitoring endpoints

### 2. Security Layer
- **ApigeeAuthenticationFilter**: Validates incoming requests via Apigee Gateway
- **SecurityConfig**: Configures security policies and CORS
- **OAuth2TokenService**: Manages OAuth2 tokens for external service calls

### 3. Service Layer
- **ConnectService**: Main orchestrator with `@Async` processing
- **AuditService**: Handles audit logging and tracking
- **VaultSecretService**: Retrieves secrets from HashiCorp Vault
- **MultiOAuth2TokenManager**: Manages OAuth2 tokens for multiple providers
- **ConnectorRegistry**: Registry for external service connectors
- **RouteExecutor**: Executes multi-step processing routes

### 4. Processing Layer
- **RequestProcessorFactory**: Factory pattern for creating request processors
- **Request Processors**: Strategy pattern implementations for different request types
- **ProcessingPipeline**: Chain of Responsibility pattern for processing steps
- **Processing Steps**: Individual processing components in the pipeline

### 5. Configuration Layer
- **AsyncConfig**: Thread pool configuration for `@Async` methods
- **AdvancedMongoConfig**: MongoDB SSL configuration with Vault integration
- **WebClientConfig**: HTTP client configuration
- **ApiVersionConfig**: API versioning configuration

### 6. Data Layer
- **ProcessingRequestRepository**: MongoDB repository for processing requests
- **ProcessingAuditLogRepository**: MongoDB repository for audit logs
- **MongoDB**: Document database with SSL encryption

## Request Processing Flow

### 1. Incoming Request Flow
```
Client → Apigee Gateway → Load Balancer → Connect Controller
```

### 2. Authentication Flow
```
Request → ApigeeAuthenticationFilter → SecurityConfig → OAuth2Service
```

### 3. Processing Flow
```
ConnectService → RequestProcessorFactory → Request Processor → ProcessingPipeline → Processing Steps
```

### 4. External Service Calls
```
Processing Steps → External APIs (XML-to-JSON, Fenergo, Others)
```

### 5. Data Persistence
```
Processing Steps → AuditService → MongoDB (via Repositories)
```

## Design Patterns Used

### 1. Strategy Pattern
- **RequestProcessorFactory**: Creates appropriate processor based on request type
- **Request Processors**: Different strategies for different request types

### 2. Chain of Responsibility Pattern
- **ProcessingPipeline**: Chains processing steps together
- **Processing Steps**: Each step handles a specific responsibility

### 3. Factory Pattern
- **RequestProcessorFactory**: Creates request processors
- **ConnectorRegistry**: Creates connector instances

### 4. Observer Pattern
- **AuditService**: Observes processing events and logs them

### 5. Template Method Pattern
- **Processing Steps**: Common structure with specific implementations

## Thread Pool Configuration

### 1. Task Executor
- **Core Pool Size**: 10 threads
- **Max Pool Size**: 50 threads
- **Queue Capacity**: 1000 tasks
- **Usage**: General async operations

### 2. Processing Executor
- **Core Pool Size**: 5 threads
- **Max Pool Size**: 20 threads
- **Queue Capacity**: 500 tasks
- **Usage**: Request processing

### 3. External API Executor
- **Core Pool Size**: 3 threads
- **Max Pool Size**: 15 threads
- **Queue Capacity**: 200 tasks
- **Usage**: External API calls

## API Endpoints

### Version 1.0 Endpoints
- `POST /v1/connect/process` - Process XML/JSON requests
- `GET /v1/connect/status/{requestId}` - Get processing status
- `GET /v1/connect/health` - Health check

### Version 2.0 Endpoints
- `POST /v2/connect/process` - Enhanced processing with validation
- `GET /v2/connect/status/{requestId}` - Enhanced status with metadata
- `GET /v2/connect/health` - Enhanced health check
- `GET /v2/connect/version` - API version information

## Monitoring and Observability

### 1. Metrics Collection
- **Prometheus**: Collects application metrics
- **Grafana**: Visualizes metrics and dashboards
- **Micrometer**: Provides metrics to Prometheus

### 2. Logging
- **Logback**: Structured logging with correlation IDs
- **Audit Logs**: Detailed processing audit trails
- **Error Tracking**: Comprehensive error logging

### 3. Health Checks
- **Application Health**: Service availability
- **Database Health**: MongoDB connectivity
- **External Service Health**: External API availability

## Security Features

### 1. Authentication
- **Apigee Gateway**: Incoming request authentication
- **OAuth2**: External service authentication
- **JWT Tokens**: Token-based authentication

### 2. Authorization
- **Role-based Access**: Different access levels
- **API Versioning**: Version-specific access control
- **Rate Limiting**: Request rate limiting

### 3. Data Protection
- **SSL/TLS**: Encrypted data transmission
- **Vault Integration**: Secure secret management
- **Data Encryption**: Sensitive data encryption

## Scalability Features

### 1. Horizontal Scaling
- **Load Balancer**: Distributes traffic across instances
- **Service Discovery**: Dynamic service registration
- **Auto-scaling**: Automatic scaling based on load

### 2. Vertical Scaling
- **Thread Pools**: Configurable thread pool sizes
- **Memory Management**: Efficient memory usage
- **CPU Optimization**: CPU-intensive task optimization

### 3. Resilience
- **Circuit Breakers**: External service failure handling
- **Retry Logic**: Automatic retry for failed operations
- **Timeout Handling**: Request timeout management

This architecture provides a robust, scalable, and maintainable solution for processing XML/JSON requests with external service integration while maintaining high performance and reliability.
