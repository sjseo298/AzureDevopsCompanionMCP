package com.mcp.server.tools.azuredevops;

import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.tools.azuredevops.client.AzureDevOpsException;
import com.mcp.server.tools.azuredevops.model.WorkItem;
import com.mcp.server.tools.base.McpTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Herramienta MCP para actualizar work items en Azure DevOps.
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class UpdateWorkItemTool implements McpTool {
    
    private final AzureDevOpsClient azureDevOpsClient;
    
    @Autowired
    public UpdateWorkItemTool(AzureDevOpsClient azureDevOpsClient) {
        this.azureDevOpsClient = azureDevOpsClient;
    }
    
    @Override
    public String getName() {
        return "azuredevops_update_workitem";
    }
    
    @Override
    public String getDescription() {
        return "Actualiza un work item existente en Azure DevOps. Permite cambiar estado, asignación, campos personalizados, etc.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("project", Map.of(
            "type", "string",
            "description", "Nombre o ID del proyecto"
        ));
        properties.put("workItemId", Map.of(
            "type", "number",
            "description", "ID del work item a actualizar"
        ));
        properties.put("title", Map.of(
            "type", "string",
            "description", "Nuevo título del work item (opcional)"
        ));
        properties.put("description", Map.of(
            "type", "string",
            "description", "Nueva descripción del work item (opcional)"
        ));
        properties.put("state", Map.of(
            "type", "string",
            "description", "Nuevo estado (ej: Active, Done, Closed, etc.) (opcional)"
        ));
        properties.put("assignedTo", Map.of(
            "type", "string",
            "description", "Email o nombre del nuevo usuario asignado (opcional)"
        ));
        properties.put("iterationPath", Map.of(
            "type", "string",
            "description", "Nueva ruta de iteración (opcional)"
        ));
        properties.put("areaPath", Map.of(
            "type", "string",
            "description", "Nueva ruta de área (opcional)"
        ));
        properties.put("remainingWork", Map.of(
            "type", "number",
            "description", "Nuevo trabajo restante en horas (opcional)"
        ));
        properties.put("completedWork", Map.of(
            "type", "number",
            "description", "Trabajo completado en horas (opcional)"
        ));
        properties.put("storyPoints", Map.of(
            "type", "number",
            "description", "Nuevos story points (opcional)"
        ));
        properties.put("priority", Map.of(
            "type", "number",
            "description", "Nueva prioridad (1-4, donde 1 es la más alta) (opcional)"
        ));
        properties.put("tags", Map.of(
            "type", "string",
            "description", "Nuevos tags separados por punto y coma (opcional)"
        ));
        properties.put("revision", Map.of(
            "type", "number",
            "description", "Número de revisión para control de concurrencia (opcional)"
        ));
        
        return Map.of(
            "type", "object",
            "properties", properties,
            "required", List.of("project", "workItemId")
        );
    }
    
    @Override
    public Map<String, Object> execute(Map<String, Object> arguments) {
        try {
            if (!azureDevOpsClient.isConfigured()) {
                return Map.of("error",
                    "Azure DevOps no está configurado. Necesita configurar AZURE_DEVOPS_ORGANIZATION y AZURE_DEVOPS_PAT."
                );
            }
            
            String project = (String) arguments.get("project");
            Number workItemIdNum = (Number) arguments.get("workItemId");
            String title = (String) arguments.get("title");
            String description = (String) arguments.get("description");
            String state = (String) arguments.get("state");
            String assignedTo = (String) arguments.get("assignedTo");
            String iterationPath = (String) arguments.get("iterationPath");
            String areaPath = (String) arguments.get("areaPath");
            Number remainingWorkNum = (Number) arguments.get("remainingWork");
            Number completedWorkNum = (Number) arguments.get("completedWork");
            Number storyPointsNum = (Number) arguments.get("storyPoints");
            Number priorityNum = (Number) arguments.get("priority");
            String tags = (String) arguments.get("tags");
            String comment = (String) arguments.get("comment");
            Number revisionNum = (Number) arguments.get("revision");
            
            if (project == null || project.trim().isEmpty()) {
                return Map.of("error", "El parámetro 'project' es requerido");
            }
            
            if (workItemIdNum == null) {
                return Map.of("error", "El parámetro 'workItemId' es requerido");
            }
            
            Integer workItemId = workItemIdNum.intValue();
            
            // Construir operaciones JSON Patch
            List<Map<String, Object>> operations = new ArrayList<>();
            
            // Verificación de revisión para control de concurrencia
            if (revisionNum != null) {
                operations.add(Map.of(
                    "op", "test",
                    "path", "/rev",
                    "value", revisionNum.intValue()
                ));
            }
            
            // Actualizar campos según lo especificado
            if (title != null && !title.trim().isEmpty()) {
                operations.add(Map.of(
                    "op", "add",
                    "path", "/fields/System.Title",
                    "value", title
                ));
            }
            
            if (description != null) {
                operations.add(Map.of(
                    "op", "add",
                    "path", "/fields/System.Description",
                    "value", description
                ));
            }
            
            if (state != null && !state.trim().isEmpty()) {
                operations.add(Map.of(
                    "op", "add",
                    "path", "/fields/System.State",
                    "value", state
                ));
            }
            
            if (assignedTo != null) {
                if (assignedTo.trim().isEmpty()) {
                    // Quitar asignación
                    operations.add(Map.of(
                        "op", "remove",
                        "path", "/fields/System.AssignedTo"
                    ));
                } else {
                    operations.add(Map.of(
                        "op", "add",
                        "path", "/fields/System.AssignedTo",
                        "value", assignedTo
                    ));
                }
            }
            
            if (iterationPath != null && !iterationPath.trim().isEmpty()) {
                operations.add(Map.of(
                    "op", "add",
                    "path", "/fields/System.IterationPath",
                    "value", iterationPath
                ));
            }
            
            if (areaPath != null && !areaPath.trim().isEmpty()) {
                operations.add(Map.of(
                    "op", "add",
                    "path", "/fields/System.AreaPath",
                    "value", areaPath
                ));
            }
            
            if (remainingWorkNum != null) {
                operations.add(Map.of(
                    "op", "add",
                    "path", "/fields/Microsoft.VSTS.Scheduling.RemainingWork",
                    "value", remainingWorkNum.doubleValue()
                ));
            }
            
            if (completedWorkNum != null) {
                operations.add(Map.of(
                    "op", "add",
                    "path", "/fields/Microsoft.VSTS.Scheduling.CompletedWork",
                    "value", completedWorkNum.doubleValue()
                ));
            }
            
            if (storyPointsNum != null) {
                operations.add(Map.of(
                    "op", "add",
                    "path", "/fields/Microsoft.VSTS.Scheduling.StoryPoints",
                    "value", storyPointsNum.doubleValue()
                ));
            }
            
            if (priorityNum != null) {
                operations.add(Map.of(
                    "op", "add",
                    "path", "/fields/Microsoft.VSTS.Common.Priority",
                    "value", priorityNum.intValue()
                ));
            }
            
            if (tags != null) {
                operations.add(Map.of(
                    "op", "add",
                    "path", "/fields/System.Tags",
                    "value", tags
                ));
            }
            
            // Agregar comentario al historial
            if (comment != null && !comment.trim().isEmpty()) {
                operations.add(Map.of(
                    "op", "add",
                    "path", "/fields/System.History",
                    "value", comment
                ));
            }
            
            if (operations.isEmpty()) {
                return Map.of("error", "No se especificaron cambios para realizar. Debe proporcionar al menos un campo a actualizar.");
            }
            
            // Actualizar el work item
            WorkItem updatedWorkItem = azureDevOpsClient.updateWorkItem(project, workItemId, operations);
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("✅ Work item #%d actualizado exitosamente:%n%n", workItemId));
            result.append(updatedWorkItem.toDetailedString());
            
            if (comment != null && !comment.trim().isEmpty()) {
                result.append(String.format("💬 Comentario agregado: %s%n", comment));
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
