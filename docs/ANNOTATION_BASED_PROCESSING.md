# Annotation-Based Request Processing

## Overview

This document explains how to use annotation-based request processing instead of the factory pattern. This approach provides a more declarative and cleaner way to handle different request types.

## How It Works

### 1. **Annotation-Based Routing**
Instead of using a factory pattern, we use annotations to mark methods that should handle specific request types.

### 2. **Automatic Discovery**
The system automatically scans for methods with `@ProcessRequestType` annotations and creates a registry.

### 3. **Priority-Based Selection**
When multiple methods can handle the same request type, the one with the highest priority is selected.

## Annotations

### 1. **@RequestProcessor**
Marks a class as a request processor.

```java
@RequestProcessor("MyProcessor")
@Component
public class MyProcessor {
    // Processor methods here
}
```

### 2. **@ProcessRequestType**
Marks a method that should handle specific request types.

```java
@ProcessRequestType(
    value = {RequestType.PAYMENT_PROCESSING},
    priority = 20,
    description = "Handles payment processing requests"
)
public ProcessingRequest processPayment(ProcessingRequest request) {
    // Processing logic here
}
```

## Example Processors

### 1. **Customer Onboarding Processor**

```java
@Slf4j
@Component
@RequestProcessor("CustomerOnboardingProcessor")
@RequiredArgsConstructor
public class CustomerOnboardingProcessor {
    
    private final ProcessingPipeline processingPipeline;
    
    @ProcessRequestType(
        value = {RequestType.CUSTOMER_ONBOARDING},
        priority = 10,
        description = "Handles customer onboarding requests with KYC and compliance checks"
    )
    public ProcessingRequest processCustomerOnboarding(ProcessingRequest request) {
        log.info("Processing customer onboarding request: {}", request.getRequestId());
        
        // Add customer onboarding specific metadata
        ProcessingRequest onboardingRequest = request.toBuilder()
                .status(ProcessingStatus.PROCESSING)
                .processingStartTime(LocalDateTime.now())
                .metadata(addCustomerOnboardingMetadata(request.getMetadata()))
                .build();
        
        return processingPipeline.execute(onboardingRequest);
    }
    
    private Map<String, Object> addCustomerOnboardingMetadata(Map<String, Object> existingMetadata) {
        if (existingMetadata == null) {
            existingMetadata = new HashMap<>();
        }
        existingMetadata.put("processType", "customer_onboarding");
        existingMetadata.put("requiresKyc", true);
        existingMetadata.put("requiresCompliance", true);
        return existingMetadata;
    }
}
```

### 2. **Payment Processor**

```java
@Slf4j
@Component
@RequestProcessor("PaymentProcessor")
@RequiredArgsConstructor
public class PaymentProcessor {
    
    private final ProcessingPipeline processingPipeline;
    
    @ProcessRequestType(
        value = {RequestType.PAYMENT_PROCESSING},
        priority = 20,
        description = "Handles payment processing requests with fraud checks"
    )
    public ProcessingRequest processPayment(ProcessingRequest request) {
        log.info("Processing payment request: {}", request.getRequestId());
        
        // Validate payment request
        validatePaymentRequest(request);
        
        // Add payment-specific metadata
        ProcessingRequest paymentRequest = request.toBuilder()
                .status(ProcessingStatus.PROCESSING)
                .processingStartTime(LocalDateTime.now())
                .metadata(addPaymentMetadata(request.getMetadata()))
                .build();
        
        return processingPipeline.execute(paymentRequest);
    }
    
    @ProcessRequestType(
        value = {RequestType.REFUND_PROCESSING},
        priority = 15,
        description = "Handles refund processing requests"
    )
    public ProcessingRequest processRefund(ProcessingRequest request) {
        log.info("Processing refund request: {}", request.getRequestId());
        
        // Add refund-specific metadata
        ProcessingRequest refundRequest = request.toBuilder()
                .status(ProcessingStatus.PROCESSING)
                .processingStartTime(LocalDateTime.now())
                .metadata(addRefundMetadata(request.getMetadata()))
                .build();
        
        return processingPipeline.execute(refundRequest);
    }
    
    private void validatePaymentRequest(ProcessingRequest request) {
        if (request.getMetadata() == null || 
            !request.getMetadata().containsKey("paymentAmount")) {
            throw new IllegalArgumentException("Payment amount is required");
        }
    }
    
    private Map<String, Object> addPaymentMetadata(Map<String, Object> existingMetadata) {
        if (existingMetadata == null) {
            existingMetadata = new HashMap<>();
        }
        existingMetadata.put("processType", "payment_processing");
        existingMetadata.put("requiresFraudCheck", true);
        existingMetadata.put("requiresCompliance", true);
        return existingMetadata;
    }
}
```

## How to Create a New Processor

### Step 1: Create the Processor Class

```java
@Slf4j
@Component
@RequestProcessor("YourCustomProcessor")
@RequiredArgsConstructor
public class YourCustomProcessor {
    
    private final ProcessingPipeline processingPipeline;
    
    @ProcessRequestType(
        value = {RequestType.YOUR_CUSTOM_TYPE},
        priority = 25,
        description = "Handles your custom request type"
    )
    public ProcessingRequest processYourCustomType(ProcessingRequest request) {
        log.info("Processing your custom request: {}", request.getRequestId());
        
        // Add custom preprocessing logic
        ProcessingRequest customRequest = addCustomMetadata(request);
        
        // Process through the pipeline
        return processingPipeline.execute(customRequest);
    }
    
    private ProcessingRequest addCustomMetadata(ProcessingRequest request) {
        return request.toBuilder()
                .status(ProcessingStatus.PROCESSING)
                .processingStartTime(LocalDateTime.now())
                .metadata(addCustomFields(request.getMetadata()))
                .build();
    }
    
    private Map<String, Object> addCustomFields(Map<String, Object> existingMetadata) {
        if (existingMetadata == null) {
            existingMetadata = new HashMap<>();
        }
        existingMetadata.put("processType", "your_custom_type");
        existingMetadata.put("customField", "customValue");
        return existingMetadata;
    }
}
```

### Step 2: Add Request Type (if new)

```java
public enum RequestType {
    // ... existing types
    YOUR_CUSTOM_TYPE("your_custom_type", "Your Custom Request Type"),
    // ... other types
}
```

### Step 3: That's It!

The annotation-based registry automatically discovers your processor method and uses it when needed!

## Priority System

| Priority | Usage | Examples |
|----------|-------|----------|
| 0-9 | Generic/Fallback | Generic processors |
| 10-19 | Standard Business | Customer onboarding, KYC |
| 20-29 | Specialized | Payment processing, refunds |
| 30+ | High Priority | Critical processes |

## Multiple Request Types

You can handle multiple request types in the same method:

```java
@ProcessRequestType(
    value = {RequestType.PAYMENT_PROCESSING, RequestType.REFUND_PROCESSING},
    priority = 20,
    description = "Handles both payment and refund processing"
)
public ProcessingRequest processPaymentOrRefund(ProcessingRequest request) {
    // Handle both payment and refund requests
    if (request.getRequestType() == RequestType.PAYMENT_PROCESSING) {
        return processPayment(request);
    } else {
        return processRefund(request);
    }
}
```

## Conditional Processing

You can add conditional logic in your processor methods:

```java
@ProcessRequestType(
    value = {RequestType.PAYMENT_PROCESSING},
    priority = 20,
    description = "Handles payment processing with conditional logic"
)
public ProcessingRequest processPayment(ProcessingRequest request) {
    // Check if this is a high-value payment
    if (isHighValuePayment(request)) {
        return processHighValuePayment(request);
    } else {
        return processRegularPayment(request);
    }
}

private boolean isHighValuePayment(ProcessingRequest request) {
    Object amount = request.getMetadata().get("paymentAmount");
    if (amount instanceof Number) {
        return ((Number) amount).doubleValue() > 10000.0;
    }
    return false;
}
```

## Testing Your Processor

### 1. **Unit Test**

```java
@ExtendWith(MockitoExtension.class)
class YourCustomProcessorTest {
    
    @Mock
    private ProcessingPipeline processingPipeline;
    
    @InjectMocks
    private YourCustomProcessor processor;
    
    @Test
    void shouldProcessCustomRequest() {
        // Given
        ProcessingRequest request = createTestRequest();
        ProcessingRequest expectedResult = createExpectedResult();
        
        when(processingPipeline.execute(any())).thenReturn(expectedResult);
        
        // When
        ProcessingRequest result = processor.processYourCustomType(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ProcessingStatus.PROCESSING);
    }
}
```

### 2. **Integration Test**

```java
@SpringBootTest
class AnnotationBasedProcessorRegistryTest {
    
    @Autowired
    private AnnotationBasedProcessorRegistry registry;
    
    @Test
    void shouldFindProcessorForRequestType() {
        // Given
        RequestType requestType = RequestType.PAYMENT_PROCESSING;
        
        // When
        Optional<ProcessorMethod> processorMethod = registry.getProcessorMethod(requestType);
        
        // Then
        assertThat(processorMethod).isPresent();
        assertThat(processorMethod.get().getMethod().getName()).isEqualTo("processPayment");
    }
}
```

## Benefits of Annotation-Based Processing

### 1. **Declarative**
- Clear intent with annotations
- Easy to understand what each method does
- Self-documenting code

### 2. **Flexible**
- Handle multiple request types in one method
- Conditional processing logic
- Easy to add new request types

### 3. **Maintainable**
- No complex factory logic
- Easy to add new processors
- Clear separation of concerns

### 4. **Testable**
- Easy to unit test individual methods
- Clear method signatures
- No complex inheritance hierarchies

## Migration from Factory Pattern

### Before (Factory Pattern)
```java
// Create processor class
public class MyProcessor implements RequestProcessor {
    @Override
    public boolean canHandle(RequestType requestType) {
        return requestType == RequestType.MY_TYPE;
    }
    
    @Override
    public Mono<ProcessingRequest> process(ProcessingRequest request) {
        // Processing logic
    }
}

// Factory automatically discovers it
RequestProcessor processor = factory.getProcessor(RequestType.MY_TYPE);
```

### After (Annotation-Based)
```java
// Create processor class
@Component
@RequestProcessor("MyProcessor")
public class MyProcessor {
    @ProcessRequestType(value = {RequestType.MY_TYPE}, priority = 20)
    public ProcessingRequest processMyType(ProcessingRequest request) {
        // Processing logic
    }
}

// Registry automatically discovers it
ProcessingRequest result = registry.processRequest(request);
```

## Configuration

### 1. **Enable Component Scanning**
```java
@SpringBootApplication
@ComponentScan(basePackages = "com.adyanta.connect.processing.processors.annotation")
public class ConnectServiceApplication {
    // Application class
}
```

### 2. **Register the Registry**
```java
@Configuration
public class ProcessingConfig {
    
    @Bean
    public AnnotationBasedProcessorRegistry processorRegistry(ApplicationContext applicationContext) {
        return new AnnotationBasedProcessorRegistry(applicationContext);
    }
}
```

## Troubleshooting

### 1. **Processor Not Found**
- Check `@Component` annotation on the class
- Check `@ProcessRequestType` annotation on the method
- Verify package scanning configuration

### 2. **Wrong Processor Selected**
- Check priority values
- Verify request type values in annotation
- Check for multiple methods handling same type

### 3. **Processing Fails**
- Check method signature (must return ProcessingRequest)
- Check method parameters (must accept ProcessingRequest)
- Verify pipeline configuration

## Summary

Annotation-based processing provides:

1. **Cleaner Code** - Declarative annotations instead of complex factory logic
2. **Better Flexibility** - Handle multiple types, conditional logic
3. **Easier Testing** - Simple method signatures, clear responsibilities
4. **Automatic Discovery** - No manual registration needed
5. **Priority-Based Selection** - Clear precedence rules

**Easy to use, easy to extend, easy to maintain!** 🚀
