package com.adyanta.connect.processing.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as a request processor
 * Classes with this annotation will be scanned for @ProcessRequestType methods
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface RequestProcessor {
    
    /**
     * Name of this processor (optional, defaults to class name)
     */
    String value() default "";
}
