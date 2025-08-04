package com.mcp.server.utils.field;

import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.utils.http.AzureDevOpsHttpUtil;
import com.mcp.server.utils.json.AzureDevOpsJsonParser;
import com.mcp.server.utils.discovery.AzureDevOpsPicklistInvestigator;
import com.mcp.server.tools.azuredevops.model.WiqlQueryResult;
import com.mcp.server.tools.azuredevops.model.WorkItem;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Componente responsable del análisis y procesamiento de campos (fields) de Azure DevOps.
 * Encapsula toda la funcionalidad relacionada con:
 * - Obtención de definiciones de campos
 * - Análisis de tipos de campos 
 * - Detección automática de campos tipo picklist
 * - Extracción de valores permitidos
 * - Procesamiento de metadatos de campos
 */
public class FieldAnalyzer {
    
    private final AzureDevOpsClient azureDevOpsClient;
    private final AzureDevOpsHttpUtil httpUtil;
    private final AzureDevOpsPicklistInvestigator picklistInvestigator;
    
    public FieldAnalyzer(AzureDevOpsClient azureDevOpsClient, 
                        AzureDevOpsHttpUtil httpUtil,
                        AzureDevOpsPicklistInvestigator picklistInvestigator) {
        this.azureDevOpsClient = azureDevOpsClient;
        this.httpUtil = httpUtil;
        this.picklistInvestigator = picklistInvestigator;
    }
    
    /**
     * Obtiene todos los campos del proyecto con metadatos detallados
     */
    public List<Map<String, Object>> getAllProjectFields(String project) {
        List<Map<String, Object>> fields = new ArrayList<>();
        
        try {
            String endpoint = httpUtil.buildFieldsEndpoint(project);
            String response = azureDevOpsClient.makeGenericApiRequest(endpoint, httpUtil.getDefaultQueryParams());
            if (response != null) {
                fields = parseProjectFields(response);
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo campos del proyecto: " + e.getMessage());
        }
        
        return fields;
    }
    
    /**
     * Parsea los campos del proyecto desde la respuesta JSON
     */
    public List<Map<String, Object>> parseProjectFields(String jsonResponse) {
        List<Map<String, Object>> fields = new ArrayList<>();
        
        // Buscar cada campo en el array "value" usando regex
        Pattern fieldPattern = Pattern.compile("\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\"[^}]*\"referenceName\"\\s*:\\s*\"([^\"]+)\"[^}]*\"type\"\\s*:\\s*\"([^\"]+)\"[^}]*\\}", Pattern.DOTALL);
        Matcher matcher = fieldPattern.matcher(jsonResponse);
        
        while (matcher.find()) {
            try {
                Map<String, Object> field = new HashMap<>();
                field.put("name", matcher.group(1));
                field.put("referenceName", matcher.group(2));
                field.put("type", matcher.group(3));
                
                // Buscar picklistId si existe en este campo
                String fieldBlock = matcher.group(0);
                String picklistId = AzureDevOpsJsonParser.extractSimpleValue(fieldBlock, "picklistId");
                if (picklistId != null) {
                    field.put("picklistId", picklistId);
                }
                
                fields.add(field);
            } catch (Exception e) {
                System.err.println("Error parsing field definition: " + e.getMessage());
            }
        }
        
        return fields;
    }
    
    /**
     * Obtiene valores permitidos para un campo específico usando múltiples estrategias
     * MEJORADO: Detecta automáticamente si un campo es de tipo picklist basado en su definición
     */
    public List<String> getFieldAllowedValues(String project, String workItemType, String referenceName, String fieldType) {
        List<String> allowedValues = new ArrayList<>();
        
        if (referenceName == null || referenceName.trim().isEmpty()) {
            return allowedValues;
        }
        
        try {
            // Estrategia 1: Obtener definición completa del campo para detectar si es picklist
            Map<String, Object> fieldDefinition = getCompleteFieldDefinition(project, referenceName);
            
            // Verificar si el campo tiene picklistId (indicador de que es un campo de lista)
            String picklistId = (String) fieldDefinition.get("picklistId");
            boolean hasPicklistId = picklistId != null && !picklistId.trim().isEmpty();
            
            // Estrategia 1a: Si tiene picklistId, obtener valores del picklist
            if (hasPicklistId) {
                allowedValues = picklistInvestigator.getPicklistValues(project, referenceName, picklistId);
            }
            
            // Estrategia 1b: Si no tiene picklistId pero la definición incluye allowedValues directamente
            if (allowedValues.isEmpty() && fieldDefinition.containsKey("allowedValues")) {
                Object allowedValuesObj = fieldDefinition.get("allowedValues");
                if (allowedValuesObj instanceof List<?>) {
                    @SuppressWarnings("unchecked")
                    List<String> directAllowedValues = (List<String>) allowedValuesObj;
                    allowedValues = new ArrayList<>(directAllowedValues);
                }
            }
            
            // Estrategia 2: Para campos de estado, obtener desde definición del tipo
            if (allowedValues.isEmpty() && "System.State".equals(referenceName)) {
                allowedValues = getWorkItemStateValues(project, workItemType);
            }
            
            // Estrategia 3: Extraer valores únicos de work items existentes (último recurso)
            if (allowedValues.isEmpty()) {
                allowedValues = extractUniqueFieldValuesFromExistingWorkItems(project, workItemType, referenceName);
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo valores permitidos para campo " + referenceName + ": " + e.getMessage());
        }
        
        return allowedValues;
    }
    
    /**
     * Obtiene la definición completa de un campo incluyendo información de picklist
     */
    public Map<String, Object> getCompleteFieldDefinition(String project, String referenceName) {
        Map<String, Object> fieldDefinition = new HashMap<>();
        
        try {
            String endpoint = httpUtil.buildFieldDefinitionEndpoint(project, referenceName);
            String response = azureDevOpsClient.makeGenericApiRequest(endpoint, httpUtil.getDefaultQueryParams());
            if (response != null) {
                fieldDefinition = parseCompleteFieldDefinition(response);
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo definición completa de campo " + referenceName + ": " + e.getMessage());
        }
        
        return fieldDefinition;
    }
    
    /**
     * Parsea la definición completa de un campo incluyendo detección automática de tipo
     */
    public Map<String, Object> parseCompleteFieldDefinition(String jsonResponse) {
        Map<String, Object> definition = new HashMap<>();
        
        try {
            // Extraer propiedades básicas
            extractFieldProperty(jsonResponse, "name", definition);
            extractFieldProperty(jsonResponse, "referenceName", definition);
            extractFieldProperty(jsonResponse, "type", definition);
            extractFieldProperty(jsonResponse, "description", definition);
            
            // CRÍTICO: Extraer picklistId si existe
            String picklistId = AzureDevOpsJsonParser.extractSimpleValue(jsonResponse, "picklistId");
            if (picklistId != null && !picklistId.trim().isEmpty()) {
                definition.put("picklistId", picklistId);
                
                // Si tiene picklistId, automáticamente es de tipo picklistString
                definition.put("inferredType", "picklistString");
            }
            
            // Extraer allowedValues directos si existen
            List<String> allowedValues = parseFieldAllowedValues(jsonResponse);
            if (!allowedValues.isEmpty()) {
                definition.put("allowedValues", allowedValues);
                
                // Si tiene allowedValues, también es probablemente un campo de lista
                if (!definition.containsKey("inferredType")) {
                    definition.put("inferredType", "picklistString");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error parseando definición completa de campo: " + e.getMessage());
        }
        
        return definition;
    }
    
    /**
     * Obtiene valores permitidos desde la definición específica de un campo
     */
    public List<String> getFieldDefinitionAllowedValues(String project, String referenceName) {
        List<String> allowedValues = new ArrayList<>();
        
        try {
            String endpoint = httpUtil.buildFieldDefinitionEndpoint(project, referenceName);
            String response = azureDevOpsClient.makeGenericApiRequest(endpoint, httpUtil.getDefaultQueryParams());
            if (response != null) {
                // Buscar valores permitidos en la respuesta
                allowedValues = parseFieldAllowedValues(response);
                
                // Si no se encontraron valores directos, buscar en picklistId
                if (allowedValues.isEmpty()) {
                    String picklistId = AzureDevOpsJsonParser.extractSimpleValue(response, "picklistId");
                    if (picklistId != null && !picklistId.trim().isEmpty()) {
                        allowedValues = picklistInvestigator.getPicklistValues(project, referenceName, picklistId);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo definición de campo " + referenceName + ": " + e.getMessage());
        }
        
        return allowedValues;
    }
    
    /**
     * Determina automáticamente el tipo de campo basado en su definición
     */
    public String determineFieldType(Map<String, Object> fieldInfo, String fieldData) {
        try {
            // Estrategia 1: Si tiene picklistId, es un campo de lista
            String picklistId = (String) fieldInfo.get("picklistId");
            if (picklistId != null && !picklistId.trim().isEmpty()) {
                return "picklistString";
            }
            
            // Estrategia 2: Buscar si tiene allowedValues definidos en el JSON
            if (fieldData.contains("\"allowedValues\"")) {
                Pattern allowedValuesPattern = Pattern.compile("\"allowedValues\"\\s*:\\s*\\[([^\\]]+)\\]");
                Matcher matcher = allowedValuesPattern.matcher(fieldData);
                if (matcher.find()) {
                    String valuesString = matcher.group(1);
                    // Si tiene valores, agregar la información
                    List<String> allowedValues = parseAllowedValuesFromString(valuesString);
                    if (!allowedValues.isEmpty()) {
                        fieldInfo.put("allowedValues", allowedValues);
                        return "picklistString";
                    }
                }
            }
            
            // Estrategia 3: Análisis del tipo base de Azure DevOps
            String baseType = (String) fieldInfo.get("type");
            if (baseType != null) {
                switch (baseType.toLowerCase()) {
                    case "boolean":
                        return "boolean";
                    case "integer":
                    case "double":
                        return baseType.toLowerCase();
                    case "datetime":
                        return "dateTime";
                    case "html":
                        return "html";
                    case "identity":
                        return "identity";
                    case "plaintext":
                        return "plainText";
                    case "string":
                    default:
                        // Para strings, verificar si es realmente un campo de lista basado en el nombre
                        String referenceName = (String) fieldInfo.get("referenceName");
                        if (referenceName != null && isLikelyPicklistField(referenceName)) {
                            return "picklistString";
                        }
                        return "string";
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error determinando tipo de campo: " + e.getMessage());
        }
        
        return "string"; // Fallback por defecto
    }
    
    /**
     * Determina si un campo es probablemente de tipo lista basado en su nombre
     */
    public boolean isLikelyPicklistField(String referenceName) {
        if (referenceName == null) return false;
        
        String fieldName = referenceName.toLowerCase();
        
        // Patrones comunes que indican campos de lista
        return fieldName.contains("tipo") ||
               fieldName.contains("type") ||
               fieldName.contains("categoria") ||
               fieldName.contains("category") ||
               fieldName.contains("clasificacion") ||
               fieldName.contains("classification") ||
               fieldName.contains("nivel") ||
               fieldName.contains("level") ||
               fieldName.contains("origen") ||
               fieldName.contains("source") ||
               fieldName.contains("fase") ||
               fieldName.contains("phase") ||
               fieldName.contains("estado") ||
               fieldName.contains("status") ||
               fieldName.contains("prioridad") ||
               fieldName.contains("priority");
    }
    
    /**
     * Extrae una propiedad específica del JSON de campo
     */
    public void extractFieldProperty(String fieldData, String propertyName, Map<String, Object> fieldInfo) {
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
    
    /**
     * Parsea valores permitidos de una cadena JSON
     */
    public List<String> parseAllowedValuesFromString(String valuesString) {
        List<String> values = new ArrayList<>();
        
        try {
            Pattern valuePattern = Pattern.compile("\"([^\"]+)\"");
            Matcher valueMatcher = valuePattern.matcher(valuesString);
            
            while (valueMatcher.find()) {
                values.add(valueMatcher.group(1));
            }
            
        } catch (Exception e) {
            System.err.println("Error parseando valores permitidos: " + e.getMessage());
        }
        
        return values;
    }
    
    /**
     * Parsea valores permitidos desde una respuesta JSON completa
     */
    public List<String> parseFieldAllowedValues(String jsonResponse) {
        List<String> allowedValues = new ArrayList<>();
        
        try {
            // Buscar allowedValues en el JSON
            Pattern allowedValuesPattern = Pattern.compile("\"allowedValues\"\\s*:\\s*\\[([^\\]]+)\\]");
            Matcher matcher = allowedValuesPattern.matcher(jsonResponse);
            
            if (matcher.find()) {
                String valuesString = matcher.group(1);
                allowedValues = parseAllowedValuesFromString(valuesString);
            }
            
        } catch (Exception e) {
            System.err.println("Error parseando valores permitidos desde JSON: " + e.getMessage());
        }
        
        return allowedValues;
    }
    
    /**
     * Obtiene todos los campos del proyecto con información detallada incluyendo metadatos extendidos
     */
    public List<Map<String, Object>> getAllProjectFieldsDetailed(String project) {
        List<Map<String, Object>> fields = new ArrayList<>();
        
        try {
            String endpoint = httpUtil.buildFieldsEndpoint(project);
            Map<String, String> params = httpUtil.getDefaultQueryParams();
            params.put("$expand", "extensionFields");
            
            String response = azureDevOpsClient.makeGenericApiRequest(endpoint, params);
            if (response != null) {
                fields = parseProjectFieldsResponse(response);
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo campos detallados del proyecto: " + e.getMessage());
        }
        
        return fields;
    }
    
    /**
     * Parsea la respuesta de campos del proyecto con información extendida
     */
    public List<Map<String, Object>> parseProjectFieldsResponse(String jsonResponse) {
        List<Map<String, Object>> fields = new ArrayList<>();
        
        try {
            // Buscar campos en el array value
            Pattern fieldArrayPattern = Pattern.compile("\"value\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
            Matcher arrayMatcher = fieldArrayPattern.matcher(jsonResponse);
            
            if (arrayMatcher.find()) {
                String fieldsArray = arrayMatcher.group(1);
                
                // Dividir por campos individuales - buscar objetos JSON completos
                Pattern fieldPattern = Pattern.compile("\\{(?:[^{}]|\\{[^{}]*\\})*\\}");
                Matcher fieldMatcher = fieldPattern.matcher(fieldsArray);
                
                while (fieldMatcher.find()) {
                    String fieldData = fieldMatcher.group(0);
                    Map<String, Object> fieldInfo = parseProjectFieldData(fieldData);
                    if (!fieldInfo.isEmpty()) {
                        fields.add(fieldInfo);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error parseando respuesta de campos del proyecto: " + e.getMessage());
        }
        
        return fields;
    }
    
    /**
     * Parsea los datos de un campo individual desde JSON
     */
    public Map<String, Object> parseProjectFieldData(String fieldData) {
        Map<String, Object> fieldInfo = new HashMap<>();
        
        try {
            // Extraer propiedades básicas
            extractFieldProperty(fieldData, "name", fieldInfo);
            extractFieldProperty(fieldData, "referenceName", fieldInfo);
            extractFieldProperty(fieldData, "type", fieldInfo);
            extractFieldProperty(fieldData, "description", fieldInfo);
            extractFieldProperty(fieldData, "picklistId", fieldInfo);
            
            // Determinar el tipo inferido del campo
            if (!fieldInfo.isEmpty()) {
                String inferredType = determineFieldType(fieldInfo, fieldData);
                fieldInfo.put("inferredType", inferredType);
            }
            
        } catch (Exception e) {
            System.err.println("Error parseando datos de campo individual: " + e.getMessage());
        }
        
        return fieldInfo;
    }
    
    // Métodos auxiliares que necesitan ser implementados o delegados
    
    /**
     * Obtiene valores de estado para un tipo de work item específico
     * TODO: Implementar o delegar a WorkItemTypeManager cuando esté disponible
     */
    private List<String> getWorkItemStateValues(String project, String workItemType) {
        // Implementación temporal - esto debería moverse a WorkItemTypeManager
        List<String> states = new ArrayList<>();
        
        try {
            String endpoint = String.format("/%s/_apis/wit/workitemtypes/%s", project, workItemType);
            String response = azureDevOpsClient.makeGenericApiRequest(endpoint, httpUtil.getDefaultQueryParams());
            if (response != null) {
                // Parsear estados desde la respuesta
                Pattern statesPattern = Pattern.compile("\"states\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
                Matcher matcher = statesPattern.matcher(response);
                
                if (matcher.find()) {
                    String statesArray = matcher.group(1);
                    Pattern statePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
                    Matcher stateMatcher = statePattern.matcher(statesArray);
                    
                    while (stateMatcher.find()) {
                        states.add(stateMatcher.group(1));
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo estados de work item: " + e.getMessage());
        }
        
        return states;
    }
    
    /**
     * Extrae valores únicos de un campo específico consultando work items existentes
     * TODO: Implementar o delegar a WorkItemProcessor cuando esté disponible
     */
    private List<String> extractUniqueFieldValuesFromExistingWorkItems(String project, String workItemType, String referenceName) {
        // Implementación temporal - esto debería delegarse a WorkItemProcessor
        List<String> uniqueValues = new ArrayList<>();
        
        try {
            // Construir consulta WIQL para obtener valores del campo
            String wiql = String.format(
                "SELECT [%s] FROM WorkItems WHERE [System.TeamProject] = '%s' AND [System.WorkItemType] = '%s'",
                referenceName, project, workItemType
            );
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", wiql);
            
            // Usar AzureDevOpsClient directamente para ejecutar query WIQL
            WiqlQueryResult queryResult = azureDevOpsClient.executeWiqlQuery(project, wiql, "100");
            
            if (queryResult != null && queryResult.workItems() != null && !queryResult.workItems().isEmpty()) {
                Set<String> valuesSet = new HashSet<>();
                
                // Obtener los IDs de work items del resultado
                List<Integer> workItemIds = new ArrayList<>();
                for (WiqlQueryResult.WorkItemReference ref : queryResult.workItems()) {
                    workItemIds.add(ref.id());
                    if (workItemIds.size() >= 100) break; // Limitar a 100
                }
                
                if (!workItemIds.isEmpty()) {
                    // Obtener work items completos con el campo específico
                    List<String> fields = Arrays.asList(referenceName);
                    List<WorkItem> workItems = azureDevOpsClient.getWorkItems(project, workItemIds, fields);
                    
                    for (WorkItem workItem : workItems) {
                        if (workItem.fields() != null && workItem.fields().containsKey(referenceName)) {
                            Object fieldValue = workItem.fields().get(referenceName);
                            if (fieldValue != null && !fieldValue.toString().trim().isEmpty()) {
                                valuesSet.add(fieldValue.toString().trim());
                            }
                        }
                    }
                }
                
                uniqueValues = new ArrayList<>(valuesSet);
                Collections.sort(uniqueValues);
            }
            
        } catch (Exception e) {
            System.err.println("Error extrayendo valores únicos de campo " + referenceName + ": " + e.getMessage());
        }
        
        return uniqueValues;
    }
}
