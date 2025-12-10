package com.adyanta.connect.fenergo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import java.util.Map;

/**
 * Service for evaluating conditional mappings
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConditionalEvaluator {
    
    private final XPathExtractor xpathExtractor;
    
    public boolean evaluate(Map<String, Object> condition, Document xmlDoc) {
        if (condition == null || condition.isEmpty()) {
            return true; // No condition means always true
        }
        
        String type = (String) condition.get("type");
        
        switch (type) {
            case "xpath":
                return evaluateXPathCondition(condition, xmlDoc);
            case "value":
                return evaluateValueCondition(condition, xmlDoc);
            case "and":
                return evaluateAndCondition(condition, xmlDoc);
            case "or":
                return evaluateOrCondition(condition, xmlDoc);
            default:
                log.warn("Unknown condition type: {}", type);
                return true;
        }
    }
    
    private boolean evaluateXPathCondition(Map<String, Object> condition, Document xmlDoc) {
        String expression = (String) condition.get("expression");
        if (expression == null) {
            return true;
        }
        
        try {
            Object result = xpathExtractor.extractValue(xmlDoc, expression, false);
            return result != null && !result.toString().isEmpty();
        } catch (Exception e) {
            log.warn("Error evaluating XPath condition: {}", expression, e);
            return false;
        }
    }
    
    private boolean evaluateValueCondition(Map<String, Object> condition, Document xmlDoc) {
        String field = (String) condition.get("field");
        String operator = (String) condition.get("operator");
        Object expectedValue = condition.get("value");
        
        if (field == null || operator == null) {
            return true;
        }
        
        try {
            Object actualValue = xpathExtractor.extractValue(xmlDoc, field, false);
            if (actualValue == null) {
                return false;
            }
            
            return compareValues(actualValue.toString(), operator, expectedValue);
        } catch (Exception e) {
            log.warn("Error evaluating value condition: {}", field, e);
            return false;
        }
    }
    
    private boolean evaluateAndCondition(Map<String, Object> condition, Document xmlDoc) {
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> conditions = 
            (java.util.List<Map<String, Object>>) condition.get("conditions");
        
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        
        return conditions.stream()
            .allMatch(c -> evaluate(c, xmlDoc));
    }
    
    private boolean evaluateOrCondition(Map<String, Object> condition, Document xmlDoc) {
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> conditions = 
            (java.util.List<Map<String, Object>>) condition.get("conditions");
        
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        
        return conditions.stream()
            .anyMatch(c -> evaluate(c, xmlDoc));
    }
    
    private boolean compareValues(String actual, String operator, Object expected) {
        if (expected == null) {
            return false;
        }
        
        String expectedStr = expected.toString();
        
        switch (operator) {
            case "equals":
            case "==":
                return actual.equals(expectedStr);
            case "!=":
            case "notEquals":
                return !actual.equals(expectedStr);
            case ">":
                return compareNumbers(actual, expectedStr) > 0;
            case ">=":
                return compareNumbers(actual, expectedStr) >= 0;
            case "<":
                return compareNumbers(actual, expectedStr) < 0;
            case "<=":
                return compareNumbers(actual, expectedStr) <= 0;
            case "contains":
                return actual.contains(expectedStr);
            case "startsWith":
                return actual.startsWith(expectedStr);
            case "endsWith":
                return actual.endsWith(expectedStr);
            default:
                log.warn("Unknown operator: {}", operator);
                return false;
        }
    }
    
    private int compareNumbers(String a, String b) {
        try {
            double numA = Double.parseDouble(a);
            double numB = Double.parseDouble(b);
            return Double.compare(numA, numB);
        } catch (NumberFormatException e) {
            return a.compareTo(b);
        }
    }
}

