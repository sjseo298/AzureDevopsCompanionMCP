package com.mcp.server.tools.azuredevops;

import com.mcp.server.config.OrganizationContextService;
import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.tools.azuredevops.client.AzureDevOpsException;
import com.mcp.server.tools.azuredevops.model.WiqlQueryResult;
import com.mcp.server.tools.azuredevops.model.WorkItem;
import com.mcp.server.tools.base.McpTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Herramienta MCP para ejecutar consultas WIQL en Azure DevOps.
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class QueryWorkItemsTool implements McpTool {
    
    private final AzureDevOpsClient azureDevOpsClient;
    private final OrganizationContextService organizationContextService;
    
    @Autowired
    public QueryWorkItemsTool(AzureDevOpsClient azureDevOpsClient, OrganizationContextService organizationContextService) {
        this.azureDevOpsClient = azureDevOpsClient;
        this.organizationContextService = organizationContextService;
    }
    
    @Override
    public String getName() {
        return "azuredevops_query_workitems";
    }
    
    @Override
    public String getDescription() {
        return "Ejecuta una consulta WIQL para buscar work items en Azure DevOps. " +
               "Soporta macros como @Me, @Today, @CurrentIteration, @Project.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "project", Map.of(
                    "type", "string",
                    "description", "Nombre o ID del proyecto"
                ),
                "team", Map.of(
                    "type", "string",
                    "description", "Nombre o ID del equipo (opcional)"
                ),
                "query", Map.of(
                    "type", "string",
                    "description", "Consulta WIQL a ejecutar. Ejemplo: 'SELECT [System.Id], [System.Title] FROM WorkItems WHERE [System.AssignedTo] = @Me AND [System.State] <> \"Closed\"'"
                ),
                "includeDetails", Map.of(
                    "type", "boolean",
                    "description", "Si incluir detalles completos de los work items (por defecto: true)"
                ),
                "maxResults", Map.of(
                    "type", "number",
                    "description", "Número máximo de work items a retornar (por defecto: 50)"
                )
            ),
            "required", List.of("project", "query")
        );
    }
    
    @Override
    public Map<String, Object> execute(Map<String, Object> arguments) {
        try {
            if (!azureDevOpsClient.isConfigured()) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", "Azure DevOps no está configurado. Necesita configurar AZURE_DEVOPS_ORGANIZATION y AZURE_DEVOPS_PAT."
                    ))
                );
            }
            
            String project = (String) arguments.get("project");
            String team = (String) arguments.get("team");
            String query = (String) arguments.get("query");
            Boolean includeDetails = (Boolean) arguments.getOrDefault("includeDetails", true);
            Number maxResultsNum = (Number) arguments.getOrDefault("maxResults", 50);
            int maxResults = maxResultsNum.intValue();
            
            if (project == null || project.trim().isEmpty()) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", "El parámetro 'project' es requerido"
                    ))
                );
            }
            
            if (query == null || query.trim().isEmpty()) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", "El parámetro 'query' es requerido"
                    ))
                );
            }
            
            // Para debugging: log de la consulta original
            System.out.println("Consulta original: " + query);
            
            // TEMPORALMENTE DESACTIVAR ENRIQUECIMIENTO PARA DEBUGGING
            // Enriquecer la consulta con campos contextuales si es necesario
            // String enrichedQuery = enrichWiqlQuery(query);
            // System.out.println("Consulta enriquecida: " + enrichedQuery);
            
            // Ejecutar la consulta WIQL directamente
            WiqlQueryResult queryResult = azureDevOpsClient.executeWiqlQuery(project, team, query);
            
            if (!queryResult.hasResults()) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", "La consulta WIQL no retornó resultados."
                    ))
                );
            }
            
            // Limitar resultados
            List<Integer> workItemIds = queryResult.getWorkItemIds();
            if (workItemIds.size() > maxResults) {
                workItemIds = workItemIds.subList(0, maxResults);
            }
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("Consulta WIQL ejecutada en proyecto '%s'", project));
            if (team != null && !team.trim().isEmpty()) {
                result.append(String.format(" equipo '%s'", team));
            }
            result.append(String.format(":%n"));
            result.append(String.format("Encontrados %d work items", queryResult.getResultCount()));
            if (workItemIds.size() < queryResult.getResultCount()) {
                result.append(String.format(" (mostrando primeros %d)", workItemIds.size()));
            }
            result.append("%n%n");
            
            if (includeDetails) {
                // Obtener detalles de los work items
                List<WorkItem> workItems = azureDevOpsClient.getWorkItems(project, workItemIds, null);
                
                for (int i = 0; i < workItems.size(); i++) {
                    WorkItem workItem = workItems.get(i);
                    result.append(String.format("%d. %s%n", i + 1, workItem.toDisplayString()));
                    
                    if (workItem.getDescription() != null && !workItem.getDescription().trim().isEmpty()) {
                        String description = workItem.getDescription();
                        if (description.length() > 100) {
                            description = description.substring(0, 100) + "...";
                        }
                        result.append(String.format("   Descripción: %s%n", description));
                    }
                    
                    if (workItem.getIterationPath() != null) {
                        result.append(String.format("   Iteración: %s%n", workItem.getIterationPath()));
                    }
                    
                    if (workItem.getRemainingWork() != null) {
                        result.append(String.format("   Trabajo restante: %.1f horas%n", workItem.getRemainingWork()));
                    }
                    
                    result.append("%n");
                }
            } else {
                // Solo mostrar IDs
                result.append("IDs de work items encontrados: ");
                result.append(workItemIds.stream()
                    .map(String::valueOf)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
                result.append("%n");
            }
            
            return Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", result.toString()
                ))
            );
            
        } catch (AzureDevOpsException e) {
            return Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", "Error de Azure DevOps: " + e.getMessage()
                ))
            );
        } catch (Exception e) {
            return Map.of(
                "content", List.of(Map.of(
                    "type", "text", 
                    "text", "Error inesperado: " + e.getMessage()
                ))
            );
        }
    }
    
    /**
     * Enriquece consultas WIQL simples con campos contextuales de la organización.
     * Si la consulta ya incluye campos específicos, no se modifica.
     */
    private String enrichWiqlQuery(String originalQuery) {
        if (originalQuery == null || originalQuery.trim().isEmpty()) {
            return originalQuery;
        }
        
        String query = originalQuery.trim();
        
        // Detectar si es una consulta SELECT simple que podría beneficiarse del enriquecimiento
        Pattern simpleSelectPattern = Pattern.compile(
            "SELECT\\s+\\[System\\.Id\\]\\s*,?\\s*\\[System\\.Title\\]\\s*(?:,\\s*\\[System\\.State\\])?\\s*(?:,\\s*\\[System\\.WorkItemType\\])?\\s+FROM\\s+WorkItems",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = simpleSelectPattern.matcher(query);
        if (matcher.find()) {
            // Es una consulta simple, enriquecerla
            String whereClause = query.substring(matcher.end()).trim();
            
            // Intentar determinar el tipo de work item de la cláusula WHERE
            String workItemType = extractWorkItemTypeFromWhere(whereClause);
            
            // Construir consulta enriquecida con enfoque conservador
            String enrichedSelect = organizationContextService.buildWiqlSelectClauseWithDates();
            
            String enrichedQuery = enrichedSelect + " FROM WorkItems";
            if (!whereClause.isEmpty()) {
                enrichedQuery += " " + whereClause;
            }
            
            return enrichedQuery;
        }
        
        // Si no es una consulta simple, devolver sin modificar
        return originalQuery;
    }
    
    /**
     * Extrae el tipo de work item de la cláusula WHERE si está presente.
     */
    private String extractWorkItemTypeFromWhere(String whereClause) {
        if (whereClause == null || whereClause.isEmpty()) {
            return null;
        }
        
        // Buscar patrones como [System.WorkItemType] = 'Feature'
        Pattern workItemTypePattern = Pattern.compile(
            "\\[System\\.WorkItemType\\]\\s*=\\s*['\"]([^'\"]+)['\"]",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = workItemTypePattern.matcher(whereClause);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Buscar patrones como [System.WorkItemType] IN ('Feature', 'Epic')
        Pattern inPattern = Pattern.compile(
            "\\[System\\.WorkItemType\\]\\s+IN\\s*\\([^)]*['\"]([^'\"]+)['\"]",
            Pattern.CASE_INSENSITIVE
        );
        
        matcher = inPattern.matcher(whereClause);
        if (matcher.find()) {
            return matcher.group(1); // Devolver el primer tipo encontrado
        }
        
        return null;
    }
    
    /**
     * Ejecuta una consulta WIQL con fallback en caso de error.
     * Si la consulta enriquecida falla, intenta con la consulta original.
     */
    private WiqlQueryResult executeWiqlQueryWithFallback(String project, String team, String enrichedQuery, String originalQuery) throws AzureDevOpsException {
        try {
            // Intentar con la consulta enriquecida primero
            return azureDevOpsClient.executeWiqlQuery(project, team, enrichedQuery);
        } catch (AzureDevOpsException e) {
            System.out.println("La consulta enriquecida falló, intentando con consulta original: " + e.getMessage());
            
            // Si la consulta enriquecida falla, intentar con la original
            if (!enrichedQuery.equals(originalQuery)) {
                try {
                    return azureDevOpsClient.executeWiqlQuery(project, team, originalQuery);
                } catch (AzureDevOpsException originalError) {
                    // Si ambas fallan, intentar con una consulta básica
                    System.out.println("La consulta original también falló, intentando consulta básica: " + originalError.getMessage());
                    
                    String basicQuery = createBasicFallbackQuery(originalQuery);
                    if (basicQuery != null) {
                        return azureDevOpsClient.executeWiqlQuery(project, team, basicQuery);
                    } else {
                        // Si todo falla, lanzar el error original
                        throw e;
                    }
                }
            } else {
                // Si la consulta enriquecida es igual a la original, intentar consulta básica
                String basicQuery = createBasicFallbackQuery(originalQuery);
                if (basicQuery != null) {
                    return azureDevOpsClient.executeWiqlQuery(project, team, basicQuery);
                } else {
                    throw e;
                }
            }
        }
    }
    
    /**
     * Crea una consulta básica de fallback basada en la consulta original.
     */
    private String createBasicFallbackQuery(String originalQuery) {
        if (originalQuery == null || originalQuery.trim().isEmpty()) {
            return null;
        }
        
        // Extraer la cláusula WHERE de la consulta original
        String upperQuery = originalQuery.toUpperCase();
        int whereIndex = upperQuery.indexOf("WHERE");
        
        if (whereIndex != -1) {
            String whereClause = originalQuery.substring(whereIndex);
            
            // Construir consulta básica con campos garantizados
            String basicSelect = organizationContextService.buildBasicWiqlSelectClause();
            return basicSelect + " FROM WorkItems " + whereClause;
        }
        
        return null;
    }
}
