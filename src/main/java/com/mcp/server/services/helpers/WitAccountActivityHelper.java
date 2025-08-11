package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Helper para actividades recientes de work items (account my work recent activity) a nivel organización.
 * Encapsula fetch y formateo, dejando la Tool como adaptador MCP.
 */
@Service
public class WitAccountActivityHelper {

    private final AzureDevOpsClientService azureService;

    public WitAccountActivityHelper(AzureDevOpsClientService azureService) {
        this.azureService = azureService;
    }

    // No hay validaciones específicas (sin parámetros de entrada), método reservado por consistencia.
    public void validate() { /* no-op */ }

    public Map<String,Object> fetchRecentActivity() {
        // Se usa el área 'work' en lugar de 'wit' según restricción del entorno actual
        return azureService.getCoreApi("work/accountmyworkrecentactivity", null);
    }

    @SuppressWarnings("unchecked")
    public String formatRecentActivity(Map<String,Object> resp) {
        if (resp == null || resp.isEmpty()) return "(Respuesta vacía)";
        Object value = resp.get("value");
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            StringBuilder sb = new StringBuilder("=== Recent Activity ===\n\n");
            int i = 1;
            for (Object o : list) {
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
                    sb.append('\n');
                }
            }
            return sb.toString();
        }
        return null; // delega a salida raw en el Tool
    }
}
