package com.mcp.server.utils.discovery;

import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.tools.azuredevops.model.WiqlQueryResult;
import com.mcp.server.tools.azuredevops.model.WorkItem;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidad especializada para investigar valores de picklist y campos con valores permitidos
 * en Azure DevOps. Centraliza toda la lógica de obtención de valores permitidos para campos.
 */
public class AzureDevOpsPicklistInvestigator {
    
    private final AzureDevOpsClient azureDevOpsClient;
    
    public AzureDevOpsPicklistInvestigator(AzureDevOpsClient azureDevOpsClient) {
        this.azureDevOpsClient = azureDevOpsClient;
    }
    
    /**
     * Obtiene valores permitidos para un campo específico usando múltiples estrategias.
     * 
     * @param project nombre del proyecto
     * @param workItemType tipo de work item (opcional)
     * @param fieldReferenceName nombre de referencia del campo (ej: Custom.TipoHistoria)
     * @return lista de valores permitidos
     */
    public List<String> getFieldAllowedValues(String project, String workItemType, String fieldReferenceName) {
        if (fieldReferenceName == null || fieldReferenceName.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> allowedValues = new ArrayList<>();
        
        try {
            // Estrategia 1: Obtener definición completa del campo
            Map<String, Object> fieldDefinition = getCompleteFieldDefinition(project, fieldReferenceName);
            
            // Verificar si el campo tiene picklistId
            String picklistId = (String) fieldDefinition.get("picklistId");
            if (picklistId != null && !picklistId.trim().isEmpty()) {
                allowedValues = getPicklistValues(project, fieldReferenceName, picklistId);
                if (!allowedValues.isEmpty()) {
                    return allowedValues;
                }
            }
            
            // Estrategia 2: Valores directos en la definición del campo
            if (fieldDefinition.containsKey("allowedValues")) {
                Object allowedValuesObj = fieldDefinition.get("allowedValues");
                if (allowedValuesObj instanceof List<?>) {
                    @SuppressWarnings("unchecked")
                    List<String> values = (List<String>) allowedValuesObj;
                    allowedValues.addAll(values);
                    if (!allowedValues.isEmpty()) {
                        return allowedValues;
                    }
                }
            }
            
            // Estrategia 3: Para campos de estado
            if ("System.State".equals(fieldReferenceName) && workItemType != null) {
                allowedValues = getWorkItemStateValues(project, workItemType);
                if (!allowedValues.isEmpty()) {
                    return allowedValues;
                }
            }
            
            // Estrategia 4: Extraer valores únicos de work items existentes (último recurso)
            if (workItemType != null) {
                allowedValues = extractUniqueFieldValuesFromExistingWorkItems(project, workItemType, fieldReferenceName);
            }
            
        } catch (Exception e) {
            // Error silencioso - retornar lista vacía
        }
        
        return allowedValues;
    }
    
    /**
     * Obtiene valores de picklist usando múltiples estrategias de endpoints.
     */
    public List<String> getPicklistValues(String project, String fieldReferenceName, String picklistId) {
        // Estrategia 1: Endpoint de procesos organizacionales
        List<String> values = tryGetPicklistFromProcesses(picklistId);
        if (!values.isEmpty()) {
            return values;
        }
        
        // Estrategia 2: Endpoint de procesos con contexto de proyecto
        values = tryGetPicklistFromProjectContext(project, picklistId);
        if (!values.isEmpty()) {
            return values;
        }
        
        // Estrategia 3: Endpoint específico de campo
        values = tryGetPicklistFromFieldEndpoint(project, fieldReferenceName);
        if (!values.isEmpty()) {
            return values;
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Obtiene la definición completa de un campo incluyendo información de picklist.
     */
    public Map<String, Object> getCompleteFieldDefinition(String project, String fieldReferenceName) {
        Map<String, Object> fieldDefinition = new HashMap<>();
        
        try {
            String encodedFieldName = URLEncoder.encode(fieldReferenceName, StandardCharsets.UTF_8);
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("api-version", "7.1");
            
            String endpoint = String.format("/%s/_apis/wit/fields/%s", project, encodedFieldName);
            String response = azureDevOpsClient.makeGenericApiRequest(endpoint, queryParams);
            
            if (response != null) {
                fieldDefinition = parseCompleteFieldDefinition(response);
            }
            
        } catch (Exception e) {
            // Error silencioso
        }
        
        return fieldDefinition;
    }
    
    /**
     * Extrae valores únicos de un campo específico consultando work items existentes.
     */
    public List<String> extractUniqueFieldValuesFromExistingWorkItems(String project, String workItemType, String fieldReferenceName) {
        Set<String> uniqueValues = new HashSet<>();
        
        try {
            // Consulta WIQL para obtener muestras de work items de este tipo
            String wiqlQuery = String.format(
                "SELECT [System.Id], [%s] FROM WorkItems WHERE [System.WorkItemType] = '%s' AND [%s] <> '' ORDER BY [System.Id] DESC",
                fieldReferenceName, workItemType, fieldReferenceName
            );
            
            WiqlQueryResult result = azureDevOpsClient.executeWiqlQuery(project, null, wiqlQuery);
            
            if (result != null && result.workItems() != null && !result.workItems().isEmpty()) {
                // Limitar a máximo 50 work items para evitar sobrecarga
                List<Integer> workItemIds = result.getWorkItemIds();
                int maxItems = Math.min(workItemIds.size(), 50);
                
                for (int i = 0; i < maxItems; i++) {
                    Integer workItemId = workItemIds.get(i);
                    try {
                        WorkItem workItem = azureDevOpsClient.getWorkItem(project, workItemId, null, null);
                        if (workItem != null && workItem.fields() != null) {
                            Object fieldValue = workItem.fields().get(fieldReferenceName);
                            if (fieldValue != null && !fieldValue.toString().trim().isEmpty()) {
                                uniqueValues.add(fieldValue.toString().trim());
                            }
                        }
                    } catch (Exception e) {
                        // Continuar con el siguiente work item
                    }
                }
            }
            
        } catch (Exception e) {
            // Error silencioso
        }
        
        List<String> result = new ArrayList<>(uniqueValues);
        result.sort(String::compareToIgnoreCase);
        return result;
    }
    
    /**
     * Obtiene estados válidos para un tipo de work item específico.
     */
    public List<String> getWorkItemStateValues(String project, String workItemType) {
        List<String> states = new ArrayList<>();
        
        try {
            Map<String, Object> typeDefinition = azureDevOpsClient.getWorkItemTypeDefinition(project, workItemType);
            
            if (typeDefinition != null) {
                states = parseWorkItemStates(typeDefinition.toString());
            }
            
        } catch (Exception e) {
            // Error silencioso
        }
        
        return states;
    }
    
    // ========================================================================
    // MÉTODOS PRIVADOS DE APOYO
    // ========================================================================
    
    private List<String> tryGetPicklistFromProcesses(String picklistId) {
        try {
            String endpoint = String.format("/_apis/work/processes/lists/%s", picklistId);
            Map<String, String> queryParams = Map.of("api-version", "7.1");
            
            String response = azureDevOpsClient.makeGenericApiRequest(endpoint, queryParams);
            if (response != null && response.contains("\"items\"")) {
                return extractArrayValues(response, "items");
            }
        } catch (Exception e) {
            // Continuar con siguiente estrategia
        }
        return Collections.emptyList();
    }
    
    private List<String> tryGetPicklistFromProjectContext(String project, String picklistId) {
        try {
            String endpoint = String.format("/%s/_apis/work/processes/lists/%s", project, picklistId);
            Map<String, String> queryParams = Map.of("api-version", "7.1");
            
            String response = azureDevOpsClient.makeGenericApiRequest(endpoint, queryParams);
            if (response != null && response.contains("\"items\"")) {
                return extractArrayValues(response, "items");
            }
        } catch (Exception e) {
            // Continuar con siguiente estrategia
        }
        return Collections.emptyList();
    }
    
    private List<String> tryGetPicklistFromFieldEndpoint(String project, String fieldReferenceName) {
        try {
            String encodedFieldName = URLEncoder.encode(fieldReferenceName, StandardCharsets.UTF_8);
            String endpoint = String.format("/%s/_apis/wit/fields/%s/allowedValues", project, encodedFieldName);
            Map<String, String> queryParams = Map.of("api-version", "7.1");
            
            String response = azureDevOpsClient.makeGenericApiRequest(endpoint, queryParams);
            if (response != null && response.contains("\"value\"")) {
                return extractArrayValues(response, "value");
            }
        } catch (Exception e) {
            // Continuar con siguiente estrategia
        }
        return Collections.emptyList();
    }
    
    private List<String> extractArrayValues(String json, String arrayKey) {
        List<String> values = new ArrayList<>();
        
        try {
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
        } catch (Exception e) {
            // Error silencioso
        }
        
        return values;
    }
    
    private Map<String, Object> parseCompleteFieldDefinition(String jsonResponse) {
        Map<String, Object> definition = new HashMap<>();
        
        try {
            // Extraer propiedades básicas usando regex
            extractFieldProperty(jsonResponse, "name", definition);
            extractFieldProperty(jsonResponse, "referenceName", definition);
            extractFieldProperty(jsonResponse, "type", definition);
            extractFieldProperty(jsonResponse, "description", definition);
            
            // Extraer picklistId si existe
            String picklistId = extractJsonValue(jsonResponse, "picklistId");
            if (picklistId != null && !picklistId.trim().isEmpty()) {
                definition.put("picklistId", picklistId);
                definition.put("inferredType", "picklistString");
            }
            
            // Extraer allowedValues directos si existen
            List<String> allowedValues = parseFieldAllowedValues(jsonResponse);
            if (!allowedValues.isEmpty()) {
                definition.put("allowedValues", allowedValues);
                if (!definition.containsKey("inferredType")) {
                    definition.put("inferredType", "stringWithAllowedValues");
                }
            }
            
        } catch (Exception e) {
            // Error silencioso
        }
        
        return definition;
    }
    
    private void extractFieldProperty(String fieldData, String propertyName, Map<String, Object> fieldInfo) {
        try {
            Pattern pattern = Pattern.compile("\"" + propertyName + "\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(fieldData);
            if (matcher.find()) {
                fieldInfo.put(propertyName, matcher.group(1));
            }
        } catch (Exception e) {
            // Ignorar errores de propiedades individuales
        }
    }
    
    private String extractJsonValue(String json, String key) {
        try {
            Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(json);
            return matcher.find() ? matcher.group(1) : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private List<String> parseFieldAllowedValues(String jsonResponse) {
        List<String> values = new ArrayList<>();
        
        try {
            Pattern allowedValuesPattern = Pattern.compile("\"allowedValues\"\\s*:\\s*\\[([^\\]]+)\\]");
            Matcher matcher = allowedValuesPattern.matcher(jsonResponse);
            
            if (matcher.find()) {
                String allowedValuesStr = matcher.group(1);
                Pattern valuePattern = Pattern.compile("\"([^\"]+)\"");
                Matcher valueMatcher = valuePattern.matcher(allowedValuesStr);
                
                while (valueMatcher.find()) {
                    values.add(valueMatcher.group(1));
                }
            }
        } catch (Exception e) {
            // Error silencioso
        }
        
        return values;
    }
    
    private List<String> parseWorkItemStates(String jsonResponse) {
        List<String> states = new ArrayList<>();
        
        try {
            Pattern statesPattern = Pattern.compile("\"states\"\\s*:\\s*\\[([^\\]]+)\\]");
            Matcher matcher = statesPattern.matcher(jsonResponse);
            
            if (matcher.find()) {
                String statesContent = matcher.group(1);
                Pattern statePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
                Matcher stateMatcher = statePattern.matcher(statesContent);
                
                while (stateMatcher.find()) {
                    states.add(stateMatcher.group(1));
                }
            }
        } catch (Exception e) {
            // Error silencioso
        }
        
        return states;
    }
}
