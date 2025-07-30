package com.mcp.server.config.model;

import java.util.*;

/**
 * Configuración de mapeo de campos customizados.
 */
public class FieldMappingConfig {
    private List<Map<String, Object>> fieldMappings = new ArrayList<>();
    private Map<String, Object> conversions = new HashMap<>();
    private Map<String, List<String>> workItemTypeFields = new HashMap<>();
    
    // Getters y setters
    public List<Map<String, Object>> getFieldMappings() { return fieldMappings; }
    public void setFieldMappings(List<Map<String, Object>> fieldMappings) { this.fieldMappings = fieldMappings; }
    
    public Map<String, Object> getConversions() { return conversions; }
    public void setConversions(Map<String, Object> conversions) { this.conversions = conversions; }
    
    public Map<String, List<String>> getWorkItemTypeFields() { return workItemTypeFields; }
    public void setWorkItemTypeFields(Map<String, List<String>> workItemTypeFields) { this.workItemTypeFields = workItemTypeFields; }
}

/**
 * Mapeo de campo individual.
 */
class FieldMapping {
    private String logicalName;
    private String displayName;
    private String azureFieldName;
    private String guid;
    private String type;
    private String description;
    private Object defaultValue;
    private boolean required;
    private List<String> allowedValues = new ArrayList<>();
    private String helpText;
    private FieldConversion conversion;
    
    public String getLogicalName() { return logicalName; }
    public void setLogicalName(String logicalName) { this.logicalName = logicalName; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getAzureFieldName() { return azureFieldName; }
    public void setAzureFieldName(String azureFieldName) { this.azureFieldName = azureFieldName; }
    
    public String getGuid() { return guid; }
    public void setGuid(String guid) { this.guid = guid; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Object getDefaultValue() { return defaultValue; }
    public void setDefaultValue(Object defaultValue) { this.defaultValue = defaultValue; }
    
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    
    public List<String> getAllowedValues() { return allowedValues; }
    public void setAllowedValues(List<String> allowedValues) { this.allowedValues = allowedValues; }
    
    public String getHelpText() { return helpText; }
    public void setHelpText(String helpText) { this.helpText = helpText; }
    
    public FieldConversion getConversion() { return conversion; }
    public void setConversion(FieldConversion conversion) { this.conversion = conversion; }
}

/**
 * Configuración de conversión de campo.
 */
class FieldConversion {
    private String type;
    private Map<String, Object> mapping = new HashMap<>();
    private String format;
    private Object defaultValue;
    private ConversionRules rules;
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public Map<String, Object> getMapping() { return mapping; }
    public void setMapping(Map<String, Object> mapping) { this.mapping = mapping; }
    
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    
    public Object getDefaultValue() { return defaultValue; }
    public void setDefaultValue(Object defaultValue) { this.defaultValue = defaultValue; }
    
    public ConversionRules getRules() { return rules; }
    public void setRules(ConversionRules rules) { this.rules = rules; }
}

/**
 * Reglas de conversión.
 */
class ConversionRules {
    private String trueValue;
    private String falseValue;
    private boolean caseSensitive;
    
    public String getTrueValue() { return trueValue; }
    public void setTrueValue(String trueValue) { this.trueValue = trueValue; }
    
    public String getFalseValue() { return falseValue; }
    public void setFalseValue(String falseValue) { this.falseValue = falseValue; }
    
    public boolean isCaseSensitive() { return caseSensitive; }
    public void setCaseSensitive(boolean caseSensitive) { this.caseSensitive = caseSensitive; }
}
