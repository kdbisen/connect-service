# Connect Service

A robust Spring Boot microservice for processing XML/JSON requests with external API integration, built with modern design patterns and comprehensive monitoring.

## 🏗️ Architecture

The Connect Service follows a microservices architecture with the following key components:

- **Fire-and-Forget Processing**: Asynchronous request processing with immediate response
- **Strategy Pattern**: Extensible request processing based on request type
- **Chain of Responsibility**: Modular processing pipeline with individual steps
- **Factory Pattern**: Dynamic processor selection based on request type
- **Observer Pattern**: Event-driven processing with comprehensive audit logging

## 🚀 Features

### Core Functionality
- **Multi-format Support**: Handles both XML and JSON payloads
- **Request Type Validation**: ENUM-based request type validation
- **External API Integration**: XML-to-JSON conversion and Fenergo API calls
- **Fire-and-Forget Processing**: Asynchronous processing with status tracking
- **Comprehensive Auditing**: Full audit trail of all processing steps

### Security
- **Apigee Authentication**: Client credential validation via Apigee
- **OAuth2 Integration**: Secure token management for external APIs
- **CORS Support**: Configurable cross-origin resource sharing

### Monitoring & Observability
- **Structured Logging**: JSON-formatted logs with correlation IDs
- **Metrics**: Prometheus metrics for monitoring
- **Health Checks**: Comprehensive health check endpoints
- **Distributed Tracing**: Request tracing across service boundaries

### Data Persistence
- **MongoDB Integration**: Reactive MongoDB for data storage
- **Audit Logging**: Complete processing history
- **Retry Mechanism**: Configurable retry logic for failed requests

## 🛠️ Technology Stack

- **Spring Boot 3.2.0** - Application framework
- **Spring WebFlux** - Reactive web framework
- **Spring Security** - Authentication and authorization
- **Spring Integration** - Message processing pipeline
- **MongoDB** - Document database
- **Reactor** - Reactive programming
- **TestContainers** - Integration testing
- **Docker** - Containerization
- **Prometheus** - Metrics collection
- **Grafana** - Monitoring dashboards

## 📋 Prerequisites

- Java 17+
- Maven 3.9+
- MongoDB 7.0+
- Docker & Docker Compose (optional)

## 🚀 Quick Start

### Using Docker Compose (Recommended)

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd connect-service
   ```

2. **Set environment variables**
   ```bash
   export APIGEE_CLIENT_ID=your-apigee-client-id
   export APIGEE_CLIENT_SECRET=your-apigee-client-secret
   export APIGEE_TOKEN_URI=https://api.apigee.com/oauth/token
   export FENERGO_CLIENT_ID=your-fenergo-client-id
   export FENERGO_CLIENT_SECRET=your-fenergo-client-secret
   export FENERGO_TOKEN_URI=https://api.fenergo.com/oauth/token
   export XML_TO_JSON_API_URL=https://api.example.com/xml-to-json
   export FENERGO_API_URL=https://api.fenergo.com/v1
   ```

3. **Start the services**
   ```bash
   docker-compose up -d
   ```

4. **Verify deployment**
   ```bash
   curl http://localhost:8080/api/v1/connect/health
   ```

### Manual Setup

1. **Start MongoDB**
   ```bash
   docker run -d -p 27017:27017 --name mongodb mongo:7.0
   ```

2. **Build and run the application**
   ```bash
   mvn clean package
   java -jar target/connect-service-1.0.0.jar
   ```

## 📚 API Documentation

### Endpoints

#### POST /api/v1/connect/process
Process a connect request (fire-and-forget)

**Request Headers:**
```
Content-Type: application/json
X-Client-Id: your-client-id
X-Client-Secret: your-client-secret
X-API-Key: your-api-key (optional)
```

**Request Body:**
```json
{
  "requestType": "CUSTOMER_ONBOARDING",
  "payload": "<xml>your data</xml>",
  "contentType": "application/xml",
  "clientId": "your-client-id",
  "requestId": "unique-request-id",
  "priority": 5,
  "timeoutMs": 300000,
  "metadata": {
    "customField": "value"
  }
}
```

**Response:**
```json
{
  "requestId": "unique-request-id",
  "status": "ACCEPTED",
  "message": "Request accepted for processing",
  "timestamp": "2024-01-01T12:00:00",
  "processingId": "processing-123"
}
```

#### GET /api/v1/connect/status/{requestId}
Get request processing status

**Response:**
```json
{
  "requestId": "unique-request-id",
  "status": "COMPLETED",
  "message": "Processing completed successfully",
  "timestamp": "2024-01-01T12:05:00",
  "processingId": "processing-123",
  "data": {
    "originalPayload": "<xml>your data</xml>",
    "convertedJsonPayload": "{\"data\": \"value\"}",
    "externalApiResponse": "{\"result\": \"success\"}",
    "fenergoResponse": "{\"fenergo\": \"response\"}",
    "retryCount": 0,
    "createdAt": "2024-01-01T12:00:00",
    "updatedAt": "2024-01-01T12:05:00"
  }
}
```

### Request Types

The service supports the following request types:

- `CUSTOMER_ONBOARDING` - Customer onboarding process
- `KYC_VERIFICATION` - KYC verification process
- `DOCUMENT_PROCESSING` - Document processing
- `COMPLIANCE_CHECK` - Compliance check
- `RISK_ASSESSMENT` - Risk assessment
- `TRANSACTION_MONITORING` - Transaction monitoring
- `GENERIC_PROCESSING` - Generic data processing

## 🔧 Configuration

### Application Properties

```yaml
server:
  port: 8080
  servlet:
    context-path: /api/v1

spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/connect-service
  
  security:
    oauth2:
      client:
        registration:
          apigee:
            client-id: ${APIGEE_CLIENT_ID}
            client-secret: ${APIGEE_CLIENT_SECRET}
          fenergo:
            client-id: ${FENERGO_CLIENT_ID}
            client-secret: ${FENERGO_CLIENT_SECRET}

external:
  apis:
    xml-to-json:
      base-url: ${XML_TO_JSON_API_URL}
      timeout: 30000
    fenergo:
      base-url: ${FENERGO_API_URL}
      timeout: 60000

processing:
  async:
    core-pool-size: 10
    max-pool-size: 50
    queue-capacity: 1000
```

## 🧪 Testing

### Run Unit Tests
```bash
mvn test
```

### Run Integration Tests
```bash
mvn verify
```

### Run All Tests
```bash
mvn clean verify
```

## 📊 Monitoring

### Health Check
```bash
curl http://localhost:8080/api/v1/connect/health
```

### Metrics
```bash
curl http://localhost:8080/actuator/metrics
```

### Prometheus Metrics
```bash
curl http://localhost:8080/actuator/prometheus
```

### Grafana Dashboard
Access Grafana at `http://localhost:3000` (admin/admin)

## 🔍 Logging

The service uses structured JSON logging with the following format:

```json
{
  "timestamp": "2024-01-01T12:00:00.000Z",
  "level": "INFO",
  "service": "connect-service",
  "traceId": "abc123",
  "spanId": "def456",
  "thread": "reactor-http-nio-1",
  "logger": "com.adyanta.connect.service.ConnectService",
  "message": "Request processing initiated"
}
```

## 🚀 Deployment

### Docker
```bash
docker build -t connect-service .
docker run -p 8080:8080 connect-service
```

### Kubernetes
```bash
kubectl apply -f k8s/
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For support and questions:
- Create an issue in the repository
- Contact the development team
- Check the documentation

## 🔄 Changelog

### Version 1.0.0
- Initial release
- Fire-and-forget processing
- XML/JSON conversion
- External API integration
- Comprehensive monitoring
- MongoDB persistence
- Security integration

