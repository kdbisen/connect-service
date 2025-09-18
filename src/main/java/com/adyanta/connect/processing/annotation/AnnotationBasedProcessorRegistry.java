package com.adyanta.connect.processing.annotation;

import com.adyanta.connect.domain.document.ProcessingRequest;
import com.adyanta.connect.domain.enums.RequestType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry that discovers and manages annotation-based request processors
 * This replaces the factory pattern with annotation-based routing
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnnotationBasedProcessorRegistry {
    
    private final ApplicationContext applicationContext;
    private final Map<RequestType, ProcessorMethod> processorMethods = new HashMap<>();
    private boolean initialized = false;
    
    /**
     * Initialize the registry by scanning for @ProcessRequestType methods
     */
    public void initialize() {
        if (initialized) {
            return;
        }
        
        log.info("Initializing annotation-based processor registry...");
        
        // Get all beans with @RequestProcessor annotation
        Map<String, Object> processorBeans = applicationContext.getBeansWithAnnotation(
                com.adyanta.connect.processing.annotation.RequestProcessor.class);
        
        for (Map.Entry<String, Object> entry : processorBeans.entrySet()) {
            Object bean = entry.getValue();
            Class<?> beanClass = bean.getClass();
            
            // Scan all methods in the bean
            for (Method method : beanClass.getDeclaredMethods()) {
                ProcessRequestType annotation = AnnotationUtils.findAnnotation(method, ProcessRequestType.class);
                if (annotation != null) {
                    registerProcessorMethod(bean, method, annotation);
                }
            }
        }
        
        initialized = true;
        log.info("Annotation-based processor registry initialized with {} processors", processorMethods.size());
    }
    
    /**
     * Register a processor method
     */
    private void registerProcessorMethod(Object bean, Method method, ProcessRequestType annotation) {
        try {
            // Make method accessible
            method.setAccessible(true);
            
            // Create processor method wrapper
            ProcessorMethod processorMethod = new ProcessorMethod(bean, method, annotation);
            
            // Register for each request type
            for (RequestType requestType : annotation.value()) {
                ProcessorMethod existing = processorMethods.get(requestType);
                
                if (existing == null || annotation.priority() > existing.getPriority()) {
                    processorMethods.put(requestType, processorMethod);
                    log.debug("Registered processor method {} for request type {} with priority {}", 
                            method.getName(), requestType, annotation.priority());
                } else {
                    log.debug("Skipped processor method {} for request type {} (lower priority: {} < {})", 
                            method.getName(), requestType, annotation.priority(), existing.getPriority());
                }
            }
        } catch (Exception e) {
            log.error("Failed to register processor method {} in bean {}", method.getName(), bean.getClass().getSimpleName(), e);
        }
    }
    
    /**
     * Get the processor method for a given request type
     */
    public Optional<ProcessorMethod> getProcessorMethod(RequestType requestType) {
        if (!initialized) {
            initialize();
        }
        
        return Optional.ofNullable(processorMethods.get(requestType));
    }
    
    /**
     * Process a request using the appropriate processor method
     */
    public ProcessingRequest processRequest(ProcessingRequest request) {
        RequestType requestType = request.getRequestType();
        
        Optional<ProcessorMethod> processorMethod = getProcessorMethod(requestType);
        
        if (processorMethod.isPresent()) {
            try {
                log.debug("Processing request {} with method {} in bean {}", 
                        request.getRequestId(), 
                        processorMethod.get().getMethod().getName(),
                        processorMethod.get().getBean().getClass().getSimpleName());
                
                return (ProcessingRequest) processorMethod.get().getMethod().invoke(
                        processorMethod.get().getBean(), request);
                        
            } catch (Exception e) {
                log.error("Failed to process request {} with processor method {}", 
                        request.getRequestId(), processorMethod.get().getMethod().getName(), e);
                throw new RuntimeException("Failed to process request", e);
            }
        } else {
            log.warn("No processor method found for request type: {}", requestType);
            throw new IllegalArgumentException("No processor method found for request type: " + requestType);
        }
    }
    
    /**
     * Get all registered processor methods
     */
    public Map<RequestType, ProcessorMethod> getAllProcessorMethods() {
        if (!initialized) {
            initialize();
        }
        return new HashMap<>(processorMethods);
    }
    
    /**
     * Wrapper class for processor method information
     */
    public static class ProcessorMethod {
        private final Object bean;
        private final Method method;
        private final ProcessRequestType annotation;
        
        public ProcessorMethod(Object bean, Method method, ProcessRequestType annotation) {
            this.bean = bean;
            this.method = method;
            this.annotation = annotation;
        }
        
        public Object getBean() {
            return bean;
        }
        
        public Method getMethod() {
            return method;
        }
        
        public ProcessRequestType getAnnotation() {
            return annotation;
        }
        
        public int getPriority() {
            return annotation.priority();
        }
        
        public String getDescription() {
            return annotation.description();
        }
    }
}
