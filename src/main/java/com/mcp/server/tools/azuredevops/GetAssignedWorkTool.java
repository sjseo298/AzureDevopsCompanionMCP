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

/**
 * Herramienta MCP para obtener work items asignados al usuario actual.
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class GetAssignedWorkTool implements McpTool {
    
    private final AzureDevOpsClient azureDevOpsClient;
    private final OrganizationContextService organizationContextService;
    
    @Autowired
    public GetAssignedWorkTool(AzureDevOpsClient azureDevOpsClient, OrganizationContextService organizationContextService) {
        this.azureDevOpsClient = azureDevOpsClient;
        this.organizationContextService = organizationContextService;
    }
    
    @Override
    public String getName() {
        return "azuredevops_get_assigned_work";
    }
    
    @Override
    public String getDescription() {
        return "Obtiene todos los work items asignados al usuario actual. " +
               "Ideal para planificación diaria y seguimiento de progreso.";
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
                "includeCompleted", Map.of(
                    "type", "boolean",
                    "description", "Si incluir work items completados/cerrados (por defecto: false)"
                ),
                "workItemTypes", Map.of(
                    "type", "array",
                    "items", Map.of("type", "string"),
                    "description", "Tipos de work items a incluir (ej: ['Task', 'Bug']) - opcional, por defecto todos"
                ),
                "iterationFilter", Map.of(
                    "type", "string",
                    "description", "Filtro de iteración: 'current' (actual), 'all' (todas) - por defecto: 'all'"
                ),
                "groupBy", Map.of(
                    "type", "string",
                    "description", "Agrupar resultados por: 'state', 'type', 'iteration' - opcional",
                    "enum", List.of("state", "type", "iteration")
                )
            ),
            "required", List.of("project")
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
                    )),
                    "isError", true
                );
            }
            
            String project = (String) arguments.get("project");
            String team = (String) arguments.get("team");
            Boolean includeCompleted = (Boolean) arguments.getOrDefault("includeCompleted", false);
            @SuppressWarnings("unchecked")
            List<String> workItemTypes = (List<String>) arguments.get("workItemTypes");
            String iterationFilter = (String) arguments.getOrDefault("iterationFilter", "all");
            String groupBy = (String) arguments.get("groupBy");
            
            if (project == null || project.trim().isEmpty()) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", "El parámetro 'project' es requerido"
                    )),
                    "isError", true
                );
            }
            
            // Construir consulta WIQL con contexto organizacional
            StringBuilder wiqlQuery = new StringBuilder();
            
            // Usar el servicio de contexto para construir la cláusula SELECT
            String selectClause = organizationContextService.buildWiqlSelectClause(
                null, // workItemType - null para incluir todos los tipos
                true, // includeDates - incluir campos de fecha
                true, // includeMetrics - incluir métricas
                false // includeCustomFields - no incluir por ahora para mantener compatibilidad
            );
            
            wiqlQuery.append(selectClause);
            wiqlQuery.append(" FROM WorkItems WHERE [System.AssignedTo] = @Me ");
            
            // Filtro de estado
            if (!includeCompleted) {
                wiqlQuery.append("AND [System.State] NOT IN ('Closed', 'Done', 'Resolved', 'Completed') ");
            }
            
            // Filtro de tipos de work item
            if (workItemTypes != null && !workItemTypes.isEmpty()) {
                wiqlQuery.append("AND [System.WorkItemType] IN (");
                for (int i = 0; i < workItemTypes.size(); i++) {
                    if (i > 0) wiqlQuery.append(", ");
                    wiqlQuery.append("'").append(workItemTypes.get(i)).append("'");
                }
                wiqlQuery.append(") ");
            }
            
            // Filtro de iteración
            if ("current".equals(iterationFilter)) {
                wiqlQuery.append("AND [System.IterationPath] = @CurrentIteration ");
            }
            
            wiqlQuery.append("ORDER BY [System.State], [Microsoft.VSTS.Common.Priority], [System.CreatedDate]");
            
            // Ejecutar consulta
            WiqlQueryResult queryResult = azureDevOpsClient.executeWiqlQuery(project, team, wiqlQuery.toString());
            
            if (!queryResult.hasResults()) {
                String message = includeCompleted 
                    ? "No tienes work items asignados en este proyecto."
                    : "No tienes work items pendientes asignados en este proyecto.";
                    
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", message
                    ))
                );
            }
            
            // Obtener detalles de work items
            List<WorkItem> workItems = azureDevOpsClient.getWorkItems(project, queryResult.getWorkItemIds(), null);
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("📋 Work Items asignados en proyecto '%s'", project));
            if (team != null && !team.trim().isEmpty()) {
                result.append(String.format(" (equipo: %s)", team));
            }
            result.append(":\n\n");
            
            if (groupBy != null) {
                result.append(formatGroupedResults(workItems, groupBy));
            } else {
                result.append(formatSimpleResults(workItems));
            }
            
            // Resumen
            result.append(generateSummary(workItems, includeCompleted));
            
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
                )),
                "isError", true
            );
        } catch (Exception e) {
            return Map.of(
                "content", List.of(Map.of(
                    "type", "text", 
                    "text", "Error inesperado: " + e.getMessage()
                )),
                "isError", true
            );
        }
    }
    
    /**
     * Formatea los resultados agrupados según el criterio especificado.
     */
    private String formatGroupedResults(List<WorkItem> workItems, String groupBy) {
        StringBuilder result = new StringBuilder();
        
        Map<String, List<WorkItem>> grouped = switch (groupBy) {
            case "state" -> workItems.stream()
                .collect(java.util.stream.Collectors.groupingBy(wi -> wi.getState() != null ? wi.getState() : "Sin Estado"));
            case "type" -> workItems.stream()
                .collect(java.util.stream.Collectors.groupingBy(wi -> wi.getWorkItemType() != null ? wi.getWorkItemType() : "Sin Tipo"));
            case "iteration" -> workItems.stream()
                .collect(java.util.stream.Collectors.groupingBy(wi -> wi.getIterationPath() != null ? wi.getIterationPath() : "Sin Iteración"));
            default -> Map.of("Todos", workItems);
        };
        
        grouped.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                String group = entry.getKey();
                List<WorkItem> items = entry.getValue();
                
                result.append(String.format("📁 %s (%d items):\n", group, items.size()));
                
                for (int i = 0; i < items.size(); i++) {
                    WorkItem workItem = items.get(i);
                    result.append(String.format("  %d. %s\n", i + 1, workItem.toDisplayString()));
                    
                    if (workItem.getRemainingWork() != null && workItem.getRemainingWork() > 0) {
                        result.append(String.format("     ⏱️ Trabajo restante: %.1f horas\n", workItem.getRemainingWork()));
                    }
                    
                    if (workItem.getStoryPoints() != null && workItem.getStoryPoints() > 0) {
                        result.append(String.format("     📊 Story Points: %.0f\n", workItem.getStoryPoints()));
                    }
                }
                result.append("\n");
            });
        
        return result.toString();
    }
    
    /**
     * Formatea los resultados de manera simple (sin agrupamiento).
     */
    private String formatSimpleResults(List<WorkItem> workItems) {
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < workItems.size(); i++) {
            WorkItem workItem = workItems.get(i);
            result.append(String.format("%d. %s\n", i + 1, workItem.toDisplayString()));
            
            if (workItem.getIterationPath() != null) {
                result.append(String.format("   📅 Iteración: %s\n", workItem.getIterationPath()));
            }
            
            if (workItem.getRemainingWork() != null && workItem.getRemainingWork() > 0) {
                result.append(String.format("   ⏱️ Trabajo restante: %.1f horas\n", workItem.getRemainingWork()));
            }
            
            if (workItem.getStoryPoints() != null && workItem.getStoryPoints() > 0) {
                result.append(String.format("   📊 Story Points: %.0f\n", workItem.getStoryPoints()));
            }
            
            result.append("\n");
        }
        
        return result.toString();
    }
    
    /**
     * Genera un resumen estadístico de los work items.
     */
    private String generateSummary(List<WorkItem> workItems, boolean includeCompleted) {
        StringBuilder summary = new StringBuilder();
        summary.append("📊 Resumen:\n");
        
        // Conteo por estado
        Map<String, Long> stateCount = workItems.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                wi -> wi.getState() != null ? wi.getState() : "Sin Estado",
                java.util.stream.Collectors.counting()
            ));
        
        summary.append(String.format("  • Total: %d work items\n", workItems.size()));
        stateCount.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                summary.append(String.format("  • %s: %d\n", entry.getKey(), entry.getValue()));
            });
        
        // Trabajo restante total
        double totalRemainingWork = workItems.stream()
            .filter(wi -> wi.getRemainingWork() != null)
            .mapToDouble(WorkItem::getRemainingWork)
            .sum();
        
        if (totalRemainingWork > 0) {
            summary.append(String.format("  • Trabajo restante total: %.1f horas\n", totalRemainingWork));
        }
        
        // Story points total
        double totalStoryPoints = workItems.stream()
            .filter(wi -> wi.getStoryPoints() != null)
            .mapToDouble(WorkItem::getStoryPoints)
            .sum();
        
        if (totalStoryPoints > 0) {
            summary.append(String.format("  • Story Points total: %.0f\n", totalStoryPoints));
        }
        
        return summary.toString();
    }
}
