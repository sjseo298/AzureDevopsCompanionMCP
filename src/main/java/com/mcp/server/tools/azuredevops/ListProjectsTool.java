package com.mcp.server.tools.azuredevops;

import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.tools.azuredevops.client.AzureDevOpsException;
import com.mcp.server.tools.azuredevops.model.Project;
import com.mcp.server.tools.base.McpTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Herramienta MCP para listar proyectos de Azure DevOps.
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class ListProjectsTool implements McpTool {
    
    private final AzureDevOpsClient azureDevOpsClient;
    
    @Autowired
    public ListProjectsTool(AzureDevOpsClient azureDevOpsClient) {
        this.azureDevOpsClient = azureDevOpsClient;
    }
    
    @Override
    public String getName() {
        return "azuredevops_list_projects";
    }
    
    @Override
    public String getDescription() {
        return "Lista todos los proyectos disponibles en la organización de Azure DevOps";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(),
            "required", List.of()
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
            
            List<Project> projects = azureDevOpsClient.listProjects();
            
            if (projects.isEmpty()) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", "No se encontraron proyectos en la organización."
                    ))
                );
            }
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("Proyectos en la organización '%s':%n%n", azureDevOpsClient.getOrganization()));
            
            for (int i = 0; i < projects.size(); i++) {
                Project project = projects.get(i);
                result.append(String.format("%d. %s%n", i + 1, project.toDisplayString()));
                if (project.description() != null && !project.description().trim().isEmpty()) {
                    result.append(String.format("   Descripción: %s%n", project.description()));
                }
                result.append("%n");
            }
            
            return Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", result.toString()
                ))
            );
            
        } catch (AzureDevOpsException e) {
            return Map.of("error", "Error de Azure DevOps: " + e.getMessage());
        } catch (Exception e) {
            return Map.of("error", "Error inesperado: " + e.getMessage());
        }
    }
}
