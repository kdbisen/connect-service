package com.adyanta.connect.domain.enums;

/**
 * Enum representing different types of requests that can be processed
 * This allows for extensible request handling based on business requirements
 */
public enum RequestType {
    
    /**
     * Customer onboarding request
     */
    CUSTOMER_ONBOARDING("customer_onboarding", "Customer Onboarding Process"),
    
    /**
     * KYC verification request
     */
    KYC_VERIFICATION("kyc_verification", "KYC Verification Process"),
    
    /**
     * Document processing request
     */
    DOCUMENT_PROCESSING("document_processing", "Document Processing"),
    
    /**
     * Compliance check request
     */
    COMPLIANCE_CHECK("compliance_check", "Compliance Check"),
    
    /**
     * Risk assessment request
     */
    RISK_ASSESSMENT("risk_assessment", "Risk Assessment"),
    
    /**
     * Transaction monitoring request
     */
    TRANSACTION_MONITORING("transaction_monitoring", "Transaction Monitoring"),
    
    /**
     * Generic data processing request
     */
    GENERIC_PROCESSING("generic_processing", "Generic Data Processing");
    
    private final String code;
    private final String description;
    
    RequestType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get RequestType by code
     */
    public static RequestType fromCode(String code) {
        for (RequestType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown request type code: " + code);
    }
}
