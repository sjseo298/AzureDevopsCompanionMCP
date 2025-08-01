package com.mcp.server.utils.http;

import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilidad centralizada para realizar peticiones HTTP a la API de Azure DevOps.
 * Proporciona métodos de conveniencia para peticiones GET y construcción de URLs.
 */
public class AzureDevOpsHttpUtil {
    
    private final AzureDevOpsClient azureDevOpsClient;
    private final String organization;
    
    public AzureDevOpsHttpUtil(AzureDevOpsClient azureDevOpsClient, String organization) {
        this.azureDevOpsClient = azureDevOpsClient;
        this.organization = organization;
    }
    
    /**
     * Realiza una petición GET a un endpoint específico de Azure DevOps.
     * 
     * @param endpoint endpoint relativo (ej: "/project/_apis/wit/fields")
     * @param queryParams parámetros de consulta opcionales
     * @return respuesta JSON como string
     */
    public String makeGetRequest(String endpoint, Map<String, String> queryParams) {
        try {
            return azureDevOpsClient.makeGenericApiRequest(endpoint, queryParams);
        } catch (Exception e) {
        System.err.println("Error realizando petición GET a " + endpoint + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Realiza una petición GET simple a un endpoint de Azure DevOps.
     * 
     * @param endpoint endpoint relativo
     * @return respuesta JSON como string
     */
    public String makeGetRequest(String endpoint) {
        return makeGetRequest(endpoint, getDefaultQueryParams());
    }
    
    /**
     * Construye un endpoint para obtener campos de un proyecto específico.
     * 
     * @param project nombre del proyecto
     * @return endpoint relativo para campos del proyecto
     */
    public String buildFieldsEndpoint(String project) {
        return String.format("/%s/_apis/wit/fields", project);
    }
    
    /**
     * Construye un endpoint para obtener definición de un campo específico.
     * 
     * @param project nombre del proyecto
     * @param fieldReferenceName nombre de referencia del campo
     * @return endpoint relativo para definición del campo
     */
    public String buildFieldDefinitionEndpoint(String project, String fieldReferenceName) {
        String encodedFieldName = URLEncoder.encode(fieldReferenceName, StandardCharsets.UTF_8);
        return String.format("/%s/_apis/wit/fields/%s", project, encodedFieldName);
    }
    
    /**
     * Construye un endpoint para obtener valores permitidos de un campo.
     * 
     * @param project nombre del proyecto
     * @param fieldReferenceName nombre de referencia del campo
     * @return endpoint relativo para valores permitidos del campo
     */
    public String buildFieldAllowedValuesEndpoint(String project, String fieldReferenceName) {
        String encodedFieldName = URLEncoder.encode(fieldReferenceName, StandardCharsets.UTF_8);
        return String.format("/%s/_apis/wit/fields/%s/allowedValues", project, encodedFieldName);
    }
    
    /**
     * Construye un endpoint para obtener información de un picklist.
     * 
     * @param picklistId ID del picklist
     * @return endpoint relativo para información del picklist
     */
    public String buildPicklistEndpoint(String picklistId) {
        return String.format("/_apis/work/processes/lists/%s", picklistId);
    }
    
    /**
     * Construye un endpoint para obtener información de un picklist con contexto de proyecto.
     * 
     * @param project nombre del proyecto
     * @param picklistId ID del picklist
     * @return endpoint relativo para información del picklist con contexto de proyecto
     */
    public String buildProjectPicklistEndpoint(String project, String picklistId) {
        return String.format("/%s/_apis/work/processes/lists/%s", project, picklistId);
    }
    
    /**
     * Construye un endpoint para consultas WIQL.
     * 
     * @param project nombre del proyecto
     * @param maxResults número máximo de resultados
     * @return endpoint relativo para consultas WIQL
     */
    public String buildWiqlQueryEndpoint(String project, int maxResults) {
        return String.format("/%s/_apis/wit/wiql?$top=%d", project, maxResults);
    }
    
    /**
     * Construye un endpoint para obtener un work item específico.
     * 
     * @param project nombre del proyecto
     * @param workItemId ID del work item
     * @param fields campos específicos a obtener (opcional)
     * @return endpoint relativo para el work item
     */
    public String buildWorkItemEndpoint(String project, Integer workItemId, String fields) {
        String endpoint = String.format("/%s/_apis/wit/workitems/%d", project, workItemId);
        if (fields != null && !fields.trim().isEmpty()) {
            endpoint += "?fields=" + URLEncoder.encode(fields, StandardCharsets.UTF_8);
        }
        return endpoint;
    }
    
    /**
     * Construye URL completa para Azure DevOps usando el endpoint relativo.
     * 
     * @param relativeEndpoint endpoint relativo
     * @return URL completa
     */
    public String buildFullUrl(String relativeEndpoint) {
        if (relativeEndpoint.startsWith("/")) {
            return String.format("https://dev.azure.com/%s%s", organization, relativeEndpoint);
        } else {
            return String.format("https://dev.azure.com/%s/%s", organization, relativeEndpoint);
        }
    }
    
    /**
     * Obtiene los parámetros de consulta por defecto para las peticiones.
     * 
     * @return mapa con parámetros por defecto
     */
    public Map<String, String> getDefaultQueryParams() {
        Map<String, String> params = new HashMap<>();
        params.put("api-version", "7.1");
        return params;
    }
    
    /**
     * Verifica si una respuesta contiene datos válidos.
     * 
     * @param response respuesta a verificar
     * @return true si la respuesta es válida
     */
    public boolean isValidResponse(String response) {
        return response != null && 
               !response.trim().isEmpty() && 
               !response.contains("\"count\":0") &&
               !response.contains("\"value\":[]");
    }
    
    /**
     * Extrae el código de error de una respuesta de error de Azure DevOps.
     * 
     * @param response respuesta que contiene error
     * @return código de error o null si no se encuentra
     */
    public String extractErrorCode(String response) {
        if (response == null) return null;
        
        // Buscar patrón típico de error de Azure DevOps
        int errorStart = response.indexOf("\"code\":\"");
        if (errorStart != -1) {
            errorStart += 8; // longitud de "\"code\":\""
            int errorEnd = response.indexOf("\"", errorStart);
            if (errorEnd != -1) {
                return response.substring(errorStart, errorEnd);
            }
        }
        
        return null;
    }
}
