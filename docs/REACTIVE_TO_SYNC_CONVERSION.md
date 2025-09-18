# Reactive to Synchronous Conversion Guide

This document explains the conversion of the Connect Service from reactive (WebFlux) to synchronous Spring Boot with `@Async` for internal asynchronous processing.

## Overview

The Connect Service has been converted from a reactive architecture using Spring WebFlux to a traditional synchronous Spring Boot application that uses `@Async` for internal asynchronous processing. This makes the codebase simpler and easier to understand while maintaining the ability to handle asynchronous operations where needed.

## Key Changes

### 1. Dependencies Updated

#### Removed Reactive Dependencies
- `spring-boot-starter-webflux` → `spring-boot-starter-web`
- `spring-boot-starter-data-mongodb-reactive` → `spring-boot-starter-data-mongodb`
- `spring-integration-webflux` → `spring-integration-web`
- `springdoc-openapi-starter-webflux-ui` → `springdoc-openapi-starter-webmvc-ui`

#### Added Synchronous Dependencies
- `spring-boot-starter-web` for traditional MVC
- `spring-boot-starter-data-mongodb` for synchronous MongoDB operations
- `spring-integration-web` for web integration
- `springdoc-openapi-starter-webmvc-ui` for Swagger UI

### 2. Application Configuration

#### Main Application Class
```java
// Before (Reactive)
@EnableReactiveMongoAuditing

// After (Synchronous)
@EnableMongoAuditing
```

#### Async Configuration
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        // Thread pool configuration for general async operations
    }
    
    @Bean(name = "processingExecutor")
    public Executor processingExecutor() {
        // Thread pool configuration for request processing
    }
    
    @Bean(name = "externalApiExecutor")
    public Executor externalApiExecutor() {
        // Thread pool configuration for external API calls
    }
}
```

### 3. Repository Layer Changes

#### Before (Reactive)
```java
public interface ProcessingRequestRepository extends ReactiveMongoRepository<ProcessingRequest, String> {
    Mono<ProcessingRequest> findByRequestId(String requestId);
    Flux<ProcessingRequest> findByStatus(ProcessingStatus status);
    Mono<Long> countByStatus(ProcessingStatus status);
}
```

#### After (Synchronous)
```java
public interface ProcessingRequestRepository extends MongoRepository<ProcessingRequest, String> {
    ProcessingRequest findByRequestId(String requestId);
    List<ProcessingRequest> findByStatus(ProcessingStatus status);
    long countByStatus(ProcessingStatus status);
}
```

### 4. Service Layer Changes

#### Before (Reactive)
```java
public Mono<ConnectResponseDto> processRequest(ConnectRequestDto requestDto) {
    return requestRepository.save(processingRequest)
            .flatMap(savedRequest -> {
                processAsync(savedRequest);
                return Mono.just(response);
            });
}

private void processAsync(ProcessingRequest request) {
    processorFactory.getProcessor(request.getRequestType())
            .process(request)
            .flatMap(requestRepository::save)
            .subscribe();
}
```

#### After (Synchronous with @Async)
```java
public CompletableFuture<ConnectResponseDto> processRequestAsync(ConnectRequestDto requestDto) {
    ProcessingRequest savedRequest = requestRepository.save(processingRequest);
    processAsync(savedRequest);
    return CompletableFuture.completedFuture(response);
}

@Async("processingExecutor")
public void processAsync(ProcessingRequest request) {
    try {
        ProcessingRequest result = processorFactory.getProcessor(request.getRequestType())
                .process(request);
        requestRepository.save(result);
    } catch (Exception error) {
        // Handle error
    }
}
```

### 5. Controller Layer Changes

#### Before (Reactive)
```java
public Mono<ResponseEntity<ConnectResponseDto>> processRequest(
        @Valid @RequestBody ConnectRequestDto request,
        @AuthenticationPrincipal Authentication authentication) {
    
    return connectService.processRequest(request)
            .map(response -> ResponseEntity.accepted().body(response))
            .doOnSuccess(response -> log.info("Request processing initiated"))
            .doOnError(error -> log.error("Failed to process request", error));
}
```

#### After (Synchronous with CompletableFuture)
```java
public CompletableFuture<ResponseEntity<ConnectResponseDto>> processRequest(
        @Valid @RequestBody ConnectRequestDto request,
        @AuthenticationPrincipal Authentication authentication) {
    
    return connectService.processRequestAsync(request)
            .thenApply(response -> ResponseEntity.accepted().body(response))
            .exceptionally(error -> {
                log.error("Failed to process request", error);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(errorResponse);
            });
}
```

## Benefits of the Conversion

### 1. Simplicity
- **Easier to understand**: Traditional synchronous code is more familiar to most developers
- **Simpler debugging**: Stack traces are clearer and easier to follow
- **Reduced complexity**: No need to understand reactive programming concepts

### 2. Performance
- **Better for CPU-intensive tasks**: Synchronous processing is more efficient for CPU-bound operations
- **Lower memory overhead**: No reactive streams overhead
- **Easier profiling**: Traditional profiling tools work better with synchronous code

### 3. Ecosystem Compatibility
- **Better library support**: Most Spring ecosystem libraries work better with synchronous code
- **Easier testing**: Traditional testing approaches work without reactive testing utilities
- **Simpler monitoring**: Standard monitoring tools work out of the box

### 4. Maintenance
- **Easier onboarding**: New developers can understand the codebase faster
- **Simpler troubleshooting**: Issues are easier to diagnose and fix
- **Better IDE support**: IDEs provide better support for synchronous code

## Async Processing Strategy

### 1. Thread Pool Configuration
The application uses three different thread pools:

- **taskExecutor**: General async operations (10-50 threads)
- **processingExecutor**: Request processing (5-20 threads)
- **externalApiExecutor**: External API calls (3-15 threads)

### 2. Async Method Annotations
```java
@Async("processingExecutor")
public void processAsync(ProcessingRequest request) {
    // Processing logic
}

@Async("externalApiExecutor")
public CompletableFuture<String> callExternalApi(String payload) {
    // External API call logic
}
```

### 3. Error Handling
```java
@Async("processingExecutor")
public void processAsync(ProcessingRequest request) {
    try {
        // Processing logic
    } catch (Exception error) {
        // Update request with error status
        request.setStatus(ProcessingStatus.FAILED);
        request.setErrorMessage(error.getMessage());
        requestRepository.save(request);
    }
}
```

## Configuration Changes

### 1. Server Configuration
```yaml
server:
  port: 8080
  servlet:
    context-path: /api
  tomcat:
    threads:
      max: 200
      min-spare: 10
```

### 2. Async Configuration
```yaml
# Processing Configuration
processing:
  async:
    core-pool-size: 10
    max-pool-size: 50
    queue-capacity: 1000
  retry:
    max-attempts: 3
    backoff-delay: 1000
    max-delay: 10000
```

## Migration Checklist

### 1. Dependencies
- [x] Updated `pom.xml` to use synchronous dependencies
- [x] Removed reactive-specific dependencies
- [x] Added `@EnableAsync` configuration

### 2. Repositories
- [x] Changed from `ReactiveMongoRepository` to `MongoRepository`
- [x] Updated return types from `Mono`/`Flux` to synchronous types
- [x] Updated method signatures

### 3. Services
- [x] Converted reactive methods to synchronous methods
- [x] Added `@Async` annotations for background processing
- [x] Updated error handling to use try-catch blocks

### 4. Controllers
- [x] Changed return types from `Mono<ResponseEntity<T>>` to `CompletableFuture<ResponseEntity<T>>`
- [x] Updated method implementations to use `CompletableFuture`
- [x] Updated error handling

### 5. Configuration
- [x] Updated `application.yml` for synchronous configuration
- [x] Added thread pool configurations
- [x] Updated MongoDB configuration

## Testing Changes

### 1. Unit Tests
```java
// Before (Reactive)
@Test
void testProcessRequest() {
    StepVerifier.create(connectService.processRequest(request))
            .expectNext(response)
            .verifyComplete();
}

// After (Synchronous)
@Test
void testProcessRequest() throws Exception {
    CompletableFuture<ConnectResponseDto> future = connectService.processRequestAsync(request);
    ConnectResponseDto response = future.get();
    assertThat(response.getStatus()).isEqualTo("ACCEPTED");
}
```

### 2. Integration Tests
```java
// Before (Reactive)
@Test
void testProcessRequestEndpoint() {
    webTestClient.post()
            .uri("/v1/connect/process")
            .bodyValue(request)
            .exchange()
            .expectStatus().isAccepted();
}

// After (Synchronous)
@Test
void testProcessRequestEndpoint() throws Exception {
    mockMvc.perform(post("/v1/connect/process")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted());
}
```

## Performance Considerations

### 1. Thread Pool Sizing
- **Core Pool Size**: Set based on expected concurrent requests
- **Max Pool Size**: Set based on system resources and expected load
- **Queue Capacity**: Set to handle burst traffic

### 2. Memory Usage
- **Lower overhead**: No reactive streams overhead
- **Better garbage collection**: Traditional object lifecycle is easier for GC
- **Simpler memory profiling**: Standard profiling tools work better

### 3. CPU Usage
- **More efficient**: Synchronous processing is more CPU-efficient
- **Better utilization**: Thread pools can be tuned for optimal CPU usage
- **Easier monitoring**: Standard CPU monitoring tools work out of the box

## Monitoring and Observability

### 1. Thread Pool Monitoring
```java
@Bean
public ThreadPoolTaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    // Configuration
    executor.setThreadNamePrefix("ConnectService-Async-");
    return executor;
}
```

### 2. Async Method Monitoring
```java
@Async("processingExecutor")
public void processAsync(ProcessingRequest request) {
    try {
        // Processing logic
        log.info("Processing completed for request: {}", request.getRequestId());
    } catch (Exception error) {
        log.error("Processing failed for request: {}", request.getRequestId(), error);
        // Error handling
    }
}
```

### 3. Metrics
- **Thread pool metrics**: Active threads, queue size, completed tasks
- **Processing metrics**: Success rate, error rate, processing time
- **External API metrics**: Response time, error rate, retry count

## Conclusion

The conversion from reactive to synchronous Spring Boot with `@Async` provides:

1. **Simpler codebase** that's easier to understand and maintain
2. **Better performance** for CPU-intensive operations
3. **Improved ecosystem compatibility** with existing tools and libraries
4. **Easier debugging and troubleshooting**
5. **Better developer experience** with familiar programming patterns

The async processing capabilities are maintained through `@Async` annotations and thread pools, ensuring that the service can still handle high loads efficiently while being much simpler to work with.
