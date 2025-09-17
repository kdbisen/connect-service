package com.adyanta.connect.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for the Connect Service
 * 
 * This configuration class sets up:
 * - API versioning interceptor
 * - CORS configuration
 * - Request/response interceptors
 * - Web-specific beans
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    
    private final ApiVersionInterceptor apiVersionInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Add API versioning interceptor
        registry.addInterceptor(apiVersionInterceptor)
                .addPathPatterns("/v*/**") // Apply to all versioned paths
                .excludePathPatterns(
                    "/actuator/**",      // Exclude actuator endpoints
                    "/swagger-ui/**",    // Exclude Swagger UI
                    "/v3/api-docs/**",   // Exclude OpenAPI docs
                    "/error"             // Exclude error pages
                );
    }
}
