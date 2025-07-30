package com.mcp.server.utils.discovery;

/**
 * Definición de un campo de Azure DevOps con sus metadatos
 */
public class FieldDefinition {
    private String name;
    private String referenceName;
    private String type;
    private boolean readOnly;
    private boolean canSortBy;
    private boolean isQueryable;
    private String picklistId;
    
    public FieldDefinition() {}
    
    public FieldDefinition(String name, String referenceName, String type) {
        this.name = name;
        this.referenceName = referenceName;
        this.type = type;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getReferenceName() {
        return referenceName;
    }
    
    public void setReferenceName(String referenceName) {
        this.referenceName = referenceName;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public boolean isReadOnly() {
        return readOnly;
    }
    
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }
    
    public boolean isCanSortBy() {
        return canSortBy;
    }
    
    public void setCanSortBy(boolean canSortBy) {
        this.canSortBy = canSortBy;
    }
    
    public boolean isQueryable() {
        return isQueryable;
    }
    
    public void setIsQueryable(boolean isQueryable) {
        this.isQueryable = isQueryable;
    }
    
    public String getPicklistId() {
        return picklistId;
    }
    
    public void setPicklistId(String picklistId) {
        this.picklistId = picklistId;
    }
}
