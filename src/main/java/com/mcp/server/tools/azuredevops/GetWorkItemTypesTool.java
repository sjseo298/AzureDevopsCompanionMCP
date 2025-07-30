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
        properties.put("includeExtendedInfo", Map.of(
            "type", "boolean",
            "description", "Si incluir información extendida como estados, transiciones y todos los campos (por defecto: false)"
        ));
        properties.put("includeFieldDetails", Map.of(
            "type", "boolean", 
            "description", "Si incluir detalles completos de campos como tipos de datos y valores permitidos (por defecto: false)"
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
            Boolean includeExtendedInfo = (Boolean) arguments.getOrDefault("includeExtendedInfo", false);
            Boolean includeFieldDetails = (Boolean) arguments.getOrDefault("includeFieldDetails", false);
            
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
                    
                    // Información básica
                    typeInfo.put("name", workItem.get("name"));
                    typeInfo.put("description", workItem.getOrDefault("description", ""));
                    typeInfo.put("color", workItem.getOrDefault("color", ""));
                    typeInfo.put("isDisabled", workItem.getOrDefault("isDisabled", false));
                    
                    // Información extendida que se estaba perdiendo (solo si se solicita)
                    if (includeExtendedInfo) {
                        typeInfo.put("referenceName", workItem.get("referenceName"));
                        typeInfo.put("url", workItem.get("url"));
                        
                        // Información del icono
                        if (workItem.containsKey("icon")) {
                            typeInfo.put("icon", workItem.get("icon"));
                        }
                        
                        // Estados disponibles
                        if (workItem.containsKey("states")) {
                            typeInfo.put("states", workItem.get("states"));
                        }
                        
                        // Transiciones
                        if (workItem.containsKey("transitions")) {
                            typeInfo.put("transitions", workItem.get("transitions"));
                        }
                    }
                    
                    // Obtener campos (todos, no solo los obligatorios)
                    if (workItem.containsKey("fieldInstances")) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> fieldInstances = (List<Map<String, Object>>) workItem.get("fieldInstances");
                        List<Map<String, Object>> requiredFields = new ArrayList<>();
                        List<Map<String, Object>> allFields = new ArrayList<>();
                        
                        for (Map<String, Object> field : fieldInstances) {
                            Map<String, Object> fieldInfo = new HashMap<>();
                            fieldInfo.put("name", field.get("name"));
                            fieldInfo.put("referenceName", field.get("referenceName"));
                            fieldInfo.put("alwaysRequired", field.get("alwaysRequired"));
                            
                            // Información básica siempre incluida
                            if (field.containsKey("helpText")) {
                                fieldInfo.put("helpText", field.get("helpText"));
                            }
                            
                            // Información detallada solo si se solicita
                            if (includeFieldDetails) {
                                fieldInfo.put("defaultValue", field.get("defaultValue"));
                                fieldInfo.put("allowedValues", field.get("allowedValues"));
                                fieldInfo.put("fieldType", field.get("fieldType"));
                                fieldInfo.put("isEditable", field.get("isEditable"));
                                fieldInfo.put("isIdentity", field.get("isIdentity"));
                                fieldInfo.put("picklistId", field.get("picklistId"));
                                fieldInfo.put("url", field.get("url"));
                            }
                            
                            allFields.add(fieldInfo);
                            
                            // Separar campos obligatorios para mantener compatibilidad
                            if (field.containsKey("alwaysRequired") && Boolean.TRUE.equals(field.get("alwaysRequired"))) {
                                requiredFields.add(fieldInfo);
                            }
                        }
                        
                        typeInfo.put("requiredFields", requiredFields);
                        if (includeFieldDetails) {
                            typeInfo.put("allFields", allFields); // Nueva información completa
                        }
                    }
                    
                    // Información completa de campos disponibles
                    if (includeExtendedInfo && workItem.containsKey("fields")) {
                        typeInfo.put("fieldDefinitions", workItem.get("fields"));
                    }
                    
                    workItemTypes.add(typeInfo);
                }
                
                return Map.of("content", List.of(Map.of(
                    "type", "text",
                    "text", formatWorkItemTypes(workItemTypes, project, includeExtendedInfo, includeFieldDetails)
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
    
    private String formatWorkItemTypes(List<Map<String, Object>> workItemTypes, String project, 
                                      Boolean includeExtendedInfo, Boolean includeFieldDetails) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 **Tipos de Work Items en el proyecto: ").append(project).append("**\n\n");
        sb.append("Total de tipos encontrados: **").append(workItemTypes.size()).append("**\n\n");
        
        for (Map<String, Object> type : workItemTypes) {
            String name = (String) type.get("name");
            String description = (String) type.get("description");
            String referenceName = (String) type.get("referenceName");
            Boolean isDisabled = (Boolean) type.get("isDisabled");
            
            sb.append("🔹 **").append(name).append("**");
            if (Boolean.TRUE.equals(isDisabled)) {
                sb.append(" _(Deshabilitado)_");
            }
            sb.append("\n");
            
            if (includeExtendedInfo && referenceName != null && !referenceName.isEmpty()) {
                sb.append("   🔗 Nombre de referencia: `").append(referenceName).append("`\n");
            }
            
            if (description != null && !description.isEmpty()) {
                sb.append("   📝 ").append(description).append("\n");
            }
            
            // Estados disponibles (solo si se incluye información extendida)
            if (includeExtendedInfo) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> states = (List<Map<String, Object>>) type.get("states");
                if (states != null && !states.isEmpty()) {
                    sb.append("   🎯 **Estados disponibles:** ");
                    for (int i = 0; i < states.size(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(states.get(i).get("name"));
                    }
                    sb.append("\n");
                }
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
                    if (includeFieldDetails && field.containsKey("fieldType")) {
                        sb.append(" (").append(field.get("fieldType")).append(")");
                    }
                    sb.append("\n");
                }
            }
            
            // Mostrar total de campos disponibles (solo si se incluyen detalles de campos)
            if (includeFieldDetails) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> allFields = (List<Map<String, Object>>) type.get("allFields");
                if (allFields != null && !allFields.isEmpty()) {
                    sb.append("   📋 **Total de campos disponibles:** ").append(allFields.size()).append("\n");
                    
                    // Mostrar campos con valores permitidos
                    long fieldsWithValues = allFields.stream()
                        .mapToLong(field -> field.containsKey("allowedValues") && field.get("allowedValues") != null ? 1 : 0)
                        .sum();
                    if (fieldsWithValues > 0) {
                        sb.append("   🎯 **Campos con valores predefinidos:** ").append(fieldsWithValues).append("\n");
                    }
                }
            }
            
            sb.append("\n");
        }
        
        sb.append("💡 **Uso sugerido:**\n");
        sb.append("Para crear work items, usa los nombres exactos mostrados arriba.\n");
        sb.append("Ejemplo: `type: \"Task\"` o `type: \"User Story\"`\n\n");
        
        if (includeExtendedInfo || includeFieldDetails) {
            sb.append("ℹ️ **Información extendida capturada:**\n");
            if (includeExtendedInfo) {
                sb.append("• Referencias de nombre completas\n");
                sb.append("• Estados y transiciones disponibles\n");
            }
            if (includeFieldDetails) {
                sb.append("• Información completa de todos los campos (no solo obligatorios)\n");
                sb.append("• Metadatos de tipos de datos y validaciones\n");
                sb.append("• Valores permitidos y configuraciones de picklists\n");
            }
        } else {
            sb.append("💡 **Tip:** Usa `includeExtendedInfo: true` para ver estados y referencias.\n");
            sb.append("💡 **Tip:** Usa `includeFieldDetails: true` para ver detalles completos de campos.\n");
        }
        
        return sb.toString();
    }
}
