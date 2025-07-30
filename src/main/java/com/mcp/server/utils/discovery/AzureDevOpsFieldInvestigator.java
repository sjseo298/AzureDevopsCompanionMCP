package com.mcp.server.utils.discovery;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidad para investigar campos personalizados y sus valores permitidos en Azure DevOps.
 * Reemplaza los scripts de shell para hacer todo desde Java de manera más robusta.
 * Usa parsing JSON simple sin dependencias externas.
 */
public class AzureDevOpsFieldInvestigator {
    
    private String organization;
    private String personalAccessToken;
    
    private final HttpClient httpClient;
    
    public AzureDevOpsFieldInvestigator(String organization, String personalAccessToken) {
        this.organization = organization;
        this.personalAccessToken = personalAccessToken;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }
    
    /**
     * Investiga un tipo de work item específico y obtiene sus campos con valores permitidos
     */
    public WorkItemTypeDefinition investigateWorkItemType(String project, String workItemTypeName) {
        System.out.println("🔍 Investigando work item type '" + workItemTypeName + "' en proyecto '" + project + "'");
        
        try {
            String encodedTypeName = java.net.URLEncoder.encode(workItemTypeName, StandardCharsets.UTF_8);
            String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/workitemtypes/%s?api-version=7.1", 
                    organization, project, encodedTypeName);
            
            String response = makeApiRequest(url);
            if (response == null || !response.contains("\"fieldInstances\"")) {
                System.out.println("❌ No se pudo obtener definición del work item type");
                return null;
            }
            
            return parseWorkItemTypeDefinition(workItemTypeName, response);
            
        } catch (Exception e) {
            System.out.println("❌ Error investigando work item type '" + workItemTypeName + "': " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Obtiene todos los campos del proyecto con sus metadatos
     */
    public List<FieldDefinition> investigateProjectFields(String project) {
        System.out.println("🔧 Obteniendo todos los campos del proyecto '" + project + "'");
        
        try {
            String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/fields?api-version=7.1", 
                    organization, project);
            
            String response = makeApiRequest(url);
            if (response == null || !response.contains("\"value\"")) {
                System.out.println("❌ No se pudieron obtener campos del proyecto");
                return Collections.emptyList();
            }
            
            List<FieldDefinition> fields = parseFieldDefinitions(response);
            System.out.println("✅ Encontrados " + fields.size() + " campos en total");
            return fields;
            
        } catch (Exception e) {
            System.out.println("❌ Error obteniendo campos del proyecto: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Obtiene valores permitidos para campos de tipo picklist
     */
    public List<String> investigatePicklistValues(String project, String fieldReferenceName, String picklistId) {
        System.out.println("🎯 Obteniendo valores de picklist para campo: " + fieldReferenceName);
        
        // Intentar múltiples endpoints como en el script original
        List<String> values = tryGetPicklistFromProcesses(picklistId);
        if (!values.isEmpty()) {
            return values;
        }
        
        values = tryGetPicklistFromProjectContext(project, picklistId);
        if (!values.isEmpty()) {
            return values;
        }
        
        values = tryGetPicklistFromFieldEndpoint(project, fieldReferenceName);
        if (!values.isEmpty()) {
            return values;
        }
        
        System.out.println("⚠️ No se pudieron obtener valores de picklist");
        return Collections.emptyList();
    }
    
    // Métodos auxiliares para requests HTTP
    
    private String makeApiRequest(String url) {
        try {
            String auth = Base64.getEncoder().encodeToString((":" + personalAccessToken).getBytes());
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + auth)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                System.out.println("❌ API request failed with status: " + response.statusCode());
                return null;
            }
            
        } catch (Exception e) {
            System.out.println("❌ Error making API request: " + e.getMessage());
            return null;
        }
    }
    
    // Métodos de parsing usando regex simple
    
    private WorkItemTypeDefinition parseWorkItemTypeDefinition(String typeName, String jsonResponse) {
        WorkItemTypeDefinition definition = new WorkItemTypeDefinition(typeName);
        
        // Buscar fieldInstances y sus allowedValues usando regex
        Pattern fieldPattern = Pattern.compile("\"referenceName\"\\s*:\\s*\"([^\"]+)\"[^}]*\"allowedValues\"\\s*:\\s*\\[([^\\]]+)\\]", Pattern.DOTALL);
        Matcher matcher = fieldPattern.matcher(jsonResponse);
        
        while (matcher.find()) {
            String referenceName = matcher.group(1);
            String allowedValuesStr = matcher.group(2);
            
            List<String> values = new ArrayList<>();
            Pattern valuePattern = Pattern.compile("\"([^\"]+)\"");
            Matcher valueMatcher = valuePattern.matcher(allowedValuesStr);
            
            while (valueMatcher.find()) {
                values.add(valueMatcher.group(1));
            }
            
            if (!values.isEmpty()) {
                definition.addFieldWithValues(referenceName, values);
            }
        }
        
        return definition;
    }
    
    private List<FieldDefinition> parseFieldDefinitions(String jsonResponse) {
        List<FieldDefinition> fields = new ArrayList<>();
        
        // Buscar cada campo en el array "value" usando regex
        Pattern fieldPattern = Pattern.compile("\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\"[^}]*\"referenceName\"\\s*:\\s*\"([^\"]+)\"[^}]*\"type\"\\s*:\\s*\"([^\"]+)\"[^}]*\\}", Pattern.DOTALL);
        Matcher matcher = fieldPattern.matcher(jsonResponse);
        
        while (matcher.find()) {
            try {
                String name = matcher.group(1);
                String referenceName = matcher.group(2);
                String type = matcher.group(3);
                
                FieldDefinition field = new FieldDefinition(name, referenceName, type);
                
                // Buscar picklistId si existe en este campo
                String fieldBlock = matcher.group(0);
                String picklistId = extractJsonValue(fieldBlock, "picklistId");
                if (picklistId != null) {
                    field.setPicklistId(picklistId);
                }
                
                fields.add(field);
            } catch (Exception e) {
                System.err.println("Error parsing field definition: " + e.getMessage());
            }
        }
        
        return fields;
    }
    
    private String extractJsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    // Métodos para obtener valores de picklist usando múltiples estrategias
    
    private List<String> tryGetPicklistFromProcesses(String picklistId) {
        try {
            String url = String.format("https://dev.azure.com/%s/_apis/work/processes/lists/%s?api-version=7.1", 
                    organization, picklistId);
            
            String response = makeApiRequest(url);
            if (response != null && response.contains("\"items\"")) {
                return extractArrayValues(response, "items");
            }
        } catch (Exception e) {
            // Continuar silenciosamente a la siguiente estrategia
        }
        return Collections.emptyList();
    }
    
    private List<String> tryGetPicklistFromProjectContext(String project, String picklistId) {
        try {
            String url = String.format("https://dev.azure.com/%s/%s/_apis/work/processes/lists/%s?api-version=7.1", 
                    organization, project, picklistId);
            
            String response = makeApiRequest(url);
            if (response != null && response.contains("\"items\"")) {
                return extractArrayValues(response, "items");
            }
        } catch (Exception e) {
            // Continuar silenciosamente a la siguiente estrategia
        }
        return Collections.emptyList();
    }
    
    private List<String> tryGetPicklistFromFieldEndpoint(String project, String fieldReferenceName) {
        try {
            String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/fields/%s/allowedValues?api-version=7.1", 
                    organization, project, fieldReferenceName);
            
            String response = makeApiRequest(url);
            if (response != null && response.contains("\"value\"")) {
                return extractArrayValues(response, "value");
            }
        } catch (Exception e) {
            // Continuar silenciosamente
        }
        return Collections.emptyList();
    }
    
    private List<String> extractArrayValues(String json, String arrayKey) {
        List<String> values = new ArrayList<>();
        
        Pattern arrayPattern = Pattern.compile("\"" + arrayKey + "\"\\s*:\\s*\\[([^\\]]+)\\]");
        Matcher arrayMatcher = arrayPattern.matcher(json);
        
        if (arrayMatcher.find()) {
            String arrayContent = arrayMatcher.group(1);
            Pattern valuePattern = Pattern.compile("\"([^\"]+)\"");
            Matcher valueMatcher = valuePattern.matcher(arrayContent);
            
            while (valueMatcher.find()) {
                values.add(valueMatcher.group(1));
            }
        }
        
        return values;
    }
}
