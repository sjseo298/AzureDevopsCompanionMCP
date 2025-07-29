package com.mcp.server.tools.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Representa el resultado de una consulta WIQL.
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WiqlQueryResult(
    @JsonProperty("queryType") String queryType,
    @JsonProperty("columns") List<Column> columns,
    @JsonProperty("workItems") List<WorkItemReference> workItems
) {
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Column(
        @JsonProperty("referenceName") String referenceName,
        @JsonProperty("name") String name
    ) {}
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WorkItemReference(
        @JsonProperty("id") Integer id,
        @JsonProperty("url") String url
    ) {}
    
    /**
     * Obtiene la lista de IDs de work items encontrados.
     * 
     * @return lista de IDs
     */
    public List<Integer> getWorkItemIds() {
        return workItems != null 
            ? workItems.stream().map(WorkItemReference::id).toList()
            : List.of();
    }
    
    /**
     * Verifica si la consulta encontró resultados.
     * 
     * @return true si hay work items encontrados
     */
    public boolean hasResults() {
        return workItems != null && !workItems.isEmpty();
    }
    
    /**
     * Obtiene el número de work items encontrados.
     * 
     * @return cantidad de work items
     */
    public int getResultCount() {
        return workItems != null ? workItems.size() : 0;
    }
}
