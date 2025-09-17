# Microservice Integration Guide

This guide covers everything you need to know when integrating the Connect Service as a microservice into another router, gateway, or service mesh.

## Table of Contents

1. [Service Discovery & Registration](#service-discovery--registration)
2. [API Gateway Integration](#api-gateway-integration)
3. [Load Balancing & Scaling](#load-balancing--scaling)
4. [Security & Authentication](#security--authentication)
5. [Monitoring & Observability](#monitoring--observability)
6. [Configuration Management](#configuration-management)
7. [Deployment Strategies](#deployment-strategies)
8. [Service Mesh Integration](#service-mesh-integration)
9. [Circuit Breaker & Resilience](#circuit-breaker--resilience)
10. [API Documentation & Discovery](#api-documentation--discovery)

## Service Discovery & Registration

### 1. Service Registration

The Connect Service needs to register itself with a service registry for discovery by other services.

#### Eureka Integration
```yaml
# application.yml
eureka:
  client:
    service-url:
      defaultZone: http://eureka-server:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${spring.application.instance_id:${random.value}}
    metadata-map:
      version: ${api.versioning.current-version}
      features: enhanced-validation,batch-processing,advanced-monitoring
```

#### Consul Integration
```yaml
# application.yml
spring:
  cloud:
    consul:
      host: consul-server
      port: 8500
      discovery:
        service-name: connect-service
        instance-id: ${spring.application.name}-${random.uuid}
        tags:
          - version=${api.versioning.current-version}
          - environment=${spring.profiles.active}
          - region=${CONSUL_REGION:us-east-1}
```

#### Kubernetes Service Discovery
```yaml
# k8s/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: connect-service
  labels:
    app: connect-service
    version: v2.0
spec:
  selector:
    app: connect-service
  ports:
    - name: http
      port: 8080
      targetPort: 8080
    - name: management
      port: 8081
      targetPort: 8081
  type: ClusterIP
```

### 2. Health Check Endpoints

Ensure health checks are properly configured for load balancers and service discovery.

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  health:
    readiness-state:
      enabled: true
    liveness-state:
      enabled: true
```

## API Gateway Integration

### 1. Kong Gateway Configuration

```yaml
# kong-connect-service.yml
services:
  - name: connect-service-v1
    url: http://connect-service:8080
    routes:
      - name: connect-v1-route
        paths:
          - /api/v1/connect
        strip_path: true
        plugins:
          - name: rate-limiting
            config:
              minute: 100
              hour: 1000
          - name: jwt
            config:
              secret_is_base64: false
              key_claim_name: iss
              claims_to_verify: exp,iat
    tags:
      - version: v1.0

  - name: connect-service-v2
    url: http://connect-service:8080
    routes:
      - name: connect-v2-route
        paths:
          - /api/v2/connect
        strip_path: true
        plugins:
          - name: rate-limiting
            config:
              minute: 200
              hour: 2000
          - name: jwt
            config:
              secret_is_base64: false
              key_claim_name: iss
              claims_to_verify: exp,iat
    tags:
      - version: v2.0
```

### 2. NGINX Configuration

```nginx
# nginx.conf
upstream connect_service {
    least_conn;
    server connect-service-1:8080 max_fails=3 fail_timeout=30s;
    server connect-service-2:8080 max_fails=3 fail_timeout=30s;
    server connect-service-3:8080 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    server_name api.company.com;

    # Rate limiting
    limit_req_zone $binary_remote_addr zone=connect_limit:10m rate=10r/s;

    # v1 API
    location /api/v1/connect {
        limit_req zone=connect_limit burst=20 nodelay;
        
        proxy_pass http://connect_service;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-API-Version "1.0";
        
        # Timeouts
        proxy_connect_timeout 5s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # Health check
        proxy_next_upstream error timeout invalid_header http_500 http_502 http_503 http_504;
    }

    # v2 API
    location /api/v2/connect {
        limit_req zone=connect_limit burst=40 nodelay;
        
        proxy_pass http://connect_service;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-API-Version "2.0";
        
        # Timeouts
        proxy_connect_timeout 5s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # Health check
        proxy_next_upstream error timeout invalid_header http_500 http_502 http_503 http_504;
    }

    # Health check endpoint
    location /health {
        proxy_pass http://connect_service/api/v1/connect/health;
        access_log off;
    }
}
```

### 3. AWS API Gateway Integration

```yaml
# serverless.yml
service: connect-service

provider:
  name: aws
  runtime: java11
  region: us-east-1

functions:
  connectService:
    handler: com.adyanta.connect.ConnectServiceApplication
    timeout: 30
    memorySize: 512
    environment:
      SPRING_PROFILES_ACTIVE: aws
      MONGODB_URI: ${env:MONGODB_URI}
    events:
      - http:
          path: /api/v1/connect/{proxy+}
          method: ANY
          cors: true
      - http:
          path: /api/v2/connect/{proxy+}
          method: ANY
          cors: true

resources:
  Resources:
    ConnectServiceApi:
      Type: AWS::ApiGateway::RestApi
      Properties:
        Name: Connect Service API
        Description: Connect Service API Gateway
        EndpointConfiguration:
          Types:
            - REGIONAL
```

## Load Balancing & Scaling

### 1. Horizontal Pod Autoscaler (Kubernetes)

```yaml
# k8s/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: connect-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: connect-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
        - type: Percent
          value: 100
          periodSeconds: 15
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Percent
          value: 10
          periodSeconds: 60
```

### 2. Docker Compose with Load Balancing

```yaml
# docker-compose.yml
version: '3.8'

services:
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    depends_on:
      - connect-service-1
      - connect-service-2
      - connect-service-3

  connect-service-1:
    build: .
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SERVER_PORT=8080
      - EUREKA_INSTANCE_INSTANCE_ID=connect-service-1
    ports:
      - "8081:8080"

  connect-service-2:
    build: .
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SERVER_PORT=8080
      - EUREKA_INSTANCE_INSTANCE_ID=connect-service-2
    ports:
      - "8082:8080"

  connect-service-3:
    build: .
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SERVER_PORT=8080
      - EUREKA_INSTANCE_INSTANCE_ID=connect-service-3
    ports:
      - "8083:8080"

  eureka:
    image: steeltoeoss/eureka-server
    ports:
      - "8761:8761"
```

## Security & Authentication

### 1. JWT Token Validation

```java
// SecurityConfig.java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .pathMatchers("/v1/connect/**", "/v2/connect/**").authenticated()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtDecoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri("https://your-auth-server/.well-known/jwks.json")
            .build();
    }
}
```

### 2. API Key Authentication

```yaml
# application.yml
security:
  api-keys:
    enabled: true
    header-name: X-API-Key
    valid-keys:
      - ${API_KEY_1:key1}
      - ${API_KEY_2:key2}
      - ${API_KEY_3:key3}
```

### 3. mTLS Configuration

```yaml
# application.yml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    trust-store: classpath:truststore.p12
    trust-store-password: ${SSL_TRUSTSTORE_PASSWORD}
    trust-store-type: PKCS12
    client-auth: need
```

## Monitoring & Observability

### 1. Prometheus Metrics

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      version: ${api.versioning.current-version}
      environment: ${spring.profiles.active}
```

### 2. Distributed Tracing

```yaml
# application.yml
spring:
  sleuth:
    zipkin:
      base-url: http://zipkin:9411
    sampler:
      probability: 1.0
  application:
    name: connect-service
```

### 3. Log Aggregation

```yaml
# logback-spring.xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <logLevel/>
                <loggerName/>
                <message/>
                <mdc/>
                <stackTrace/>
            </providers>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/connect-service.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/connect-service.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <logLevel/>
                <loggerName/>
                <message/>
                <mdc/>
                <stackTrace/>
            </providers>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="STDOUT"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

## Configuration Management

### 1. External Configuration

```yaml
# application-external.yml
spring:
  config:
    import:
      - configserver:http://config-server:8888
  cloud:
    config:
      discovery:
        enabled: true
        service-id: config-server
      fail-fast: true
      retry:
        initial-interval: 1000
        max-attempts: 6
        max-interval: 2000
        multiplier: 1.1
```

### 2. Environment-Specific Configuration

```yaml
# application-dev.yml
spring:
  profiles:
    active: dev
  data:
    mongodb:
      uri: mongodb://localhost:27017/connect-service-dev

external:
  apis:
    xml-to-json:
      base-url: http://localhost:3000/xml-to-json
    fenergo:
      base-url: http://localhost:3001/fenergo

# application-prod.yml
spring:
  profiles:
    active: prod
  data:
    mongodb:
      uri: ${MONGODB_URI}

external:
  apis:
    xml-to-json:
      base-url: ${XML_TO_JSON_API_URL}
    fenergo:
      base-url: ${FENERGO_API_URL}
```

## Service Mesh Integration

### 1. Istio Configuration

```yaml
# istio-virtual-service.yaml
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: connect-service
spec:
  hosts:
    - connect-service
  http:
    - match:
        - uri:
            prefix: /api/v1/connect
      route:
        - destination:
            host: connect-service
            subset: v1
      timeout: 30s
      retries:
        attempts: 3
        perTryTimeout: 10s
    - match:
        - uri:
            prefix: /api/v2/connect
      route:
        - destination:
            host: connect-service
            subset: v2
      timeout: 30s
      retries:
        attempts: 3
        perTryTimeout: 10s
```

### 2. Destination Rules

```yaml
# istio-destination-rule.yaml
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: connect-service
spec:
  host: connect-service
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        http1MaxPendingRequests: 50
        maxRequestsPerConnection: 2
    circuitBreaker:
      consecutiveErrors: 3
      interval: 30s
      baseEjectionTime: 30s
  subsets:
    - name: v1
      labels:
        version: v1.0
    - name: v2
      labels:
        version: v2.0
```

## Circuit Breaker & Resilience

### 1. Resilience4j Configuration

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      connect-service:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 5s
        failureRateThreshold: 50
        eventConsumerBufferSize: 10
  retry:
    instances:
      connect-service:
        maxAttempts: 3
        waitDuration: 1s
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - java.util.concurrent.TimeoutException
          - java.io.IOException
  timelimiter:
    instances:
      connect-service:
        timeoutDuration: 2s
        cancelRunningFuture: true
```

### 2. Circuit Breaker Implementation

```java
@Service
public class ConnectService {
    
    @CircuitBreaker(name = "connect-service", fallbackMethod = "fallbackProcess")
    @TimeLimiter(name = "connect-service")
    @Retry(name = "connect-service")
    public Mono<ConnectResponseDto> processRequest(ConnectRequestDto request) {
        return processRequestInternal(request);
    }
    
    public Mono<ConnectResponseDto> fallbackProcess(ConnectRequestDto request, Exception ex) {
        return Mono.just(ConnectResponseDto.builder()
            .requestId(request.getRequestId())
            .status("CIRCUIT_OPEN")
            .message("Service temporarily unavailable")
            .error(ConnectResponseDto.ErrorDetails.builder()
                .code("CIRCUIT_BREAKER_OPEN")
                .message("Service is experiencing issues")
                .build())
            .build());
    }
}
```

## API Documentation & Discovery

### 1. OpenAPI Discovery

```yaml
# application.yml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    operationsSorter: method
    tagsSorter: alpha
    display-request-duration: true
  packages-to-scan: com.adyanta.connect
  paths-to-match: /v1/connect/**, /v2/connect/**
```

### 2. Service Registry Integration

```java
@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {
    
    @GetMapping("/services")
    public Mono<Map<String, Object>> getServices() {
        return Mono.just(Map.of(
            "connect-service", Map.of(
                "versions", Arrays.asList("1.0", "2.0"),
                "endpoints", Map.of(
                    "v1", "/api/v1/connect",
                    "v2", "/api/v2/connect"
                ),
                "health", "/api/v1/connect/health",
                "docs", "/swagger-ui.html"
            )
        ));
    }
}
```

## Deployment Strategies

### 1. Blue-Green Deployment

```yaml
# k8s/blue-green-deployment.yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: connect-service
spec:
  replicas: 3
  strategy:
    blueGreen:
      activeService: connect-service-active
      previewService: connect-service-preview
      autoPromotionEnabled: false
      scaleDownDelaySeconds: 30
      prePromotionAnalysis:
        templates:
        - templateName: success-rate
        args:
        - name: service-name
          value: connect-service-preview
      postPromotionAnalysis:
        templates:
        - templateName: success-rate
        args:
        - name: service-name
          value: connect-service-active
  selector:
    matchLabels:
      app: connect-service
  template:
    metadata:
      labels:
        app: connect-service
    spec:
      containers:
      - name: connect-service
        image: connect-service:latest
        ports:
        - containerPort: 8080
```

### 2. Canary Deployment

```yaml
# k8s/canary-deployment.yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: connect-service
spec:
  replicas: 5
  strategy:
    canary:
      steps:
      - setWeight: 20
      - pause: {duration: 10m}
      - setWeight: 40
      - pause: {duration: 10m}
      - setWeight: 60
      - pause: {duration: 10m}
      - setWeight: 80
      - pause: {duration: 10m}
      analysis:
        templates:
        - templateName: success-rate
        args:
        - name: service-name
          value: connect-service
      trafficRouting:
        istio:
          virtualService:
            name: connect-service
            routes:
            - primary
```

## Best Practices

### 1. Service Design
- ✅ **Stateless**: No session state stored in the service
- ✅ **Idempotent**: Operations can be safely retried
- ✅ **Fault Tolerant**: Graceful degradation and error handling
- ✅ **Observable**: Comprehensive logging, metrics, and tracing

### 2. API Design
- ✅ **Versioning**: Clear API versioning strategy
- ✅ **Documentation**: Comprehensive OpenAPI specifications
- ✅ **Consistency**: Consistent response formats and error handling
- ✅ **Backward Compatibility**: Maintain compatibility across versions

### 3. Security
- ✅ **Authentication**: JWT token validation
- ✅ **Authorization**: Role-based access control
- ✅ **Encryption**: TLS for all communications
- ✅ **Input Validation**: Comprehensive request validation

### 4. Monitoring
- ✅ **Health Checks**: Liveness and readiness probes
- ✅ **Metrics**: Business and technical metrics
- ✅ **Logging**: Structured logging with correlation IDs
- ✅ **Tracing**: Distributed tracing for request flow

### 5. Deployment
- ✅ **Containerization**: Docker images with proper tagging
- ✅ **Configuration**: External configuration management
- ✅ **Secrets**: Secure secret management
- ✅ **Rolling Updates**: Zero-downtime deployments

## Integration Checklist

### Pre-Integration
- [ ] Service discovery configuration
- [ ] Load balancer configuration
- [ ] Security policies (JWT, mTLS)
- [ ] Monitoring and logging setup
- [ ] Configuration management
- [ ] Health check endpoints

### Integration
- [ ] API gateway routing rules
- [ ] Rate limiting policies
- [ ] Circuit breaker configuration
- [ ] Retry policies
- [ ] Timeout settings
- [ ] CORS configuration

### Post-Integration
- [ ] Load testing
- [ ] Performance monitoring
- [ ] Error rate monitoring
- [ ] Capacity planning
- [ ] Documentation updates
- [ ] Team training

This comprehensive guide should help you successfully integrate the Connect Service as a microservice into your router/gateway infrastructure!
