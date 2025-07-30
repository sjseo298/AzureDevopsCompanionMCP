package com.mcp.server.utils.discovery;

import java.util.*;

/**
 * Resultado completo de la investigación de campos organizacionales
 */
public class OrganizationFieldInvestigation {
    private String project;
    private Date investigationDate;
    private List<WorkItemTypeDefinition> workItemTypes;
    private List<FieldDefinition> allFields;
    private List<FieldDefinition> customFields;
    private Map<String, List<String>> picklistValues;
    
    public OrganizationFieldInvestigation() {
        this.workItemTypes = new ArrayList<>();
        this.allFields = new ArrayList<>();
        this.customFields = new ArrayList<>();
        this.picklistValues = new HashMap<>();
    }
    
    public String getProject() {
        return project;
    }
    
    public void setProject(String project) {
        this.project = project;
    }
    
    public Date getInvestigationDate() {
        return investigationDate;
    }
    
    public void setInvestigationDate(Date investigationDate) {
        this.investigationDate = investigationDate;
    }
    
    public List<WorkItemTypeDefinition> getWorkItemTypes() {
        return workItemTypes;
    }
    
    public void setWorkItemTypes(List<WorkItemTypeDefinition> workItemTypes) {
        this.workItemTypes = workItemTypes;
    }
    
    public void addWorkItemTypeDefinition(WorkItemTypeDefinition definition) {
        this.workItemTypes.add(definition);
    }
    
    public List<FieldDefinition> getAllFields() {
        return allFields;
    }
    
    public void setAllFields(List<FieldDefinition> allFields) {
        this.allFields = allFields;
    }
    
    public List<FieldDefinition> getCustomFields() {
        return customFields;
    }
    
    public void setCustomFields(List<FieldDefinition> customFields) {
        this.customFields = customFields;
    }
    
    public Map<String, List<String>> getPicklistValues() {
        return picklistValues;
    }
    
    public void setPicklistValues(Map<String, List<String>> picklistValues) {
        this.picklistValues = picklistValues;
    }
    
    /**
     * Genera un resumen de la investigación
     */
    public InvestigationSummary getSummary() {
        InvestigationSummary summary = new InvestigationSummary();
        summary.setTotalWorkItemTypes(workItemTypes.size());
        summary.setTotalFields(allFields.size());
        summary.setCustomFieldsFound(customFields.size());
        summary.setPicklistFieldsFound(picklistValues.size());
        
        long dateFieldsFound = allFields.stream()
                .filter(field -> "dateTime".equals(field.getType()))
                .count();
        summary.setDateFieldsFound((int) dateFieldsFound);
        
        return summary;
    }
    
    /**
     * Clase interna para resumen estadístico
     */
    public static class InvestigationSummary {
        private int totalWorkItemTypes;
        private int totalFields;
        private int customFieldsFound;
        private int picklistFieldsFound;
        private int dateFieldsFound;
        
        public int getTotalWorkItemTypes() {
            return totalWorkItemTypes;
        }
        
        public void setTotalWorkItemTypes(int totalWorkItemTypes) {
            this.totalWorkItemTypes = totalWorkItemTypes;
        }
        
        public int getTotalFields() {
            return totalFields;
        }
        
        public void setTotalFields(int totalFields) {
            this.totalFields = totalFields;
        }
        
        public int getCustomFieldsFound() {
            return customFieldsFound;
        }
        
        public void setCustomFieldsFound(int customFieldsFound) {
            this.customFieldsFound = customFieldsFound;
        }
        
        public int getPicklistFieldsFound() {
            return picklistFieldsFound;
        }
        
        public void setPicklistFieldsFound(int picklistFieldsFound) {
            this.picklistFieldsFound = picklistFieldsFound;
        }
        
        public int getDateFieldsFound() {
            return dateFieldsFound;
        }
        
        public void setDateFieldsFound(int dateFieldsFound) {
            this.dateFieldsFound = dateFieldsFound;
        }
    }
}
