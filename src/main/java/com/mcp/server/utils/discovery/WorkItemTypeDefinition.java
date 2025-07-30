package com.mcp.server.utils.discovery;

import java.util.*;

/**
 * Definición de un tipo de work item con sus campos y valores permitidos
 */
public class WorkItemTypeDefinition {
    
    private String typeName;
    private String description;
    private Map<String, List<String>> fieldsWithValues;
    
    public WorkItemTypeDefinition() {
        this.fieldsWithValues = new HashMap<>();
    }
    
    public WorkItemTypeDefinition(String typeName) {
        this.typeName = typeName;
        this.fieldsWithValues = new HashMap<>();
    }
    
    public void addFieldWithValues(String fieldReferenceName, List<String> allowedValues) {
        fieldsWithValues.put(fieldReferenceName, new ArrayList<>(allowedValues));
    }
    
    public String getTypeName() {
        return typeName;
    }
    
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Map<String, List<String>> getFieldsWithValues() {
        return new HashMap<>(fieldsWithValues);
    }
    
    public void setFieldsWithValues(Map<String, List<String>> fieldsWithValues) {
        this.fieldsWithValues = new HashMap<>(fieldsWithValues);
    }
    
    public List<String> getAllowedValuesForField(String fieldReferenceName) {
        return fieldsWithValues.getOrDefault(fieldReferenceName, Collections.emptyList());
    }
    
    public boolean hasFieldWithValues(String fieldReferenceName) {
        return fieldsWithValues.containsKey(fieldReferenceName) && 
               !fieldsWithValues.get(fieldReferenceName).isEmpty();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("WorkItemTypeDefinition{typeName='").append(typeName).append("'");
        sb.append(", fieldsWithValues=").append(fieldsWithValues.size()).append(" fields}");
        return sb.toString();
    }
}
