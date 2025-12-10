package com.adyanta.connect.fenergo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Service for applying data transformations
 */
@Slf4j
@Service
public class DataTransformService {
    
    public Object applyTransform(Object value, String transformType) {
        if (value == null || transformType == null) {
            return value;
        }
        
        String transform = transformType.toLowerCase().trim();
        
        // Handle parameterized transforms: "date:yyyy-MM-dd"
        if (transform.contains(":")) {
            String[] parts = transform.split(":", 2);
            String transformName = parts[0];
            String param = parts.length > 1 ? parts[1] : "";
            
            switch (transformName) {
                case "date":
                    return parseDate(value.toString(), param);
                case "regex":
                    return applyRegex(value.toString(), param);
                default:
                    return applySimpleTransform(value, transform);
            }
        }
        
        return applySimpleTransform(value, transform);
    }
    
    private Object applySimpleTransform(Object value, String transform) {
        String strValue = value.toString();
        
        switch (transform) {
            case "trim":
                return strValue.trim();
            case "upper":
                return strValue.toUpperCase();
            case "lower":
                return strValue.toLowerCase();
            case "capitalize":
                return capitalize(strValue);
            case "number":
                return parseNumber(strValue);
            case "boolean":
                return parseBoolean(strValue);
            default:
                return value;
        }
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + 
               str.substring(1).toLowerCase();
    }
    
    private LocalDate parseDate(String dateStr, String format) {
        try {
            if (format == null || format.isEmpty()) {
                format = "yyyy-MM-dd";
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            return LocalDate.parse(dateStr, formatter);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse date: {} with format: {}", dateStr, format);
            throw new RuntimeException("Invalid date format: " + dateStr, e);
        }
    }
    
    private Number parseNumber(String numStr) {
        try {
            if (numStr.contains(".")) {
                return Double.parseDouble(numStr);
            } else {
                return Long.parseLong(numStr);
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse number: {}", numStr);
            throw new RuntimeException("Invalid number: " + numStr, e);
        }
    }
    
    private Boolean parseBoolean(String boolStr) {
        String lower = boolStr.toLowerCase().trim();
        return "true".equals(lower) || "yes".equals(lower) || "1".equals(lower);
    }
    
    private String applyRegex(String value, String param) {
        // Format: "regex:pattern:replacement"
        String[] parts = param.split(":", 2);
        if (parts.length == 2) {
            String pattern = parts[0];
            String replacement = parts[1];
            return value.replaceAll(pattern, replacement);
        }
        return value;
    }
}

