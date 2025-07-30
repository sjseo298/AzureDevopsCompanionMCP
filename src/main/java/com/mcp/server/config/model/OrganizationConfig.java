package com.mcp.server.config.model;

import java.util.*;

/**
 * Configuración principal de la organización.
 */
public class OrganizationConfig {
    private OrganizationInfo organization;
    private List<ProjectConfig> projects = new ArrayList<>();
    private Map<String, Object> workItemTypes = new HashMap<>();
    private Map<String, Object> naming = new HashMap<>();
    private Map<String, Object> businessRules = new HashMap<>();
    private Map<String, Object> integration = new HashMap<>();
    private Map<String, Object> environment = new HashMap<>();
    
    // Getters y setters
    public OrganizationInfo getOrganization() { return organization; }
    public void setOrganization(OrganizationInfo organization) { this.organization = organization; }
    
    public List<ProjectConfig> getProjects() { return projects; }
    public void setProjects(List<ProjectConfig> projects) { this.projects = projects; }
    
    public Map<String, Object> getWorkItemTypes() { return workItemTypes; }
    public void setWorkItemTypes(Map<String, Object> workItemTypes) { this.workItemTypes = workItemTypes; }
    
    public Map<String, Object> getNaming() { return naming; }
    public void setNaming(Map<String, Object> naming) { this.naming = naming; }
    
    public Map<String, Object> getBusinessRules() { return businessRules; }
    public void setBusinessRules(Map<String, Object> businessRules) { this.businessRules = businessRules; }
    
    public Map<String, Object> getIntegration() { return integration; }
    public void setIntegration(Map<String, Object> integration) { this.integration = integration; }
    
    public Map<String, Object> getEnvironment() { return environment; }
    public void setEnvironment(Map<String, Object> environment) { this.environment = environment; }
}

/**
 * Configuración de proyecto simplificada.
 */
class ProjectConfig {
    private String id;
    private String name;
    private String displayName;
    private String description;
    private boolean isDefault;
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
}
