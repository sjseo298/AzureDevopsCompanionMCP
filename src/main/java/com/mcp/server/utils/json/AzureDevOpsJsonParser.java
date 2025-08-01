package com.mcp.server.utils.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidad centralizada para parsear respuestas JSON de Azure DevOps usando regex.
 * Proporciona métodos para extraer valores comunes y patrones específicos de Azure DevOps.
 */
public class AzureDevOpsJsonParser {
    
    /**
     * Extrae un valor simple de una respuesta JSON usando el nombre de la clave.
     * 
     * @param json respuesta JSON
     * @param key nombre de la clave a extraer
     * @return valor de la clave o null si no se encuentra
     */
    public static String extractSimpleValue(String json, String key) {
        if (json == null || key == null) return null;
        
        try {
            Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(json);
            return matcher.find() ? matcher.group(1) : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Extrae un valor numérico de una respuesta JSON.
     * 
     * @param json respuesta JSON
     * @param key nombre de la clave a extraer
     * @return valor numérico como string o null si no se encuentra
     */
    public static String extractNumericValue(String json, String key) {
        if (json == null || key == null) return null;
        
        try {
            Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)");
            Matcher matcher = pattern.matcher(json);
            return matcher.find() ? matcher.group(1) : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Extrae un array de strings de una respuesta JSON.
     * 
     * @param json respuesta JSON
     * @param arrayKey nombre del array a extraer
     * @return lista de valores del array
     */
    public static List<String> extractStringArray(String json, String arrayKey) {
        List<String> values = new ArrayList<>();
        
        if (json == null || arrayKey == null) return values;
        
        try {
            Pattern arrayPattern = Pattern.compile("\"" + Pattern.quote(arrayKey) + "\"\\s*:\\s*\\[([^\\]]+)\\]");
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
            // Error silencioso - retornar lista vacía
        }
        
        return values;
    }
    
    /**
     * Extrae la sección de campos de un work item.
     * 
     * @param workItemJson JSON del work item
     * @return contenido de la sección fields o null si no se encuentra
     */
    public static String extractFieldsSection(String workItemJson) {
        if (workItemJson == null) return null;
        
        try {
            // Patrón mejorado que maneja objetos anidados
            Pattern fieldsPattern = Pattern.compile("\"fields\"\\s*:\\s*\\{([^}]+(?:\\{[^}]*\\}[^}]*)*)\\}");
            Matcher fieldsMatcher = fieldsPattern.matcher(workItemJson);
            
            return fieldsMatcher.find() ? fieldsMatcher.group(1) : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Extrae el valor de un campo específico de la sección de campos de un work item.
     * 
     * @param fieldsSection contenido de la sección fields
     * @param fieldReferenceName nombre de referencia del campo
     * @return valor del campo o null si no se encuentra
     */
    public static String extractFieldValue(String fieldsSection, String fieldReferenceName) {
        if (fieldsSection == null || fieldReferenceName == null) return null;
        
        try {
            // Escapar caracteres especiales en el nombre del campo
            String escapedFieldName = Pattern.quote(fieldReferenceName);
            
            // Intentar primero con valor entre comillas
            Pattern fieldPattern = Pattern.compile("\"" + escapedFieldName + "\"\\s*:\\s*\"([^\"]+)\"");
            Matcher fieldMatcher = fieldPattern.matcher(fieldsSection);
            
            if (fieldMatcher.find()) {
                return fieldMatcher.group(1);
            }
            
            // Intentar sin comillas (para valores numéricos o booleanos)
            Pattern fieldPatternNoQuotes = Pattern.compile("\"" + escapedFieldName + "\"\\s*:\\s*([^,\\}]+)");
            Matcher fieldMatcherNoQuotes = fieldPatternNoQuotes.matcher(fieldsSection);
            
            if (fieldMatcherNoQuotes.find()) {
                String value = fieldMatcherNoQuotes.group(1).trim();
                // Limpiar comillas si las tiene
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }
            
        } catch (Exception e) {
            // Error silencioso
        }
        
        return null;
    }
    
    /**
     * Extrae los IDs de work items de una respuesta WIQL.
     * 
     * @param wiqlResponse respuesta JSON de una consulta WIQL
     * @return lista de IDs de work items
     */
    public static List<Integer> extractWorkItemIds(String wiqlResponse) {
        List<Integer> workItemIds = new ArrayList<>();
        
        if (wiqlResponse == null) return workItemIds;
        
        try {
            // Buscar sección "workItems" en la respuesta
            Pattern workItemsPattern = Pattern.compile("\"workItems\"\\s*:\\s*\\[([^\\]]+)\\]");
            Matcher workItemsMatcher = workItemsPattern.matcher(wiqlResponse);
            
            if (workItemsMatcher.find()) {
                String workItemsContent = workItemsMatcher.group(1);
                
                // Extraer IDs individuales
                Pattern idPattern = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");
                Matcher idMatcher = idPattern.matcher(workItemsContent);
                
                while (idMatcher.find()) {
                    try {
                        workItemIds.add(Integer.parseInt(idMatcher.group(1)));
                    } catch (NumberFormatException e) {
                        // Ignorar IDs inválidos
                    }
                }
            }
        } catch (Exception e) {
            // Error silencioso
        }
        
        return workItemIds;
    }
    
    /**
     * Extrae información de campos de la respuesta de la API de campos.
     * 
     * @param fieldsResponse respuesta JSON de la API de campos
     * @return lista de mapas con información de campos
     */
    public static List<Map<String, Object>> extractFieldDefinitions(String fieldsResponse) {
        List<Map<String, Object>> fields = new ArrayList<>();
        
        if (fieldsResponse == null) return fields;
        
        try {
            // Buscar el array "value" que contiene los campos
            Pattern valuePattern = Pattern.compile("\"value\"\\s*:\\s*\\[([^\\]]+(?:\\[[^\\]]*\\][^\\]]*)*)\\]");
            Matcher valueMatcher = valuePattern.matcher(fieldsResponse);
            
            if (valueMatcher.find()) {
                String valueContent = valueMatcher.group(1);
                
                // Dividir en objetos individuales (aproximado)
                String[] fieldObjects = valueContent.split("\\},\\s*\\{");
                
                for (String fieldObj : fieldObjects) {
                    // Limpiar y procesar cada objeto de campo
                    fieldObj = fieldObj.trim();
                    if (!fieldObj.startsWith("{")) fieldObj = "{" + fieldObj;
                    if (!fieldObj.endsWith("}")) fieldObj = fieldObj + "}";
                    
                    Map<String, Object> fieldInfo = parseFieldObject(fieldObj);
                    if (!fieldInfo.isEmpty()) {
                        fields.add(fieldInfo);
                    }
                }
            }
        } catch (Exception e) {
            // Error silencioso
        }
        
        return fields;
    }
    
    /**
     * Parsea un objeto de campo individual.
     * 
     * @param fieldJson JSON del campo individual
     * @return mapa con propiedades del campo
     */
    public static Map<String, Object> parseFieldObject(String fieldJson) {
        Map<String, Object> fieldInfo = new HashMap<>();
        
        if (fieldJson == null) return fieldInfo;
        
        try {
            // Extraer propiedades básicas
            extractProperty(fieldJson, "name", fieldInfo);
            extractProperty(fieldJson, "referenceName", fieldInfo);
            extractProperty(fieldJson, "type", fieldInfo);
            extractProperty(fieldJson, "description", fieldInfo);
            extractProperty(fieldJson, "picklistId", fieldInfo);
            
            // Extraer valores permitidos si existen
            List<String> allowedValues = extractStringArray(fieldJson, "allowedValues");
            if (!allowedValues.isEmpty()) {
                fieldInfo.put("allowedValues", allowedValues);
            }
            
        } catch (Exception e) {
            // Error silencioso
        }
        
        return fieldInfo;
    }
    
    /**
     * Extrae una propiedad específica de un JSON y la coloca en un mapa.
     * 
     * @param json JSON fuente
     * @param propertyName nombre de la propiedad
     * @param targetMap mapa donde colocar la propiedad
     */
    public static void extractProperty(String json, String propertyName, Map<String, Object> targetMap) {
        if (json == null || propertyName == null || targetMap == null) return;
        
        try {
            String value = extractSimpleValue(json, propertyName);
            if (value != null) {
                targetMap.put(propertyName, value);
            }
        } catch (Exception e) {
            // Ignorar errores de propiedades individuales
        }
    }
    
    /**
     * Extrae estados de work item de la respuesta JSON de definición de tipo.
     * 
     * @param typeDefinitionJson JSON de definición de tipo de work item
     * @return lista de estados disponibles
     */
    public static List<String> extractWorkItemStates(String typeDefinitionJson) {
        List<String> states = new ArrayList<>();
        
        if (typeDefinitionJson == null) return states;
        
        try {
            Pattern statesPattern = Pattern.compile("\"states\"\\s*:\\s*\\[([^\\]]+)\\]");
            Matcher matcher = statesPattern.matcher(typeDefinitionJson);
            
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
    
    /**
     * Verifica si una respuesta JSON indica error.
     * 
     * @param jsonResponse respuesta a verificar
     * @return true si la respuesta indica error
     */
    public static boolean isErrorResponse(String jsonResponse) {
        if (jsonResponse == null) return true;
        
        return jsonResponse.contains("\"errorCode\"") ||
               jsonResponse.contains("\"message\":") && jsonResponse.contains("error") ||
               jsonResponse.contains("\"count\":0") ||
               jsonResponse.contains("\"value\":[]");
    }
    
    /**
     * Extrae mensaje de error de una respuesta JSON de error.
     * 
     * @param errorResponse respuesta JSON que contiene error
     * @return mensaje de error o descripción genérica
     */
    public static String extractErrorMessage(String errorResponse) {
        if (errorResponse == null) return "Respuesta nula";
        
        // Intentar extraer mensaje específico
        String message = extractSimpleValue(errorResponse, "message");
        if (message != null) return message;
        
        // Intentar extraer mensaje de error
        message = extractSimpleValue(errorResponse, "errorMessage");
        if (message != null) return message;
        
        // Fallback genérico
        return "Error no especificado en la respuesta de Azure DevOps";
    }
}
