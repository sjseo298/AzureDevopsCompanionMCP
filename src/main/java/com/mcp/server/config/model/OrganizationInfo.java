package com.mcp.server.config.model;

/**
 * Información básica de la organización.
 */
public class OrganizationInfo {
    private String name;
    private String displayName;
    private String description;
    private AzureConfig azure;
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public AzureConfig getAzure() { return azure; }
    public void setAzure(AzureConfig azure) { this.azure = azure; }
}
