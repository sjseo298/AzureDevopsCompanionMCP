package com.mcp.server.tools.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Representa un proyecto de Azure DevOps.
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Project(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("description") String description,
    @JsonProperty("state") String state,
    @JsonProperty("url") String url
) {
    
    /**
     * Verifica si el proyecto está bien formado.
     * 
     * @return true si el estado es "wellFormed"
     */
    public boolean isWellFormed() {
        return "wellFormed".equalsIgnoreCase(state);
    }
    
    /**
     * Convierte el proyecto a una representación de texto.
     * 
     * @return representación textual del proyecto
     */
    public String toDisplayString() {
        return String.format("%s (%s) - %s", name, id, state);
    }
}
