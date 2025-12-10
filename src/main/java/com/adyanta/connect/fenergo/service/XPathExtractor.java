package com.adyanta.connect.fenergo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for extracting values from XML using XPath expressions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XPathExtractor {
    
    private final XPathFactory xpathFactory;
    private final DocumentBuilderFactory documentBuilderFactory;
    
    public XPathExtractor() {
        this.xpathFactory = XPathFactory.newInstance();
        this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
        this.documentBuilderFactory.setNamespaceAware(true);
    }
    
    public Object extractValue(Document xmlDoc, String xpath, boolean isMulti) {
        try {
            XPath xpathObj = xpathFactory.newXPath();
            XPathExpression expr = xpathObj.compile(xpath);
            
            if (isMulti) {
                NodeList nodes = (NodeList) expr.evaluate(xmlDoc, XPathConstants.NODESET);
                return extractNodeList(nodes);
            } else {
                Node node = (Node) expr.evaluate(xmlDoc, XPathConstants.NODE);
                if (node == null) {
                    // Try as string value
                    String value = (String) expr.evaluate(xmlDoc, XPathConstants.STRING);
                    return value != null && !value.trim().isEmpty() ? value.trim() : null;
                }
                return extractNodeValue(node);
            }
        } catch (Exception e) {
            log.error("Error extracting XPath: {}", xpath, e);
            throw new RuntimeException("Failed to extract XPath " + xpath + ": " + e.getMessage(), e);
        }
    }
    
    private List<String> extractNodeList(NodeList nodes) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            String value = extractNodeValue(nodes.item(i));
            if (value != null && !value.isEmpty()) {
                values.add(value);
            }
        }
        return values.isEmpty() ? null : values;
    }
    
    private String extractNodeValue(Node node) {
        if (node == null) {
            return null;
        }
        
        if (node.getNodeType() == Node.TEXT_NODE) {
            return node.getTextContent().trim();
        } else if (node.getNodeType() == Node.ELEMENT_NODE) {
            return node.getTextContent().trim();
        } else if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
            return node.getNodeValue().trim();
        }
        return null;
    }
}

