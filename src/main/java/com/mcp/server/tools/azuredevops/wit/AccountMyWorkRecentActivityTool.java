package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool: azuredevops_wit_get_account_my_work_recent_activity
 * Endpoint org-level: GET /_apis/wit/accountmyworkrecentactivity
 * No requiere project/team.
 */
@Component
public class AccountMyWorkRecentActivityTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_get_account_my_work_recent_activity";
    private static final String DESC = "Lista actividades recientes de work items del usuario autenticado (nivel organización).";

    @Autowired
    public AccountMyWorkRecentActivityTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        // No requiere 'project'
    }

    @Override
    public Map<String, Object> getInputSchema() {
        // Sin parámetros obligatorios
        Map<String,Object> props = new LinkedHashMap<>();
        return Map.of(
            "type", "object",
            "properties", props,
            "required", List.of()
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        // Usar el área 'work' ya que /wit/... no está disponible en esta org
        Map<String,Object> resp = azureService.getCoreApi("work/accountmyworkrecentactivity", null);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return success(format(resp));
    }

    private String format(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Respuesta vacía)";
        if (data.containsKey("error")) return "Error remoto: " + data.get("error");
        Object value = data.get("value");
        if (value instanceof List) {
            StringBuilder sb = new StringBuilder("=== Recent Activity ===\n\n");
            int i = 1;
            for (Object o : (List<?>) value) {
                if (o instanceof Map) {
                    Map<?,?> m = (Map<?,?>) o;
                    Object id = m.get("workItemId");
                    Object type = m.get("activityType");
                    Object date = m.get("activityDate");
                    Object title = m.get("title");
                    sb.append(i++).append(". ");
                    if (title != null) sb.append(title).append(" ");
                    if (id != null) sb.append("[#").append(id).append("] ");
                    if (type != null) sb.append("(").append(type).append(") ");
                    if (date != null) sb.append("@ ").append(date);
                    sb.append("\n");
                }
            }
            return sb.toString();
        }
        return data.toString();
    }
}
