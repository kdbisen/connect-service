# Connect Service - Simple Flow

## What This Service Does
Takes XML/JSON requests, processes them through external services, and saves the results.

## Simple Flow Diagram

```mermaid
flowchart LR
    %% Client Request
    A[Client Sends<br/>XML/JSON Request] --> B[Apigee Gateway<br/>Authentication]
    
    %% Immediate Response
    B --> C[Connect Service<br/>Receives Request]
    C --> D[Immediate Response<br/>202 Accepted + Request ID]
    D --> A
    
    %% Background Processing
    C --> E[Background Processing<br/>@Async]
    
    %% Processing Steps
    E --> F[Step 1:<br/>XML → JSON]
    F --> G[Step 2:<br/>Call External API]
    G --> H[Step 3:<br/>Call Fenergo API]
    H --> I[Step 4:<br/>Save to Database]
    
    %% External Services
    F --> J[XML-to-JSON<br/>Service]
    G --> K[External<br/>APIs]
    H --> L[Fenergo<br/>API]
    
    %% Database
    I --> M[(MongoDB<br/>Database)]
    
    %% Status Check
    A --> N[Check Status<br/>GET /status/{id}]
    N --> C
    C --> M
    M --> O[Status Response<br/>COMPLETED/FAILED]
    O --> A
    
    %% Styling
    classDef client fill:#e3f2fd,stroke:#1976d2,stroke-width:2px
    classDef service fill:#e8f5e8,stroke:#388e3c,stroke-width:2px
    classDef processing fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    classDef external fill:#ffebee,stroke:#d32f2f,stroke-width:2px
    classDef database fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    
    class A,D,O client
    class B,C,N service
    class E,F,G,H,I processing
    class J,K,L external
    class M database
```

## Step-by-Step Process

### 1. **Client Sends Request**
```
POST /v1/connect/process
{
  "requestType": "CUSTOMER_ONBOARDING",
  "payload": "<customer>...</customer>",
  "clientId": "client123"
}
```

### 2. **Immediate Response**
```
HTTP 202 Accepted
{
  "requestId": "req-123456",
  "status": "ACCEPTED",
  "message": "Request accepted for processing"
}
```

### 3. **Background Processing**
```
Step 1: Convert XML to JSON
Step 2: Call External API
Step 3: Call Fenergo API
Step 4: Save Results
```

### 4. **Check Status**
```
GET /v1/connect/status/req-123456
{
  "requestId": "req-123456",
  "status": "COMPLETED",
  "message": "Processing completed successfully"
}
```

## API Endpoints (Simple)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/v1/connect/process` | Process request |
| GET | `/v1/connect/status/{id}` | Check status |
| GET | `/v1/connect/health` | Health check |
| POST | `/v2/connect/process` | Process request (v2) |
| GET | `/v2/connect/status/{id}` | Check status (v2) |
| GET | `/v2/connect/health` | Health check (v2) |

## Key Concepts

### **Fire-and-Forget**
- Client gets immediate response
- Processing happens in background
- No waiting for completion

### **Async Processing**
- Uses `@Async` annotation
- Runs in separate thread
- Handles errors automatically

### **Factory Pattern**
- Different processors for different request types
- Easy to add new types

### **Chain of Responsibility**
- Steps are chained together
- Each step does one thing
- Easy to modify

## Error Handling

### **If Something Fails**
1. Log the error
2. Update status to FAILED
3. Save error message
4. Client can check status

### **Retry Logic**
- Automatic retry for temporary failures
- Maximum 3 attempts
- Wait between retries

## Monitoring

### **Health Check**
```
GET /v1/connect/health
{
  "status": "UP",
  "message": "Service is running"
}
```

### **Metrics**
- How many requests processed
- Success rate
- Average processing time
- Error count

## Security

### **Authentication**
- Apigee Gateway validates clients
- OAuth2 for external services
- Vault stores secrets securely

### **Data Protection**
- All data encrypted
- Secure database connections
- No sensitive data in logs

## That's It! 🎉

The Connect Service is simple:
1. **Receive** request
2. **Respond** immediately  
3. **Process** in background
4. **Save** results
5. **Check** status anytime

**Easy to understand, easy to use, easy to maintain!**
