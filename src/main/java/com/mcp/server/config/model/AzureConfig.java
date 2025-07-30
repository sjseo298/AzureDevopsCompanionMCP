package com.mcp.server.config.model;

/**
 * Configuración específica de Azure DevOps.
 */
public class AzureConfig {
    private String organization;
    private String baseUrl;
    private String defaultProject;
    
    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }
    
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    
    public String getDefaultProject() { return defaultProject; }
    public void setDefaultProject(String defaultProject) { this.defaultProject = defaultProject; }
}
