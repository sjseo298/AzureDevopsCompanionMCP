package com.mcp.server.tools.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Modelos de respuesta genéricos para las APIs de Azure DevOps.
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ApiResponse {
    
    /**
     * Respuesta genérica que contiene una lista de elementos.
     *
     * @param <T> tipo de elementos en la lista
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ListResponse<T>(
        @JsonProperty("count") Integer count,
        @JsonProperty("value") List<T> value
    ) {
        
        /**
         * Verifica si la respuesta tiene elementos.
         * 
         * @return true si hay elementos
         */
        public boolean hasItems() {
            return value != null && !value.isEmpty();
        }
        
        /**
         * Obtiene el número de elementos.
         * 
         * @return cantidad de elementos
         */
        public int getItemCount() {
            return value != null ? value.size() : 0;
        }
        
        /**
         * Obtiene la lista de elementos o una lista vacía.
         * 
         * @return lista de elementos
         */
        public List<T> getItems() {
            return value != null ? value : List.of();
        }
    }
    
    /**
     * Respuesta para consultas WIQL.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WiqlResponse(
        @JsonProperty("query") String query
    ) {}
    
    // Constructor privado para evitar instanciación
    private ApiResponse() {}
}
