# RequestProcessorFactory - Visual Guide

## How the Factory Works

```mermaid
graph TD
    %% Request comes in
    A[Request with RequestType] --> B[RequestProcessorFactory]
    
    %% Factory logic
    B --> C{Find Processors<br/>that can handle<br/>this RequestType}
    
    %% Available processors
    C --> D[GenericRequestProcessor<br/>Priority: 0<br/>Handles: ALL]
    C --> E[CustomerOnboardingProcessor<br/>Priority: 10<br/>Handles: CUSTOMER_ONBOARDING]
    C --> F[YourCustomProcessor<br/>Priority: 20<br/>Handles: YOUR_TYPE]
    
    %% Selection logic
    D --> G{Select Highest<br/>Priority Processor}
    E --> G
    F --> G
    
    %% Result
    G --> H[Return Selected Processor]
    H --> I[Processor.process(request)]
    
    %% Styling
    classDef factory fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    classDef processor fill:#e8f5e8,stroke:#2e7d32,stroke-width:2px
    classDef selection fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    classDef result fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    
    class A,B factory
    class D,E,F processor
    class C,G selection
    class H,I result
```

## Processor Selection Examples

### Example 1: CUSTOMER_ONBOARDING Request
```
RequestType: CUSTOMER_ONBOARDING
↓
Available Processors:
- GenericRequestProcessor (Priority: 0, Handles: ALL) ✓
- CustomerOnboardingProcessor (Priority: 10, Handles: CUSTOMER_ONBOARDING) ✓
↓
Selected: CustomerOnboardingProcessor (Higher Priority: 10)
```

### Example 2: YOUR_CUSTOM_TYPE Request
```
RequestType: YOUR_CUSTOM_TYPE
↓
Available Processors:
- GenericRequestProcessor (Priority: 0, Handles: ALL) ✓
- YourCustomProcessor (Priority: 20, Handles: YOUR_CUSTOM_TYPE) ✓
↓
Selected: YourCustomProcessor (Higher Priority: 20)
```

### Example 3: UNKNOWN_TYPE Request
```
RequestType: UNKNOWN_TYPE
↓
Available Processors:
- GenericRequestProcessor (Priority: 0, Handles: ALL) ✓
- CustomerOnboardingProcessor (Priority: 10, Handles: CUSTOMER_ONBOARDING) ✗
- YourCustomProcessor (Priority: 20, Handles: YOUR_CUSTOM_TYPE) ✗
↓
Selected: GenericRequestProcessor (Only one that can handle)
```

## Creating a New Processor - Step by Step

### Step 1: Create the Class
```java
@Component
public class PaymentProcessor implements RequestProcessor {
    // Implementation here
}
```

### Step 2: Implement Required Methods
```java
@Override
public boolean canHandle(RequestType requestType) {
    return requestType == RequestType.PAYMENT_PROCESSING;
}

@Override
public String getName() {
    return "PaymentProcessor";
}

@Override
public int getPriority() {
    return 15; // Higher than generic (0), lower than specialized (20+)
}

@Override
public Mono<ProcessingRequest> process(ProcessingRequest request) {
    // Your processing logic here
    return processingPipeline.execute(request);
}
```

### Step 3: Add Request Type (if new)
```java
public enum RequestType {
    GENERIC_PROCESSING,
    CUSTOMER_ONBOARDING,
    PAYMENT_PROCESSING,  // Your new type
    // ... other types
}
```

### Step 4: That's It!
The factory automatically discovers your processor and uses it when needed!

## Priority System

| Priority Range | Usage | Examples |
|----------------|-------|----------|
| 0-9 | Generic/Fallback | GenericRequestProcessor (0) |
| 10-19 | Standard Business | CustomerOnboardingProcessor (10) |
| 20-29 | Specialized | PaymentProcessor (20) |
| 30+ | High Priority | CriticalProcessor (30) |

## Key Benefits

### 1. **Easy to Add New Processors**
- Just create a new class
- No configuration needed
- Automatic discovery

### 2. **Flexible Selection**
- Priority-based selection
- Fallback to generic processor
- Multiple processors per type

### 3. **Clean Separation**
- Each processor handles one concern
- Easy to test individually
- Easy to modify without affecting others

### 4. **Extensible**
- Add new request types easily
- Add new processors easily
- No changes to existing code

## Common Patterns

### 1. **Generic Processor** (Fallback)
```java
@Override
public boolean canHandle(RequestType requestType) {
    return true; // Handles all types
}

@Override
public int getPriority() {
    return 0; // Lowest priority
}
```

### 2. **Specific Processor** (Specialized)
```java
@Override
public boolean canHandle(RequestType requestType) {
    return requestType == RequestType.SPECIFIC_TYPE;
}

@Override
public int getPriority() {
    return 20; // Higher priority
}
```

### 3. **Conditional Processor** (Smart)
```java
@Override
public boolean canHandle(RequestType requestType) {
    return requestType == RequestType.SPECIFIC_TYPE && 
           someBusinessCondition();
}
```

## Testing Your Processor

### 1. **Test canHandle()**
```java
@Test
void shouldHandleCorrectRequestType() {
    assertTrue(processor.canHandle(RequestType.PAYMENT_PROCESSING));
    assertFalse(processor.canHandle(RequestType.CUSTOMER_ONBOARDING));
}
```

### 2. **Test Priority**
```java
@Test
void shouldHaveCorrectPriority() {
    assertEquals(15, processor.getPriority());
}
```

### 3. **Test Processing**
```java
@Test
void shouldProcessRequest() {
    ProcessingRequest result = processor.process(request).block();
    assertThat(result.getStatus()).isEqualTo(ProcessingStatus.COMPLETED);
}
```

## Troubleshooting

### Problem: Processor Not Found
**Solution**: Check `@Component` annotation and package scanning

### Problem: Wrong Processor Selected
**Solution**: Check priority values and `canHandle()` logic

### Problem: Processing Fails
**Solution**: Check error logs and pipeline configuration

## Summary

The `RequestProcessorFactory` is simple but powerful:

1. **Creates** processors based on request type
2. **Selects** the highest priority processor
3. **Falls back** to generic processor if needed
4. **Automatically discovers** new processors

**Easy to use, easy to extend!** 🚀
