package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import java.util.List;
import java.util.Map;

/**
 * Helper para listar versiones de un comentario de un work item.
 */
public class WitCommentsVersionsListHelper {
    private final AzureDevOpsClientService azureService;

    public WitCommentsVersionsListHelper(AzureDevOpsClientService azureService) {
        this.azureService = azureService;
    }

    public void validate(String project, Object wiObj, Object ciObj) {
        if (project == null || project.isBlank()) throw new IllegalArgumentException("Parámetro 'project' es obligatorio.");
        if (wiObj == null || !wiObj.toString().matches("\\d+")) throw new IllegalArgumentException("'workItemId' es requerido y debe ser numérico");
        if (ciObj == null || !ciObj.toString().matches("\\d+")) throw new IllegalArgumentException("'commentId' es requerido y debe ser numérico");
    }

    public Map<String,Object> fetchVersions(String project, String team, String wi, String ci, String apiVersion) {
        String endpoint = "workItems/" + wi + "/comments/" + ci + "/versions";
        return azureService.getWitApiWithQuery(project, team, endpoint, null, apiVersion);
    }

    public String formatVersionsResponse(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Sin versiones)";
        Object container = data.get("value");
        if (container instanceof List) {
            List<?> list = (List<?>) container;
            if (list.isEmpty()) return "(Sin versiones)";
            StringBuilder sb = new StringBuilder("=== Versiones de Comentario ===\n\n");
            int i=1;
            for (Object o : list) {
                if (o instanceof Map) {
                    Map<?,?> m = (Map<?,?>) o;
                    Object ver = m.get("version");
                    Object text = m.get("text");
                    sb.append(i++).append(") v").append(ver != null ? ver : "?").append(": ")
                      .append(text != null ? text.toString() : "").append('\n');
                }
            }
            return sb.toString();
        }
        return data.toString();
    }
}
