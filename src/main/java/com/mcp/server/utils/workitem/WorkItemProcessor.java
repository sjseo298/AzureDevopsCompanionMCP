
package com.mcp.server.utils.workitem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.utils.http.AzureDevOpsHttpUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Clase especializada para procesar y analizar work items de Azure DevOps.
 * Proporciona métodos para extraer información, valores de campos, y parsear respuestas JSON de work items.
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class WorkItemProcessor {
    private final AzureDevOpsClient azureDevOpsClient;
    private final AzureDevOpsHttpUtil httpUtil;

    /**
     * Constructor que inicializa el procesador con las dependencias necesarias.
     *
     * @param azureDevOpsClient Cliente para comunicación con Azure DevOps API
     */
    @Autowired
    public WorkItemProcessor(AzureDevOpsClient azureDevOpsClient) {
        this.azureDevOpsClient = azureDevOpsClient;
        if (azureDevOpsClient != null) {
            this.httpUtil = new AzureDevOpsHttpUtil(azureDevOpsClient, azureDevOpsClient.getOrganization());
        } else {
            this.httpUtil = null;
        }
    }
    
    /**
     * Procesa un work item de referencia para extraer información de contexto organizacional
     * 
     * @param workItemReferencia Referencia al work item (URL o ID)
     * @return Mapa con la información extraída o null si hubo un error
     */
    public Map<String, Object> procesarWorkItemReferencia(String workItemReferencia) {
        try {
            // Extraer ID del work item
            Integer workItemId = extractWorkItemIdFromReference(workItemReferencia);
            if (workItemId == null) {
                System.err.println("No se pudo extraer ID válido del work item de referencia: " + workItemReferencia);
                return null;
            }
            
            // Buscar el proyecto del work item - intentar con proyectos conocidos
            Map<String, Object> workItemDetails = findWorkItemAcrossProjects(workItemId);
            if (workItemDetails == null) {
                System.err.println("No se pudo encontrar el work item " + workItemId + " en ningún proyecto accesible");
                return null;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> fields = (Map<String, Object>) workItemDetails.get("fields");
            
            if (fields != null) {
                String areaPath = (String) fields.get("System.AreaPath");
                String project = null;
                String team = null;
                
                // Extraer proyecto del área path
                if (areaPath != null && !areaPath.trim().isEmpty()) {
                    String[] pathParts = areaPath.split("\\\\");
                    if (pathParts.length > 0) {
                        project = pathParts[0];
                    }
                    
                    // El equipo puede estar en el segundo nivel del área path
                    if (pathParts.length > 1) {
                        team = pathParts[1];
                    }
                }
                
                Map<String, Object> result = new HashMap<>();
                result.put("workItemId", workItemId);
                result.put("project", project);
                result.put("areaPath", areaPath);
                result.put("team", team);
                result.put("iterationPath", fields.get("System.IterationPath"));
                result.put("workItemType", fields.get("System.WorkItemType"));
                
                System.out.println("Work item de referencia procesado exitosamente: ID=" + workItemId + 
                                 ", Proyecto=" + project + ", Área=" + areaPath);
                
                return result;
            }
            
        } catch (Exception e) {
            System.err.println("Error procesando work item de referencia " + workItemReferencia + ": " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extrae el ID del work item de una URL o texto de referencia
     * 
     * @param reference Referencia del work item (URL o texto)
     * @return ID numérico del work item o null si no se pudo extraer
     */
    public Integer extractWorkItemIdFromReference(String reference) {
        if (reference == null || reference.trim().isEmpty()) {
            return null;
        }
        
        reference = reference.trim();
        
        // Si es solo un número, devolverlo directamente
        try {
            return Integer.parseInt(reference);
        } catch (NumberFormatException e) {
            // No es un número simple, intentar extraer de URL
        }
        
        // Patrones para URLs de Azure DevOps
        String[] patterns = {
            "/_workitems/edit/(\\d+)",  // https://dev.azure.com/org/project/_workitems/edit/12345
            "/workitems/(\\d+)",        // https://dev.azure.com/org/project/_workitems/12345  
            "workItemId=(\\d+)",        // Query parameter
            "#(\\d+)"                   // Referencia simple como #12345
        };
        
        for (String pattern : patterns) {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(reference);
            if (m.find()) {
                try {
                    return Integer.parseInt(m.group(1));
                } catch (NumberFormatException e) {
                    // Continuar con el siguiente patrón
                }
            }
        }
        
        return null;
    }
    
    /**
     * Busca un work item específico a través de múltiples proyectos
     * 
     * @param workItemId ID del work item a buscar
     * @return Información del work item encontrado o null si no se encontró
     */
    public Map<String, Object> findWorkItemAcrossProjects(Integer workItemId) {
        try {
            // Primero, intentar obtener la lista de proyectos
            String organization = azureDevOpsClient.getOrganization();
            String url = String.format("https://dev.azure.com/%s/_apis/projects?api-version=6.0", organization);
            String projectsResponse = makeHttpGetRequest(url);
            
            if (projectsResponse != null) {
                // Extraer nombres de proyectos de la respuesta
                List<String> projectNames = extractProjectNames(projectsResponse);
                
                // Intentar encontrar el work item en cada proyecto
                for (String projectName : projectNames) {
                    try {
                        String workItemUrl = String.format(
                            "https://dev.azure.com/%s/%s/_apis/wit/workitems/%d?api-version=6.0",
                            organization, projectName, workItemId
                        );
                        
                        String workItemResponse = makeHttpGetRequest(workItemUrl);
                        if (workItemResponse != null && !workItemResponse.contains("does not exist")) {
                            // Parsear respuesta JSON básica
                            Map<String, Object> workItem = parseWorkItemResponse(workItemResponse);
                            if (workItem != null) {
                                return workItem;
                            }
                        }
                    } catch (Exception e) {
                        // Continuar con el siguiente proyecto
                        System.out.println("Work item " + workItemId + " no encontrado en proyecto " + projectName + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error buscando work item " + workItemId + " a través de proyectos: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extrae nombres de proyectos de la respuesta JSON de la API
     * 
     * @param jsonResponse Respuesta JSON con la lista de proyectos
     * @return Lista de nombres de proyectos extraídos
     */
    public List<String> extractProjectNames(String jsonResponse) {
        List<String> projectNames = new ArrayList<>();
        
        try {
            // Buscar patrones de nombres de proyecto en la respuesta JSON
            Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
            Matcher nameMatcher = namePattern.matcher(jsonResponse);
            
            while (nameMatcher.find()) {
                String projectName = nameMatcher.group(1);
                if (!projectNames.contains(projectName)) {
                    projectNames.add(projectName);
                }
            }
        } catch (Exception e) {
            System.err.println("Error extrayendo nombres de proyectos: " + e.getMessage());
        }
        
        return projectNames;
    }
    
    /**
     * Parsea la respuesta JSON de un work item de forma básica
     * 
     * @param jsonResponse Respuesta JSON del work item
     * @return Mapa con los campos extraídos
     */
    public Map<String, Object> parseWorkItemResponse(String jsonResponse) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Extraer campos usando regex (parsing básico)
            Pattern fieldsPattern = Pattern.compile("\"fields\"\\s*:\\s*\\{([^}]+(?:\\{[^}]*\\}[^}]*)*)\\}");
            Matcher fieldsMatcher = fieldsPattern.matcher(jsonResponse);
            
            if (fieldsMatcher.find()) {
                String fieldsSection = fieldsMatcher.group(1);
                Map<String, Object> fields = parseFieldsSection(fieldsSection);
                result.put("fields", fields);
                return result;
            }
        } catch (Exception e) {
            System.err.println("Error parseando respuesta de work item: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Parsea la sección de campos del JSON
     * 
     * @param fieldsSection Sección JSON que contiene los campos
     * @return Mapa con los campos extraídos
     */
    public Map<String, Object> parseFieldsSection(String fieldsSection) {
        Map<String, Object> fields = new HashMap<>();
        
        try {
            // Patrones para diferentes tipos de campos
            Pattern stringFieldPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*(?:\\\\.[^\"]*)*?)\"");
            Matcher stringMatcher = stringFieldPattern.matcher(fieldsSection);
            
            while (stringMatcher.find()) {
                String fieldName = stringMatcher.group(1);
                String fieldValue = stringMatcher.group(2);
                
                // Solo procesar campos del sistema relevantes
                if (fieldName.startsWith("System.")) {
                    fields.put(fieldName, fieldValue);
                }
            }
        } catch (Exception e) {
            System.err.println("Error parseando sección de campos: " + e.getMessage());
        }
        
        return fields;
    }
    
    /**
     * Extrae un campo específico de una respuesta JSON de Azure DevOps
     * 
     * @param jsonResponse Respuesta JSON completa
     * @param fieldName Nombre del campo a extraer
     * @return Valor del campo o null si no se encuentra
     */
    public String extractFieldFromJson(String jsonResponse, String fieldName) {
        try {
            // Buscar el campo en la sección "fields"
            String fieldsPattern = "\"fields\"\\s*:\\s*\\{([^}]+(?:\\{[^}]*\\}[^}]*)*)\\}";
            Pattern pattern = Pattern.compile(fieldsPattern);
            Matcher matcher = pattern.matcher(jsonResponse);
            
            if (matcher.find()) {
                String fieldsSection = matcher.group(1);
                
                // Buscar el campo específico
                String fieldPattern = "\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"";
                Pattern fieldPatternCompiled = Pattern.compile(fieldPattern);
                Matcher fieldMatcher = fieldPatternCompiled.matcher(fieldsSection);
                
                if (fieldMatcher.find()) {
                    return fieldMatcher.group(1);
                }
            }
        } catch (Exception e) {
            System.out.println("Error extrayendo campo " + fieldName + ": " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Extrae el valor de un campo específico de la respuesta JSON de un work item
     * 
     * @param jsonResponse Respuesta JSON del work item
     * @param referenceName Nombre de referencia del campo
     * @return Valor del campo o null si no se encuentra
     */
    public String extractFieldValueFromWorkItemResponse(String jsonResponse, String referenceName) {
        try {
            // Buscar en la sección "fields"
            Pattern fieldsPattern = Pattern.compile("\"fields\"\\s*:\\s*\\{([^}]+(?:\\{[^}]*\\}[^}]*)*)\\}");
            Matcher fieldsMatcher = fieldsPattern.matcher(jsonResponse);
            
            if (fieldsMatcher.find()) {
                String fieldsSection = fieldsMatcher.group(1);
                
                // Buscar el campo específico
                String escapedReferenceName = referenceName.replace(".", "\\.");
                Pattern fieldPattern = Pattern.compile("\"" + escapedReferenceName + "\"\\s*:\\s*\"([^\"]+)\"");
                Matcher fieldMatcher = fieldPattern.matcher(fieldsSection);
                
                if (fieldMatcher.find()) {
                    return fieldMatcher.group(1);
                }
                
                // También intentar sin comillas (para valores numéricos o booleanos)
                Pattern fieldPatternNoQuotes = Pattern.compile("\"" + escapedReferenceName + "\"\\s*:\\s*([^,\\}]+)");
                Matcher fieldMatcherNoQuotes = fieldPatternNoQuotes.matcher(fieldsSection);
                
                if (fieldMatcherNoQuotes.find()) {
                    return fieldMatcherNoQuotes.group(1).trim();
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error extrayendo valor de campo " + referenceName + " de respuesta: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Obtiene el valor de un campo específico de un work item individual
     * 
     * @param project Proyecto al que pertenece el work item
     * @param workItemId ID del work item
     * @param referenceName Nombre de referencia del campo
     * @return Valor del campo o null si hubo un error
     */
    public String getFieldValueFromWorkItem(String project, Integer workItemId, String referenceName) {
        try {
            String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/workitems/%d?fields=%s&api-version=7.1",
                    azureDevOpsClient.getOrganization(), project, workItemId, referenceName);
            
            String response = makeHttpGetRequest(url);
            if (response != null) {
                return extractFieldValueFromWorkItemResponse(response, referenceName);
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo valor de campo " + referenceName + " para work item " + workItemId + ": " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extrae valores únicos de un campo específico consultando work items existentes
     * 
     * @param project Proyecto a consultar
     * @param workItemType Tipo de work item
     * @param referenceName Nombre de referencia del campo
     * @return Lista de valores únicos encontrados
     */
    public List<String> extractUniqueFieldValuesFromExistingWorkItems(String project, String workItemType, String referenceName) {
        Set<String> uniqueValues = new HashSet<>();
        
        try {
            // Consulta WIQL para obtener muestras de work items de este tipo
            String wiqlQuery = String.format(
                "SELECT [System.Id], [%s] FROM WorkItems WHERE [System.WorkItemType] = '%s' ORDER BY [System.Id] DESC",
                referenceName, workItemType
            );
            
            // Limitar resultados para evitar sobrecarga
            String queryUrl = String.format("https://dev.azure.com/%s/%s/_apis/wit/wiql?$top=50&api-version=7.1", 
                    azureDevOpsClient.getOrganization(), project);
            
            String requestBody = String.format("{\"query\":\"%s\"}", wiqlQuery.replace("\"", "\\\""));
            String response = makePostRequest(queryUrl, requestBody);
            
            if (response != null) {
                List<Integer> workItemIds = extractWorkItemIds(response);
                
                // Para cada work item, obtener el valor del campo
                for (Integer workItemId : workItemIds) {
                    if (workItemIds.indexOf(workItemId) >= 20) break; // Limitar a primeros 20 para eficiencia
                    String fieldValue = getFieldValueFromWorkItem(project, workItemId, referenceName);
                    if (fieldValue != null && !fieldValue.trim().isEmpty() && !"null".equals(fieldValue)) {
                        uniqueValues.add(fieldValue);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error extrayendo valores únicos para campo " + referenceName + ": " + e.getMessage());
        }
        
        List<String> result = new ArrayList<>(uniqueValues);
        result.sort(String::compareToIgnoreCase);
        return result;
    }
    
    /**
     * Extrae IDs de work items de una respuesta JSON
     * 
     * @param jsonResponse Respuesta JSON que contiene IDs de work items
     * @return Lista de IDs de work items extraídos
     */
    public List<Integer> extractWorkItemIds(String jsonResponse) {
        List<Integer> ids = new ArrayList<>();
        
        try {
            // Buscar patrones de IDs en la respuesta JSON
            Pattern idPattern = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");
            Matcher matcher = idPattern.matcher(jsonResponse);
            
            while (matcher.find()) {
                try {
                    int id = Integer.parseInt(matcher.group(1));
                    ids.add(id);
                } catch (NumberFormatException e) {
                    // Ignorar IDs inválidos
                }
            }
        } catch (Exception e) {
            System.err.println("Error extrayendo IDs de work items: " + e.getMessage());
        }
        
        return ids;
    }
    
    /**
     * Extrae valores de campos de work items específicos
     * 
     * @param project Proyecto al que pertenecen los work items
     * @param workItemIds Lista de IDs de work items
     * @param customFields Lista de campos personalizados a extraer
     * @return Mapa con los valores de campos extraídos
     */
    public Map<String, Set<String>> extractFieldValuesFromWorkItems(String project, List<Integer> workItemIds, List<String> customFields) {
        Map<String, Set<String>> fieldValues = new HashMap<>();
        
        // Inicializar sets para cada campo
        for (String field : customFields) {
            fieldValues.put(field, new HashSet<>());
        }
        
        try {
            // Procesar work items en lotes para evitar URLs muy largas
            int batchSize = 10;
            for (int i = 0; i < workItemIds.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, workItemIds.size());
                List<Integer> batch = workItemIds.subList(i, endIndex);
                
                String idsParam = batch.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                
                String fieldsParam = customFields.stream()
                        .collect(Collectors.joining(","));
                
                String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/workitems?ids=%s&fields=%s&api-version=7.1", 
                        azureDevOpsClient.getOrganization(), project, idsParam, fieldsParam);
                
                String response = makeHttpGetRequest(url);
                if (response != null) {
                    parseFieldValuesFromBatch(response, customFields, fieldValues);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error extracting field values from work items: " + e.getMessage());
        }
        
        return fieldValues;
    }
    
    /**
     * Parsea valores de campos de un lote de work items
     * 
     * @param jsonResponse Respuesta JSON con el lote de work items
     * @param customFields Lista de campos personalizados a extraer
     * @param fieldValues Mapa donde almacenar los valores extraídos
     */
    public void parseFieldValuesFromBatch(String jsonResponse, List<String> customFields, Map<String, Set<String>> fieldValues) {
        // Buscar cada work item en la respuesta
        Pattern workItemPattern = Pattern.compile("\\{[^{}]*\"fields\"\\s*:\\s*\\{([^{}]+)\\}[^{}]*\\}");
        Matcher workItemMatcher = workItemPattern.matcher(jsonResponse);
        
        while (workItemMatcher.find()) {
            String fieldsContent = workItemMatcher.group(1);
            
            // Para cada campo personalizado, buscar su valor
            for (String field : customFields) {
                Pattern fieldPattern = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]+)\"");
                Matcher fieldMatcher = fieldPattern.matcher(fieldsContent);
                
                if (fieldMatcher.find()) {
                    String value = fieldMatcher.group(1);
                    if (value != null && !value.trim().isEmpty()) {
                        fieldValues.get(field).add(value);
                    }
                }
            }
        }
    }
    
    /**
     * Obtiene un work item como JSON usando AzureDevOpsClient
     * 
     * @param project Proyecto al que pertenece el work item
     * @param workItemId ID del work item
     * @return Respuesta JSON del work item o null si hubo un error
     */
    public String getWorkItemAsJson(String project, Integer workItemId) {
        try {
            String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/workitems/%d?api-version=7.1",
                    azureDevOpsClient.getOrganization(), project, workItemId);
            
            return makeHttpGetRequest(url);
        } catch (Exception e) {
            System.err.println("Error obteniendo work item como JSON: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Realiza una petición HTTP GET usando AzureDevOpsClient
     * 
     * @param url URL a la que hacer la petición
     * @return Respuesta de la petición o null si hubo un error
     */
    private String makeHttpGetRequest(String url) {
        try {
            return httpUtil.makeGetRequest(url);
        } catch (Exception e) {
            System.err.println("Error making HTTP GET request: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Realiza una petición POST HTTP usando AzureDevOpsClient
     * 
     * @param endpoint URL del endpoint
     * @param requestBody Cuerpo de la petición
     * @return Respuesta de la petición o null si hubo un error
     */
    private String makePostRequest(String endpoint, String requestBody) {
        try {
            // Extraer el endpoint relativo de la URL completa si es necesario
            String relativeEndpoint = endpoint;
            if (endpoint.startsWith("https://dev.azure.com/")) {
                // Extraer la parte después de la organización
                String orgName = azureDevOpsClient.getOrganization();
                String orgPrefix = "https://dev.azure.com/" + orgName;
                if (endpoint.startsWith(orgPrefix)) {
                    relativeEndpoint = endpoint.substring(orgPrefix.length());
                }
            }
            
            // Por ahora, usar el método genérico GET ya que no hay un método POST genérico
            // En el futuro se podría agregar un método makeGenericPostRequest a AzureDevOpsClient
            System.out.println("⚠️ makePostRequest temporalmente usando GET - considerar implementar POST genérico en AzureDevOpsClient");
            return azureDevOpsClient.makeGenericApiRequest(relativeEndpoint, null);
            
        } catch (Exception e) {
            System.err.println("Error realizando petición POST: " + e.getMessage());
            return null;
        }
    }
}
