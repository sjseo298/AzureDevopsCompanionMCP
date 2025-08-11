package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Helper para eliminar nodos de clasificación (areas/iterations) a nivel proyecto.
 */
@Component
public class WitClassificationNodesDeleteHelper {
    private final AzureDevOpsClientService azureService;

    public WitClassificationNodesDeleteHelper(AzureDevOpsClientService azureService) {
        this.azureService = azureService;
    }

    public void validateDelete(String project, String group, String path) {
        if (project == null || project.isBlank()) throw new IllegalArgumentException("Parámetro 'project' es obligatorio.");
        if (!"areas".equals(group) && !"iterations".equals(group)) throw new IllegalArgumentException("Parámetro 'group' inválido. Valores: areas, iterations.");
        if (path == null || path.isBlank()) throw new IllegalArgumentException("Parámetro 'path' es obligatorio.");
    }

    public Map<String,Object> deleteNode(String project, String team, String group, String path, String apiVersion) {
        String endpoint = "classificationnodes/" + group + "/" + path;
        return azureService.deleteWitApi(project, team, endpoint, apiVersion);
    }

    public String formatDeleteResponse(Map<String,Object> resp) {
        if (resp == null || resp.isEmpty()) return "(Respuesta vacía)";
        if (Boolean.TRUE.equals(resp.get("isHttpError"))) {
            String msg = Objects.toString(resp.get("message"), null);
            String type = Objects.toString(resp.get("typeKey"), null);
            String code = Objects.toString(resp.get("errorCode"), null);
            String status = Objects.toString(resp.get("httpStatus"), null);
            String reason = Objects.toString(resp.get("httpReason"), null);
            StringBuilder sb = new StringBuilder("Error remoto: ");
            if (code != null) sb.append(code).append(": ");
            if (msg != null) sb.append(msg);
            if (type != null) sb.append(" (type: ").append(type).append(")");
            if (status != null) sb.append(" [HTTP ").append(status).append("]");
            if (reason != null) sb.append(" ").append(reason);
            return sb.toString();
        }
        return "Nodo eliminado (si existía). Revise permisos y que el nodo no tenga hijos.";
    }
}
