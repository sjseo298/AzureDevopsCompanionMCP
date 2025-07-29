package com.mcp.server.tools.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Representa un equipo de Azure DevOps.
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Team(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("description") String description,
    @JsonProperty("url") String url
) {
    
    /**
     * Convierte el equipo a una representación de texto.
     * 
     * @return representación textual del equipo
     */
    public String toDisplayString() {
        String desc = description != null && !description.trim().isEmpty() 
            ? " - " + description 
            : "";
        return String.format("%s (%s)%s", name, id, desc);
    }
}
