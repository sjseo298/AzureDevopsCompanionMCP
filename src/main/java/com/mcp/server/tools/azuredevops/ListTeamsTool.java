package com.mcp.server.tools.azuredevops;

import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.tools.azuredevops.client.AzureDevOpsException;
import com.mcp.server.tools.azuredevops.model.Team;
import com.mcp.server.tools.base.McpTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Herramienta MCP para listar equipos de un proyecto en Azure DevOps.
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class ListTeamsTool implements McpTool {
    
    private final AzureDevOpsClient azureDevOpsClient;
    
    @Autowired
    public ListTeamsTool(AzureDevOpsClient azureDevOpsClient) {
        this.azureDevOpsClient = azureDevOpsClient;
    }
    
    @Override
    public String getName() {
        return "azuredevops_list_teams";
    }
    
    @Override
    public String getDescription() {
        return "Lista todos los equipos disponibles en un proyecto de Azure DevOps";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "project", Map.of(
                    "type", "string",
                    "description", "Nombre o ID del proyecto"
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
                    "error", "Azure DevOps no está configurado. Necesita configurar AZURE_DEVOPS_ORGANIZATION y AZURE_DEVOPS_PAT."
                );
            }
            
            String project = (String) arguments.get("project");
            if (project == null || project.trim().isEmpty()) {
                return Map.of("error", "El parámetro 'project' es requerido");
            }
            
            List<Team> teams = azureDevOpsClient.listTeams(project);
            
            if (teams.isEmpty()) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", String.format("No se encontraron equipos en el proyecto '%s'.", project)
                    ))
                );
            }
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("Equipos en el proyecto '%s':%n%n", project));
            
            for (int i = 0; i < teams.size(); i++) {
                Team team = teams.get(i);
                result.append(String.format("%d. %s%n", i + 1, team.toDisplayString()));
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
