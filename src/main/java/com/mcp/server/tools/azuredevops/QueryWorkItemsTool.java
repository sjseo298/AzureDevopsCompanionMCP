package com.mcp.server.tools.azuredevops;

import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.tools.azuredevops.client.AzureDevOpsException;
import com.mcp.server.tools.azuredevops.model.WiqlQueryResult;
import com.mcp.server.tools.azuredevops.model.WorkItem;
import com.mcp.server.tools.base.McpTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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
    
    @Autowired
    public QueryWorkItemsTool(AzureDevOpsClient azureDevOpsClient) {
        this.azureDevOpsClient = azureDevOpsClient;
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
                    "error", "Azure DevOps no está configurado. Necesita configurar AZURE_DEVOPS_ORGANIZATION y AZURE_DEVOPS_PAT."
                );
            }
            
            String project = (String) arguments.get("project");
            String team = (String) arguments.get("team");
            String query = (String) arguments.get("query");
            Boolean includeDetails = (Boolean) arguments.getOrDefault("includeDetails", true);
            Number maxResultsNum = (Number) arguments.getOrDefault("maxResults", 50);
            int maxResults = maxResultsNum.intValue();
            
            if (project == null || project.trim().isEmpty()) {
                return Map.of("error", "El parámetro 'project' es requerido");
            }
            
            if (query == null || query.trim().isEmpty()) {
                return Map.of("error", "El parámetro 'query' es requerido");
            }
            
            // Ejecutar la consulta WIQL
            WiqlQueryResult queryResult = azureDevOpsClient.executeWiqlQuery(project, team, query);
            
            if (!queryResult.hasResults()) {
                return(Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", "La consulta WIQL no retornó resultados."
                    ))
                ));
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
            
            return(Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", result.toString()
                ))
            ));
            
        } catch (AzureDevOpsException e) {
            return Map.of("error", "Error de Azure DevOps: " + e.getMessage());
        } catch (Exception e) {
            return Map.of("error", "Error inesperado: " + e.getMessage());
        }
    }
}
