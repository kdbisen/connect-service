package com.adyanta.connect.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Configuration for API versioning
 * 
 * This class manages API versioning configuration including:
 * - Supported API versions
 * - Version deprecation policies
 * - Version-specific features
 * - Migration guidance
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "api.versioning")
public class ApiVersionConfig implements WebMvcConfigurer {
    
    /**
     * Current API version
     */
    private String currentVersion = "2.0";
    
    /**
     * Default API version for backward compatibility
     */
    private String defaultVersion = "1.0";
    
    /**
     * Supported API versions
     */
    private String[] supportedVersions = {"1.0", "2.0"};
    
    /**
     * Deprecated API versions
     */
    private String[] deprecatedVersions = {};
    
    /**
     * Version-specific features configuration
     */
    private Features features = new Features();
    
    /**
     * Deprecation policy configuration
     */
    private DeprecationPolicy deprecationPolicy = new DeprecationPolicy();
    
    @Data
    public static class Features {
        /**
         * Features available in v1.0
         */
        private V1Features v1 = new V1Features();
        
        /**
         * Features available in v2.0
         */
        private V2Features v2 = new V2Features();
    }
    
    @Data
    public static class V1Features {
        private boolean basicProcessing = true;
        private boolean xmlToJsonConversion = true;
        private boolean externalApiIntegration = true;
        private boolean fenergoIntegration = true;
        private boolean mongodbStorage = true;
        private boolean auditLogging = true;
        private boolean healthChecks = true;
        private boolean swaggerDocumentation = true;
        private boolean correlationId = true;
        private boolean vaultIntegration = true;
        private boolean sslMongoDb = true;
        private boolean reactiveProcessing = true;
    }
    
    @Data
    public static class V2Features {
        private boolean basicProcessing = true;
        private boolean xmlToJsonConversion = true;
        private boolean externalApiIntegration = true;
        private boolean fenergoIntegration = true;
        private boolean mongodbStorage = true;
        private boolean auditLogging = true;
        private boolean healthChecks = true;
        private boolean swaggerDocumentation = true;
        private boolean correlationId = true;
        private boolean vaultIntegration = true;
        private boolean sslMongoDb = true;
        private boolean reactiveProcessing = true;
        
        // v2.0 specific features
        private boolean enhancedValidation = true;
        private boolean batchProcessing = true;
        private boolean advancedMonitoring = true;
        private boolean realTimeMetrics = true;
        private boolean improvedErrorHandling = true;
        private boolean multiConnectorSupport = true;
        private boolean routeExecutor = true;
        private boolean visualUiConsole = false; // Future feature
    }
    
    @Data
    public static class DeprecationPolicy {
        /**
         * Number of months before deprecation notice
         */
        private int deprecationNoticeMonths = 12;
        
        /**
         * Number of months before version removal
         */
        private int removalNoticeMonths = 24;
        
        /**
         * Migration guide URL template
         */
        private String migrationGuideUrl = "https://docs.adyanta.com/connect-service/migration/{from}-to-{to}";
        
        /**
         * Support contact for migration questions
         */
        private String supportContact = "support@adyanta.com";
    }
    
    /**
     * Get version-specific features
     * 
     * @param version API version
     * @return Features for the specified version
     */
    public Object getFeaturesForVersion(String version) {
        switch (version) {
            case "1.0":
                return features.getV1();
            case "2.0":
                return features.getV2();
            default:
                throw new IllegalArgumentException("Unsupported API version: " + version);
        }
    }
    
    /**
     * Check if a version is supported
     * 
     * @param version API version
     * @return true if version is supported
     */
    public boolean isVersionSupported(String version) {
        for (String supportedVersion : supportedVersions) {
            if (supportedVersion.equals(version)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if a version is deprecated
     * 
     * @param version API version
     * @return true if version is deprecated
     */
    public boolean isVersionDeprecated(String version) {
        for (String deprecatedVersion : deprecatedVersions) {
            if (deprecatedVersion.equals(version)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get migration guide URL
     * 
     * @param fromVersion Source version
     * @param toVersion Target version
     * @return Migration guide URL
     */
    public String getMigrationGuideUrl(String fromVersion, String toVersion) {
        return deprecationPolicy.getMigrationGuideUrl()
                .replace("{from}", fromVersion)
                .replace("{to}", toVersion);
    }
    
    /**
     * Get API version information
     * 
     * @return Complete API version information
     */
    public ApiVersionInfo getApiVersionInfo() {
        return ApiVersionInfo.builder()
                .currentVersion(currentVersion)
                .defaultVersion(defaultVersion)
                .supportedVersions(supportedVersions)
                .deprecatedVersions(deprecatedVersions)
                .features(features)
                .deprecationPolicy(deprecationPolicy)
                .build();
    }
    
    @Data
    @lombok.Builder
    public static class ApiVersionInfo {
        private String currentVersion;
        private String defaultVersion;
        private String[] supportedVersions;
        private String[] deprecatedVersions;
        private Features features;
        private DeprecationPolicy deprecationPolicy;
    }
}
