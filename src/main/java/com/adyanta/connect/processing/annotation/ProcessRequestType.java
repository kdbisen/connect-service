package com.adyanta.connect.processing.annotation;

import com.adyanta.connect.domain.enums.RequestType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should handle specific request types
 * This provides a declarative way to route requests based on RequestType enum
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProcessRequestType {
    
    /**
     * The request types this method can handle
     */
    RequestType[] value();
    
    /**
     * Priority for this processor (higher number = higher priority)
     * Default is 0
     */
    int priority() default 0;
    
    /**
     * Description of what this processor does
     */
    String description() default "";
}
