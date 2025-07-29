package com.mcp.server.tools.azuredevops;

import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.tools.azuredevops.client.AzureDevOpsException;
import com.mcp.server.tools.base.McpTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Herramienta MCP para obtener los tipos de work items en Azure DevOps.
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class GetWorkItemTypesTool implements McpTool {
    
    private final AzureDevOpsClient azureDevOpsClient;
    
    @Autowired
    public GetWorkItemTypesTool(AzureDevOpsClient azureDevOpsClient) {
        this.azureDevOpsClient = azureDevOpsClient;
    }

    @Override
    public String getName() {
        return "azuredevops_get_workitem_types";
    }

    @Override
    public String getDescription() {
        return "Obtiene todos los tipos de work items disponibles en un proyecto de Azure DevOps";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("project", Map.of(
            "type", "string",
            "description", "Nombre o ID del proyecto"
        ));
        
        return Map.of(
            "type", "object",
            "properties", properties,
            "required", List.of("project")
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> arguments) {
        try {
            String project = (String) arguments.get("project");
            
            if (project == null || project.trim().isEmpty()) {
                throw new IllegalArgumentException("El parámetro 'project' es requerido");
            }
            
            // Usar el método del cliente para obtener tipos de work items
            Map<String, Object> response = azureDevOpsClient.getWorkItemTypes(project);
            
            if (response.containsKey("value")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> workItemTypesRaw = (List<Map<String, Object>>) response.get("value");
                
                List<Map<String, Object>> workItemTypes = new ArrayList<>();
                
                for (Map<String, Object> workItem : workItemTypesRaw) {
                    Map<String, Object> typeInfo = new HashMap<>();
                    typeInfo.put("name", workItem.get("name"));
                    typeInfo.put("description", workItem.getOrDefault("description", ""));
                    typeInfo.put("color", workItem.getOrDefault("color", ""));
                    typeInfo.put("isDisabled", workItem.getOrDefault("isDisabled", false));
                    
                    // Obtener campos requeridos
                    if (workItem.containsKey("fieldInstances")) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> fieldInstances = (List<Map<String, Object>>) workItem.get("fieldInstances");
                        List<Map<String, Object>> requiredFields = new ArrayList<>();
                        
                        for (Map<String, Object> field : fieldInstances) {
                            if (field.containsKey("alwaysRequired") && Boolean.TRUE.equals(field.get("alwaysRequired"))) {
                                Map<String, Object> fieldInfo = new HashMap<>();
                                fieldInfo.put("name", field.get("name"));
                                fieldInfo.put("referenceName", field.get("referenceName"));
                                if (field.containsKey("helpText")) {
                                    fieldInfo.put("helpText", field.get("helpText"));
                                }
                                requiredFields.add(fieldInfo);
                            }
                        }
                        typeInfo.put("requiredFields", requiredFields);
                    }
                    
                    workItemTypes.add(typeInfo);
                }
                
                return Map.of("content", List.of(Map.of(
                    "type", "text",
                    "text", formatWorkItemTypes(workItemTypes, project)
                )));
                
            } else {
                return Map.of("content", List.of(Map.of(
                    "type", "text",
                    "text", "Error: No se pudieron obtener los tipos de work items para el proyecto " + project
                )));
            }
            
        } catch (AzureDevOpsException e) {
            return Map.of("content", List.of(Map.of(
                "type", "text",
                "text", "Error de Azure DevOps: " + e.getMessage()
            )));
        } catch (Exception e) {
            return Map.of("content", List.of(Map.of(
                "type", "text",
                "text", "Error al obtener tipos de work items: " + e.getMessage()
            )));
        }
    }
    
    private String formatWorkItemTypes(List<Map<String, Object>> workItemTypes, String project) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 **Tipos de Work Items en el proyecto: ").append(project).append("**\n\n");
        sb.append("Total de tipos encontrados: **").append(workItemTypes.size()).append("**\n\n");
        
        for (Map<String, Object> type : workItemTypes) {
            String name = (String) type.get("name");
            String description = (String) type.get("description");
            Boolean isDisabled = (Boolean) type.get("isDisabled");
            
            sb.append("🔹 **").append(name).append("**");
            if (Boolean.TRUE.equals(isDisabled)) {
                sb.append(" _(Deshabilitado)_");
            }
            sb.append("\n");
            
            if (description != null && !description.isEmpty()) {
                sb.append("   📝 ").append(description).append("\n");
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> requiredFields = (List<Map<String, Object>>) type.get("requiredFields");
            if (requiredFields != null && !requiredFields.isEmpty()) {
                sb.append("   🔑 **Campos requeridos:**\n");
                for (Map<String, Object> field : requiredFields) {
                    sb.append("      • ").append(field.get("name"));
                    if (field.containsKey("helpText") && !((String) field.get("helpText")).isEmpty()) {
                        sb.append(" - ").append(field.get("helpText"));
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }
        
        sb.append("💡 **Uso sugerido:**\n");
        sb.append("Para crear work items, usa los nombres exactos mostrados arriba.\n");
        sb.append("Ejemplo: `type: \"Task\"` o `type: \"User Story\"`");
        
        return sb.toString();
    }
}
