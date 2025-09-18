# Annotation-Based Processing - Visual Guide

## How Annotation-Based Processing Works

```mermaid
graph TD
    %% Request comes in
    A[Request with RequestType] --> B[AnnotationBasedProcessorRegistry]
    
    %% Registry logic
    B --> C{Scan for @ProcessRequestType<br/>methods}
    
    %% Available processor methods
    C --> D[CustomerOnboardingProcessor<br/>@ProcessRequestType(CUSTOMER_ONBOARDING, priority=10)]
    C --> E[PaymentProcessor<br/>@ProcessRequestType(PAYMENT_PROCESSING, priority=20)]
    C --> F[YourCustomProcessor<br/>@ProcessRequestType(YOUR_TYPE, priority=25)]
    
    %% Selection logic
    D --> G{Select Highest<br/>Priority Method}
    E --> G
    F --> G
    
    %% Result
    G --> H[Invoke Selected Method]
    H --> I[Method.processRequest(request)]
    
    %% Styling
    classDef registry fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    classDef processor fill:#e8f5e8,stroke:#2e7d32,stroke-width:2px
    classDef selection fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    classDef result fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    
    class A,B registry
    class D,E,F processor
    class C,G selection
    class H,I result
```

## Annotation-Based vs Factory Pattern

### Factory Pattern (Old)
```java
// 1. Create interface
public interface RequestProcessor {
    boolean canHandle(RequestType type);
    Mono<ProcessingRequest> process(ProcessingRequest request);
}

// 2. Implement interface
@Component
public class PaymentProcessor implements RequestProcessor {
    @Override
    public boolean canHandle(RequestType type) {
        return type == RequestType.PAYMENT_PROCESSING;
    }
    
    @Override
    public Mono<ProcessingRequest> process(ProcessingRequest request) {
        // Processing logic
    }
}

// 3. Factory discovers and selects
RequestProcessor processor = factory.getProcessor(RequestType.PAYMENT_PROCESSING);
```

### Annotation-Based (New)
```java
// 1. Create processor class
@Component
@RequestProcessor("PaymentProcessor")
public class PaymentProcessor {
    
    @ProcessRequestType(
        value = {RequestType.PAYMENT_PROCESSING},
        priority = 20,
        description = "Handles payment processing"
    )
    public ProcessingRequest processPayment(ProcessingRequest request) {
        // Processing logic
    }
}

// 2. Registry automatically discovers and invokes
ProcessingRequest result = registry.processRequest(request);
```

## Creating a New Processor (3 Steps)

### Step 1: Create the Class
```java
@Component
@RequestProcessor("YourProcessor")
public class YourProcessor {
    // Methods here
}
```

### Step 2: Add Processing Method
```java
@ProcessRequestType(
    value = {RequestType.YOUR_TYPE},
    priority = 25,
    description = "Handles your custom type"
)
public ProcessingRequest processYourType(ProcessingRequest request) {
    // Your processing logic here
    return processingPipeline.execute(request);
}
```

### Step 3: That's It!
The registry automatically discovers and uses your method!

## Multiple Request Types Example

```java
@Component
@RequestProcessor("PaymentProcessor")
public class PaymentProcessor {
    
    // Handle payment processing
    @ProcessRequestType(
        value = {RequestType.PAYMENT_PROCESSING},
        priority = 20,
        description = "Handles payment processing"
    )
    public ProcessingRequest processPayment(ProcessingRequest request) {
        return processPaymentLogic(request);
    }
    
    // Handle refund processing
    @ProcessRequestType(
        value = {RequestType.REFUND_PROCESSING},
        priority = 15,
        description = "Handles refund processing"
    )
    public ProcessingRequest processRefund(ProcessingRequest request) {
        return processRefundLogic(request);
    }
    
    // Handle both payment and refund
    @ProcessRequestType(
        value = {RequestType.PAYMENT_PROCESSING, RequestType.REFUND_PROCESSING},
        priority = 10,
        description = "Handles both payment and refund"
    )
    public ProcessingRequest processPaymentOrRefund(ProcessingRequest request) {
        if (request.getRequestType() == RequestType.PAYMENT_PROCESSING) {
            return processPaymentLogic(request);
        } else {
            return processRefundLogic(request);
        }
    }
}
```

## Priority Selection Examples

### Example 1: PAYMENT_PROCESSING Request
```
Available Methods:
- processPaymentOrRefund (Priority: 10, Handles: PAYMENT_PROCESSING, REFUND_PROCESSING) ✓
- processPayment (Priority: 20, Handles: PAYMENT_PROCESSING) ✓
↓
Selected: processPayment (Higher Priority: 20)
```

### Example 2: REFUND_PROCESSING Request
```
Available Methods:
- processPaymentOrRefund (Priority: 10, Handles: PAYMENT_PROCESSING, REFUND_PROCESSING) ✓
- processRefund (Priority: 15, Handles: REFUND_PROCESSING) ✓
↓
Selected: processRefund (Higher Priority: 15)
```

### Example 3: UNKNOWN_TYPE Request
```
Available Methods:
- processPaymentOrRefund (Priority: 10, Handles: PAYMENT_PROCESSING, REFUND_PROCESSING) ✗
- processPayment (Priority: 20, Handles: PAYMENT_PROCESSING) ✗
- processRefund (Priority: 15, Handles: REFUND_PROCESSING) ✗
↓
Selected: None (No method handles this type)
Result: IllegalArgumentException
```

## Key Benefits

### 1. **Declarative**
- Clear intent with annotations
- Self-documenting code
- Easy to understand

### 2. **Flexible**
- Handle multiple types in one method
- Conditional processing logic
- Easy to add new types

### 3. **Maintainable**
- No complex factory logic
- Easy to add new processors
- Clear separation of concerns

### 4. **Testable**
- Easy to unit test individual methods
- Clear method signatures
- No complex inheritance

## Common Patterns

### 1. **Single Type Processor**
```java
@ProcessRequestType(value = {RequestType.PAYMENT_PROCESSING}, priority = 20)
public ProcessingRequest processPayment(ProcessingRequest request) {
    // Handle only payment processing
}
```

### 2. **Multiple Type Processor**
```java
@ProcessRequestType(
    value = {RequestType.PAYMENT_PROCESSING, RequestType.REFUND_PROCESSING},
    priority = 15
)
public ProcessingRequest processPaymentOrRefund(ProcessingRequest request) {
    // Handle both payment and refund
}
```

### 3. **Conditional Processor**
```java
@ProcessRequestType(value = {RequestType.PAYMENT_PROCESSING}, priority = 20)
public ProcessingRequest processPayment(ProcessingRequest request) {
    if (isHighValuePayment(request)) {
        return processHighValuePayment(request);
    } else {
        return processRegularPayment(request);
    }
}
```

### 4. **Fallback Processor**
```java
@ProcessRequestType(value = {RequestType.GENERIC_PROCESSING}, priority = 0)
public ProcessingRequest processGeneric(ProcessingRequest request) {
    // Handle any request type as fallback
}
```

## Testing Your Processor

### 1. **Unit Test**
```java
@Test
void shouldProcessPaymentRequest() {
    // Given
    ProcessingRequest request = createPaymentRequest();
    
    // When
    ProcessingRequest result = processor.processPayment(request);
    
    // Then
    assertThat(result.getStatus()).isEqualTo(ProcessingStatus.PROCESSING);
}
```

### 2. **Integration Test**
```java
@Test
void shouldSelectCorrectProcessor() {
    // Given
    RequestType requestType = RequestType.PAYMENT_PROCESSING;
    
    // When
    Optional<ProcessorMethod> method = registry.getProcessorMethod(requestType);
    
    // Then
    assertThat(method).isPresent();
    assertThat(method.get().getMethod().getName()).isEqualTo("processPayment");
}
```

## Migration Guide

### From Factory Pattern to Annotation-Based

1. **Remove** `implements RequestProcessor`
2. **Add** `@RequestProcessor` annotation to class
3. **Add** `@ProcessRequestType` annotation to methods
4. **Change** method signature to return `ProcessingRequest` instead of `Mono<ProcessingRequest>`
5. **Remove** `canHandle()` method
6. **Remove** `getPriority()` method
7. **Remove** `getName()` method

### Before
```java
@Component
public class PaymentProcessor implements RequestProcessor {
    @Override
    public boolean canHandle(RequestType type) {
        return type == RequestType.PAYMENT_PROCESSING;
    }
    
    @Override
    public Mono<ProcessingRequest> process(ProcessingRequest request) {
        // Processing logic
    }
    
    @Override
    public int getPriority() {
        return 20;
    }
    
    @Override
    public String getName() {
        return "PaymentProcessor";
    }
}
```

### After
```java
@Component
@RequestProcessor("PaymentProcessor")
public class PaymentProcessor {
    @ProcessRequestType(
        value = {RequestType.PAYMENT_PROCESSING},
        priority = 20,
        description = "Handles payment processing"
    )
    public ProcessingRequest processPayment(ProcessingRequest request) {
        // Processing logic
    }
}
```

## Summary

Annotation-based processing is:

1. **Simpler** - No complex interfaces or inheritance
2. **Cleaner** - Declarative annotations instead of boilerplate code
3. **More Flexible** - Handle multiple types, conditional logic
4. **Easier to Test** - Simple method signatures
5. **Self-Documenting** - Annotations explain what each method does

**Easy to use, easy to extend, easy to maintain!** 🚀
