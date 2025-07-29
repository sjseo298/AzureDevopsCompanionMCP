package com.mcp.server.tools.azuredevops;

import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.tools.base.McpTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Versión simplificada para test de GetAssignedWorkTool
 */
@Component("getAssignedWorkToolTest")
public class GetAssignedWorkToolTest implements McpTool {
    
    private final AzureDevOpsClient azureDevOpsClient;
    
    @Autowired
    public GetAssignedWorkToolTest(AzureDevOpsClient azureDevOpsClient) {
        this.azureDevOpsClient = azureDevOpsClient;
    }
    
    @Override
    public String getName() {
        return "azuredevops_get_assigned_work_test";
    }
    
    @Override
    public String getDescription() {
        return "Test simplificado para obtener work items asignados";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "project", Map.of(
                    "type", "string",
                    "description", "Nombre del proyecto"
                )
            ),
            "required", List.of("project")
        );
    }
    
    @Override
    public Map<String, Object> execute(Map<String, Object> arguments) {
        try {
            String project = (String) arguments.get("project");
            
            if (project == null || project.trim().isEmpty()) {
                return Map.of("error", "El parámetro 'project' es requerido");
            }

            return Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", "✅ GetAssignedWork TEST funcionando correctamente para proyecto: " + project
                ))
            );
            
        } catch (Exception e) {
            return Map.of("error", "Error en test: " + e.getMessage());
        }
    }
}
