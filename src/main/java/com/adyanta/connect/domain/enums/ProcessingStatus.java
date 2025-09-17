package com.adyanta.connect.domain.enums;

/**
 * Enum representing the status of request processing
 */
public enum ProcessingStatus {
    
    /**
     * Request received and queued for processing
     */
    RECEIVED("received", "Request received"),
    
    /**
     * Request is being processed
     */
    PROCESSING("processing", "Request is being processed"),
    
    /**
     * XML to JSON conversion completed
     */
    XML_CONVERTED("xml_converted", "XML converted to JSON"),
    
    /**
     * External API call completed
     */
    EXTERNAL_API_CALLED("external_api_called", "External API called successfully"),
    
    /**
     * Fenergo API call completed
     */
    FENERGO_CALLED("fenergo_called", "Fenergo API called successfully"),
    
    /**
     * Processing completed successfully
     */
    COMPLETED("completed", "Processing completed successfully"),
    
    /**
     * Processing failed
     */
    FAILED("failed", "Processing failed"),
    
    /**
     * Request timed out
     */
    TIMEOUT("timeout", "Request processing timed out"),
    
    /**
     * Request was retried
     */
    RETRY("retry", "Request is being retried");
    
    private final String code;
    private final String description;
    
    ProcessingStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static ProcessingStatus fromCode(String code) {
        for (ProcessingStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown processing status code: " + code);
    }
}

