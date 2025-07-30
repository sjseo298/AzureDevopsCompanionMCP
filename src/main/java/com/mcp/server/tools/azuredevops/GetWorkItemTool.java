package com.mcp.server.tools.azuredevops;

import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.tools.azuredevops.client.AzureDevOpsException;
import com.mcp.server.tools.azuredevops.model.WorkItem;
import com.mcp.server.tools.base.McpTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Herramienta MCP para obtener detalles de un work item específico.
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class GetWorkItemTool implements McpTool {
    
    private final AzureDevOpsClient azureDevOpsClient;
    
    @Autowired
    public GetWorkItemTool(AzureDevOpsClient azureDevOpsClient) {
        this.azureDevOpsClient = azureDevOpsClient;
    }
    
    @Override
    public String getName() {
        return "azuredevops_get_workitem";
    }
    
    @Override
    public String getDescription() {
        return "Obtiene los detalles completos de un work item específico por su ID";
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
                "workItemId", Map.of(
                    "type", "number",
                    "description", "ID del work item a obtener"
                ),
                "expand", Map.of(
                    "type", "string",
                    "description", "Expansiones adicionales (Relations, Links, etc.) - opcional"
                ),
                "fields", Map.of(
                    "type", "array",
                    "items", Map.of("type", "string"),
                    "description", "Campos específicos a obtener (opcional - si no se especifica, obtiene todos)"
                )
            ),
            "required", List.of("project", "workItemId")
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
            Number workItemIdNum = (Number) arguments.get("workItemId");
            String expand = (String) arguments.get("expand");
            @SuppressWarnings("unchecked")
            List<String> fields = (List<String>) arguments.get("fields");
            
            if (project == null || project.trim().isEmpty()) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", "El parámetro 'project' es requerido"
                    ))
                );
            }
            
            if (workItemIdNum == null) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", "El parámetro 'workItemId' es requerido"
                    ))
                );
            }
            
            Integer workItemId = workItemIdNum.intValue();
            
            WorkItem workItem = azureDevOpsClient.getWorkItem(project, workItemId, fields, expand);
            
            if (workItem == null) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", String.format("No se encontró el work item con ID %d en el proyecto '%s'", workItemId, project)
                    ))
                );
            }
            
            StringBuilder result = new StringBuilder();
            result.append(workItem.toDetailedString());
            
            // Información adicional si está disponible
            if (workItem.getCreatedDate() != null) {
                result.append(String.format("Creado: %s%n", workItem.getCreatedDate().toString()));
            }
            
            if (workItem.getChangedDate() != null) {
                result.append(String.format("Última modificación: %s%n", workItem.getChangedDate().toString()));
            }
            
            result.append(String.format("URL: %s%n", workItem.url()));
            
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
}
