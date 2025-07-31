package com.mcp.server.tools.azuredevops;

import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.tools.azuredevops.client.AzureDevOpsException;
import com.mcp.server.tools.azuredevops.model.WorkItem;
import com.mcp.server.tools.base.McpTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Herramienta MCP especializada para agregar comentarios a work items sin riesgo de sobreescribir contenido.
 * 
 * <p>Esta herramienta es la forma SEGURA de agregar comentarios a work items existentes.
 * A diferencia de update_workitem con description, esta herramienta:
 * <ul>
 *   <li>Solo agrega comentarios a la discusión (System.History)</li>
 *   <li>No puede sobreescribir la descripción original</li>
 *   <li>Proporciona confirmación visual del comentario agregado</li>
 * </ul>
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class AddCommentTool implements McpTool {
    
    private final AzureDevOpsClient azureDevOpsClient;
    
    @Autowired
    public AddCommentTool(AzureDevOpsClient azureDevOpsClient) {
        this.azureDevOpsClient = azureDevOpsClient;
    }
    
    @Override
    public String getName() {
        return "azuredevops_add_comment";
    }
    
    @Override
    public String getDescription() {
        return "Agrega un comentario a la discusión de un work item SIN sobreescribir la descripción original. Forma SEGURA de comentar work items.";
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
            "description", "ID del work item al que agregar el comentario"
        ));
        properties.put("comment", Map.of(
            "type", "string",
            "description", "Comentario a agregar a la discusión del work item"
        ));
        
        return Map.of(
            "type", "object",
            "properties", properties,
            "required", List.of("project", "workItemId", "comment")
        );
    }
    
    @Override
    public Map<String, Object> execute(Map<String, Object> arguments) {
        try {
            if (!azureDevOpsClient.isConfigured()) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", "❌ Azure DevOps no está configurado. Necesita configurar AZURE_DEVOPS_ORGANIZATION y AZURE_DEVOPS_PAT."
                    )),
                    "isError", true
                );
            }
            
            String project = (String) arguments.get("project");
            Number workItemIdNum = (Number) arguments.get("workItemId");
            String comment = (String) arguments.get("comment");
            
            // Validaciones
            if (project == null || project.trim().isEmpty()) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", "❌ El parámetro 'project' es requerido"
                    )),
                    "isError", true
                );
            }
            
            if (workItemIdNum == null) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", "❌ El parámetro 'workItemId' es requerido"
                    )),
                    "isError", true
                );
            }
            
            if (comment == null || comment.trim().isEmpty()) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", "❌ El parámetro 'comment' es requerido y no puede estar vacío"
                    )),
                    "isError", true
                );
            }
            
            Integer workItemId = workItemIdNum.intValue();
            
            // Obtener información actual del work item para confirmación
            WorkItem currentWorkItem = azureDevOpsClient.getWorkItem(project, workItemId, null, null);
            if (currentWorkItem == null) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", String.format("❌ No se encontró el work item #%d en el proyecto '%s'", workItemId, project)
                    )),
                    "isError", true
                );
            }
            
            // Preparar operación JSON Patch SOLO para agregar comentario
            List<Map<String, Object>> operations = List.of(
                Map.of(
                    "op", "add",
                    "path", "/fields/System.History",
                    "value", comment
                )
            );
            
            // Ejecutar actualización
            WorkItem result = azureDevOpsClient.updateWorkItem(project, workItemId, operations);
            
            // Construir respuesta detallada
            StringBuilder response = new StringBuilder();
            response.append(String.format("✅ Comentario agregado exitosamente al work item #%d%n%n", workItemId));
            
            // Información del work item
            response.append(String.format("📋 **Work Item**: %s%n", currentWorkItem.getTitle()));
            response.append(String.format("🔖 **Tipo**: %s%n", currentWorkItem.getWorkItemType()));
            response.append(String.format("📊 **Estado**: %s%n", currentWorkItem.getState()));
            
            if (currentWorkItem.getAssignedTo() != null) {
                response.append(String.format("👤 **Asignado**: %s%n", currentWorkItem.getAssignedTo()));
            }
            
            response.append(String.format("%n💬 **Comentario agregado**:%n```%n%s%n```%n%n", comment));
            
            response.append("🔗 **Verificar en Azure DevOps**:%n");
            response.append(String.format("https://dev.azure.com/%s/%s/_workitems/edit/%d%n%n", 
                System.getenv("AZURE_DEVOPS_ORGANIZATION"), project, workItemId));
            
            response.append("ℹ️  **Nota**: El comentario aparece en la pestaña 'Discussion' del work item.%n");
            response.append("✅ **Seguridad**: La descripción original NO fue modificada.");
            
            return Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", response.toString()
                ))
            );
            
        } catch (AzureDevOpsException e) {
            return Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", "❌ Error de Azure DevOps: " + e.getMessage()
                )),
                "isError", true
            );
        } catch (Exception e) {
            return Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", "❌ Error inesperado: " + e.getMessage()
                )),
                "isError", true
            );
        }
    }
}
