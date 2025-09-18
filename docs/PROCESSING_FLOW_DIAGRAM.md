# Connect Service Processing Flow Diagram

## Processing Flow Overview

This diagram shows the detailed processing flow from API request to completion, including how the factory and pipeline patterns work.

## Mermaid Diagram

```mermaid
flowchart TD
    %% API Entry Points
    Start([Client Request]) --> Apigee[Apigee Gateway<br/>Authentication]
    Apigee --> LB[Load Balancer]
    
    %% API Controllers
    LB --> V1API[POST /v1/connect/process]
    LB --> V2API[POST /v2/connect/process]
    
    %% Controller Processing
    V1API --> Controller1[ConnectController<br/>processRequest()]
    V2API --> Controller2[ConnectControllerV2<br/>processRequestV2()]
    
    %% Service Layer
    Controller1 --> ConnectService[ConnectService<br/>processRequestAsync()]
    Controller2 --> ConnectService
    
    %% Immediate Response
    ConnectService --> ImmediateResponse[Immediate Response<br/>202 Accepted]
    ImmediateResponse --> Client[Client Receives Response]
    
    %% Async Processing
    ConnectService --> AsyncProcessing[@Async Processing<br/>processAsync()]
    
    %% Factory Pattern
    AsyncProcessing --> Factory[RequestProcessorFactory<br/>getProcessor()]
    
    %% Strategy Pattern - Request Processors
    Factory --> Strategy{Request Type?}
    Strategy -->|GENERIC| GenericProcessor[GenericRequestProcessor]
    Strategy -->|CUSTOMER_ONBOARDING| CustomerProcessor[CustomerOnboardingProcessor]
    Strategy -->|FUTURE_TYPE| FutureProcessor[FutureRequestProcessor]
    
    %% Pipeline Pattern
    GenericProcessor --> Pipeline[ProcessingPipeline<br/>Chain of Responsibility]
    CustomerProcessor --> Pipeline
    FutureProcessor --> Pipeline
    
    %% Pipeline Steps
    Pipeline --> Step1[1. XmlToJsonConversionStep<br/>Convert XML to JSON]
    Step1 --> Step2[2. ExternalApiCallStep<br/>Call XML-to-JSON Service]
    Step2 --> Step3[3. FenergoApiCallStep<br/>Call Fenergo API]
    Step3 --> Step4[4. CompletionStep<br/>Finalize Processing]
    
    %% External Service Calls
    Step1 --> External1[XML-to-JSON Service<br/>via Apigee Gateway]
    Step2 --> External2[External APIs<br/>via OAuth2]
    Step3 --> External3[Fenergo API<br/>via OAuth2]
    
    %% Data Persistence
    Step1 --> Audit1[AuditService<br/>Log Step 1]
    Step2 --> Audit2[AuditService<br/>Log Step 2]
    Step3 --> Audit3[AuditService<br/>Log Step 3]
    Step4 --> Audit4[AuditService<br/>Log Step 4]
    
    %% Database Updates
    Audit1 --> MongoDB1[(MongoDB<br/>ProcessingRequest)]
    Audit2 --> MongoDB2[(MongoDB<br/>ProcessingRequest)]
    Audit3 --> MongoDB3[(MongoDB<br/>ProcessingRequest)]
    Audit4 --> MongoDB4[(MongoDB<br/>ProcessingRequest)]
    
    %% Final Status Update
    Step4 --> FinalStatus[Update Status<br/>COMPLETED/FAILED]
    FinalStatus --> MongoDB5[(MongoDB<br/>Final Status)]
    
    %% Error Handling
    Step1 --> Error1[Error Handling<br/>Retry Logic]
    Step2 --> Error2[Error Handling<br/>Retry Logic]
    Step3 --> Error3[Error Handling<br/>Retry Logic]
    Step4 --> Error4[Error Handling<br/>Retry Logic]
    
    Error1 --> ErrorStatus[Update Status<br/>FAILED]
    Error2 --> ErrorStatus
    Error3 --> ErrorStatus
    Error4 --> ErrorStatus
    ErrorStatus --> MongoDB6[(MongoDB<br/>Error Status)]
    
    %% Styling
    classDef apiLayer fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    classDef serviceLayer fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef processingLayer fill:#e8f5e8,stroke:#1b5e20,stroke-width:2px
    classDef externalLayer fill:#ffebee,stroke:#b71c1c,stroke-width:2px
    classDef dataLayer fill:#fff3e0,stroke:#e65100,stroke-width:2px
    classDef errorLayer fill:#ffcdd2,stroke:#d32f2f,stroke-width:2px
    
    class V1API,V2API,Controller1,Controller2,ImmediateResponse apiLayer
    class ConnectService,AsyncProcessing,Factory,Strategy serviceLayer
    class GenericProcessor,CustomerProcessor,FutureProcessor,Pipeline,Step1,Step2,Step3,Step4 processingLayer
    class External1,External2,External3 externalLayer
    class Audit1,Audit2,Audit3,Audit4,MongoDB1,MongoDB2,MongoDB3,MongoDB4,MongoDB5,MongoDB6 dataLayer
    class Error1,Error2,Error3,Error4,ErrorStatus,MongoDB6 errorLayer
```

## Detailed Processing Steps

### 1. Request Reception
```
Client Request → Apigee Gateway → Load Balancer → API Controller
```

### 2. Immediate Response (Fire-and-Forget)
```
Controller → ConnectService.processRequestAsync() → Immediate 202 Response
```

### 3. Async Processing Initiation
```
@Async("processingExecutor") → processAsync() → RequestProcessorFactory
```

### 4. Factory Pattern - Processor Selection
```java
// RequestProcessorFactory.getProcessor()
switch (requestType) {
    case GENERIC -> new GenericRequestProcessor();
    case CUSTOMER_ONBOARDING -> new CustomerOnboardingProcessor();
    case FUTURE_TYPE -> new FutureRequestProcessor();
}
```

### 5. Pipeline Pattern - Step Execution
```java
// ProcessingPipeline.execute()
public ProcessingRequest process(ProcessingRequest request) {
    return steps.stream()
        .reduce(request, (req, step) -> step.execute(req), (a, b) -> b);
}
```

### 6. Individual Step Processing
```java
// Each step follows the same pattern
public ProcessingRequest execute(ProcessingRequest request) {
    try {
        // 1. Validate input
        // 2. Process data
        // 3. Call external service
        // 4. Update request with result
        // 5. Log audit trail
        return updatedRequest;
    } catch (Exception e) {
        // Handle error and update status
        return errorRequest;
    }
}
```

## Design Patterns Implementation

### 1. Strategy Pattern
```java
public interface RequestProcessor {
    ProcessingRequest process(ProcessingRequest request);
}

public class RequestProcessorFactory {
    public RequestProcessor getProcessor(RequestType type) {
        return processors.get(type);
    }
}
```

### 2. Chain of Responsibility Pattern
```java
public interface ProcessingStep {
    ProcessingRequest execute(ProcessingRequest request);
}

public class ProcessingPipeline {
    private List<ProcessingStep> steps;
    
    public ProcessingRequest process(ProcessingRequest request) {
        return steps.stream()
            .reduce(request, (req, step) -> step.execute(req));
    }
}
```

### 3. Factory Pattern
```java
@Component
public class RequestProcessorFactory {
    private final Map<RequestType, RequestProcessor> processors;
    
    public RequestProcessor getProcessor(RequestType type) {
        return processors.getOrDefault(type, defaultProcessor);
    }
}
```

### 4. Observer Pattern
```java
@Service
public class AuditService {
    public void logStep(String requestId, String stepName, 
                       ProcessingStatus status, String details) {
        // Log processing step
    }
}
```

## Thread Pool Management

### 1. Processing Executor
```java
@Bean(name = "processingExecutor")
public Executor processingExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(20);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("ConnectService-Processing-");
    return executor;
}
```

### 2. Async Method Execution
```java
@Async("processingExecutor")
public void processAsync(ProcessingRequest request) {
    // Processing logic runs in separate thread
}
```

## Error Handling Flow

### 1. Step-Level Error Handling
```java
public ProcessingRequest execute(ProcessingRequest request) {
    try {
        // Process step
        return successRequest;
    } catch (Exception e) {
        // Log error
        auditService.logError(request.getRequestId(), stepName, e);
        
        // Update request status
        request.setStatus(ProcessingStatus.FAILED);
        request.setErrorMessage(e.getMessage());
        return request;
    }
}
```

### 2. Pipeline-Level Error Handling
```java
public ProcessingRequest process(ProcessingRequest request) {
    for (ProcessingStep step : steps) {
        try {
            request = step.execute(request);
            if (request.getStatus() == ProcessingStatus.FAILED) {
                break; // Stop processing on failure
            }
        } catch (Exception e) {
            // Handle pipeline error
            break;
        }
    }
    return request;
}
```

## Data Flow

### 1. Request Data
```java
ProcessingRequest {
    String requestId;
    RequestType requestType;
    String originalPayload;        // XML input
    String convertedJsonPayload;   // After XML-to-JSON conversion
    String externalApiResponse;    // Response from external API
    String fenergoResponse;        // Response from Fenergo
    ProcessingStatus status;
    String errorMessage;
    // ... other fields
}
```

### 2. Audit Data
```java
ProcessingAuditLog {
    String processingRequestId;
    String stepName;
    ProcessingStatus status;
    String inputData;
    String outputData;
    String errorMessage;
    LocalDateTime createdAt;
}
```

## Monitoring and Observability

### 1. Processing Metrics
- **Request Count**: Total requests processed
- **Success Rate**: Percentage of successful requests
- **Processing Time**: Time taken for each step
- **Error Rate**: Percentage of failed requests

### 2. Thread Pool Metrics
- **Active Threads**: Currently processing threads
- **Queue Size**: Pending requests in queue
- **Completed Tasks**: Total completed tasks

### 3. External Service Metrics
- **Response Time**: Time taken by external services
- **Error Rate**: External service error rate
- **Retry Count**: Number of retries attempted

This architecture provides a robust, scalable, and maintainable solution for processing XML/JSON requests with external service integration while maintaining high performance and reliability.
