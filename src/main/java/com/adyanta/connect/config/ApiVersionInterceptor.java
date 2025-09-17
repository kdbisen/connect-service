package com.adyanta.connect.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interceptor for handling API versioning
 * 
 * This interceptor:
 * - Extracts API version from request path
 * - Validates version support
 * - Adds version information to request attributes
 * - Handles deprecation warnings
 * - Provides migration guidance
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiVersionInterceptor implements HandlerInterceptor {
    
    private final ApiVersionConfig versionConfig;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestPath = request.getRequestURI();
        String version = extractVersionFromPath(requestPath);
        
        if (version != null) {
            // Add version to request attributes for use in controllers
            request.setAttribute("apiVersion", version);
            
            // Validate version support
            if (!versionConfig.isVersionSupported(version)) {
                log.warn("Unsupported API version requested: {} for path: {}", version, requestPath);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("application/json");
                response.getWriter().write(String.format("""
                        {
                          "error": "UNSUPPORTED_VERSION",
                          "message": "API version %s is not supported",
                          "supportedVersions": %s,
                          "currentVersion": "%s",
                          "timestamp": "%s"
                        }
                        """, version, java.util.Arrays.toString(versionConfig.getSupportedVersions()), 
                        versionConfig.getCurrentVersion(), java.time.Instant.now()));
                return false;
            }
            
            // Check for deprecation
            if (versionConfig.isVersionDeprecated(version)) {
                log.info("Deprecated API version accessed: {} for path: {}", version, requestPath);
                response.setHeader("X-API-Deprecated", "true");
                response.setHeader("X-API-Deprecated-Version", version);
                response.setHeader("X-API-Current-Version", versionConfig.getCurrentVersion());
                response.setHeader("X-API-Migration-Guide", 
                    versionConfig.getMigrationGuideUrl(version, versionConfig.getCurrentVersion()));
            }
            
            // Add version headers
            response.setHeader("X-API-Version", version);
            response.setHeader("X-API-Supported-Versions", String.join(",", versionConfig.getSupportedVersions()));
            
            log.debug("Processing request with API version: {} for path: {}", version, requestPath);
        }
        
        return true;
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        String version = (String) request.getAttribute("apiVersion");
        
        if (version != null) {
            // Add version-specific response headers
            response.setHeader("X-API-Version-Used", version);
            
            // Add feature information based on version
            if ("2.0".equals(version)) {
                response.setHeader("X-API-Features", "enhanced-validation,batch-processing,advanced-monitoring");
            } else if ("1.0".equals(version)) {
                response.setHeader("X-API-Features", "basic-processing,xml-to-json,external-api-integration");
            }
        }
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        String version = (String) request.getAttribute("apiVersion");
        String requestPath = request.getRequestURI();
        
        if (version != null) {
            log.debug("Completed request processing for API version: {} on path: {} with status: {}", 
                version, requestPath, response.getStatus());
        }
    }
    
    /**
     * Extract API version from request path
     * 
     * @param requestPath The request path
     * @return API version or null if not found
     */
    private String extractVersionFromPath(String requestPath) {
        if (requestPath == null || requestPath.isEmpty()) {
            return null;
        }
        
        // Remove leading slash and split by slash
        String[] pathSegments = requestPath.startsWith("/") ? 
            requestPath.substring(1).split("/") : requestPath.split("/");
        
        // Check if first segment is a version (v1, v2, etc.)
        if (pathSegments.length > 0 && pathSegments[0].startsWith("v")) {
            String versionSegment = pathSegments[0];
            // Extract version number (e.g., "v1" -> "1.0", "v2" -> "2.0")
            if (versionSegment.matches("v\\d+")) {
                String versionNumber = versionSegment.substring(1);
                return versionNumber + ".0";
            }
        }
        
        return null;
    }
}
