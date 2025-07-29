package com.mcp.server.tools.azuredevops;

import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.tools.azuredevops.client.AzureDevOpsException;
import com.mcp.server.tools.base.McpTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Herramienta MCP para eliminar un work item en Azure DevOps.
 * 
 * <p>Permite eliminar work items enviándolos a la papelera de reciclaje
 * (por defecto) o eliminándolos permanentemente (con destroy=true).
 * 
 * <p><strong>ADVERTENCIA:</strong> La eliminación permanente (destroy=true) 
 * es IRREVERSIBLE. Use con extrema precaución.
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class DeleteWorkItemTool implements McpTool {
    
    private final AzureDevOpsClient azureDevOpsClient;
    
    @Autowired
    public DeleteWorkItemTool(AzureDevOpsClient azureDevOpsClient) {
        this.azureDevOpsClient = azureDevOpsClient;
    }
    
    @Override
    public String getName() {
        return "azuredevops_delete_workitem";
    }
    
    @Override
    public String getDescription() {
        return "Elimina un work item en Azure DevOps. Por defecto va a la papelera de reciclaje, " +
               "pero puede eliminarse permanentemente con destroy=true (IRREVERSIBLE)";
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
                    "description", "ID del work item a eliminar"
                ),
                "destroy", Map.of(
                    "type", "boolean",
                    "description", "Si true, elimina permanentemente (IRREVERSIBLE). " +
                                   "Por defecto false (envía a papelera de reciclaje)"
                ),
                "confirmDestroy", Map.of(
                    "type", "boolean",
                    "description", "Confirmación requerida cuando destroy=true para evitar eliminaciones accidentales"
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
                        "text", "❌ Azure DevOps no está configurado.\n\n" +
                               "🔧 Necesita configurar las variables de entorno:\n" +
                               "• AZURE_DEVOPS_ORGANIZATION\n" +
                               "• AZURE_DEVOPS_PAT"
                    ))
                );
            }
            
            String project = (String) arguments.get("project");
            Number workItemIdNum = (Number) arguments.get("workItemId");
            Boolean destroy = (Boolean) arguments.get("destroy");
            Boolean confirmDestroy = (Boolean) arguments.get("confirmDestroy");
            
            if (project == null || project.trim().isEmpty()) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", "❌ Error: El parámetro 'project' es requerido"
                    ))
                );
            }
            
            if (workItemIdNum == null) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", "❌ Error: El parámetro 'workItemId' es requerido"
                    ))
                );
            }
            
            Integer workItemId = workItemIdNum.intValue();
            
            // Validar que destroy=true requiere confirmación
            if (destroy != null && destroy && (confirmDestroy == null || !confirmDestroy)) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", "⚠️  ERROR: Confirmación requerida para eliminación permanente\n\n" +
                               "🔒 La eliminación permanente (destroy=true) requiere confirmación explícita.\n\n" +
                               "✅ Para confirmar, agregue el parámetro:\n" +
                               "   confirmDestroy: true\n\n" +
                               "⚠️  ADVERTENCIA: Esta acción es IRREVERSIBLE.\n" +
                               "❌ Los work items eliminados permanentemente NO se pueden recuperar.\n\n" +
                               "💡 Alternativa: Use eliminación normal (sin destroy=true) para enviar a papelera de reciclaje."
                    ))
                );
            }
            
            // Obtener información del work item antes de eliminarlo
            String workItemInfo;
            try {
                var workItem = azureDevOpsClient.getWorkItem(project, workItemId, null, null);
                workItemInfo = workItem != null ? workItem.toDisplayString() : "Work item #" + workItemId;
            } catch (Exception e) {
                workItemInfo = "Work item #" + workItemId + " (no se pudo obtener información)";
            }
            
            // Ejecutar la eliminación
            Map<String, Object> deleteResult = azureDevOpsClient.deleteWorkItem(project, workItemId, destroy);
            
            StringBuilder result = new StringBuilder();
            result.append("✅ Work item eliminado exitosamente\n\n");
            result.append(String.format("📋 Work item: %s\n", workItemInfo));
            result.append(String.format("🆔 ID: %d\n", workItemId));
            result.append(String.format("📁 Proyecto: %s\n", project));
            
            if (destroy != null && destroy) {
                result.append("⚠️  Tipo de eliminación: PERMANENTE (IRREVERSIBLE)\n");
                result.append("❌ El work item NO puede ser restaurado\n");
            } else {
                result.append("🗑️  Tipo de eliminación: Enviado a papelera de reciclaje\n");
                result.append("♻️  El work item puede ser restaurado desde la papelera\n");
            }
            
            // Agregar información adicional del resultado si está disponible
            if (deleteResult != null && deleteResult.containsKey("deletedDate")) {
                result.append(String.format("📅 Fecha de eliminación: %s\n", deleteResult.get("deletedDate")));
            }
            
            if (deleteResult != null && deleteResult.containsKey("deletedBy")) {
                result.append(String.format("👤 Eliminado por: %s\n", deleteResult.get("deletedBy")));
            }
            
            result.append("\n");
            if (destroy != null && destroy) {
                result.append("⚠️  RECORDATORIO: La eliminación permanente no se puede deshacer.");
            } else {
                result.append("ℹ️  Para restaurar el work item, use la interfaz web de Azure DevOps > Papelera de reciclaje.");
            }
            
            return Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", result.toString()
                ))
            );
            
        } catch (AzureDevOpsException e) {
            // Manejo específico para errores conocidos de eliminación permanente
            if (e.getMessage().contains("VS402324") || e.getMessage().contains("destroy") || e.getMessage().contains("access is denied")) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", "❌ Error: No se puede realizar la eliminación permanente.\n\n" +
                               "🔒 Motivos posibles:\n" +
                               "• No tiene permisos para eliminación permanente\n" +
                               "• El work item ya fue eliminado permanentemente\n" +
                               "• La eliminación permanente está restringida en esta organización\n\n" +
                               "💡 Alternativa: Use eliminación normal (sin destroy=true) para enviar a papelera de reciclaje.\n\n" +
                               "📋 Error técnico: " + e.getMessage()
                    ))
                );
            }
            return Map.of(
                "content", List.of(Map.of(
                    "type", "text", 
                    "text", "❌ Error de Azure DevOps: " + e.getMessage()
                ))
            );
        } catch (Exception e) {
            return Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", "❌ Error inesperado: " + e.getMessage()
                ))
            );
        }
    }
}
