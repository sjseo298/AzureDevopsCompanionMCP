package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import java.util.List;
import java.util.Map;

/**
 * Helper para listar reacciones de un comentario de un work item.
 */
public class WitCommentsReactionsListHelper {
    private final AzureDevOpsClientService azureService;

    public WitCommentsReactionsListHelper(AzureDevOpsClientService azureService) {
        this.azureService = azureService;
    }

    public void validate(String project, Object wiObj, Object ciObj) {
        if (project == null || project.isBlank()) throw new IllegalArgumentException("Parámetro 'project' es obligatorio.");
        if (wiObj == null || !wiObj.toString().matches("\\d+")) throw new IllegalArgumentException("'workItemId' es requerido y debe ser numérico");
        if (ciObj == null || !ciObj.toString().matches("\\d+")) throw new IllegalArgumentException("'commentId' es requerido y debe ser numérico");
    }

    public Map<String,Object> fetchReactions(String project, String team, String wi, String ci, String apiVersion) {
        String endpoint = "workItems/" + wi + "/comments/" + ci + "/reactions";
        return azureService.getWitApiWithQuery(project, team, endpoint, null, apiVersion);
    }

    public String formatReactionsResponse(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Sin reacciones)";
        Object container = data.get("value");
        if (container instanceof List) {
            List<?> list = (List<?>) container;
            if (list.isEmpty()) return "(Sin reacciones)";
            StringBuilder sb = new StringBuilder("=== Reacciones ===\n\n");
            int i=1;
            for (Object o : list) {
                if (o instanceof Map) {
                    Map<?,?> m = (Map<?,?>) o;
                    Object type = m.get("type");
                    Object count = m.get("count");
                    Object engaged = m.get("isCurrentUserEngaged");
                    sb.append(i++).append(") ")
                      .append(type != null ? type : "?")
                      .append(" -> count=")
                      .append(count != null ? count : 0)
                      .append(", me=")
                      .append(Boolean.TRUE.equals(engaged))
                      .append('\n');
                }
            }
            return sb.toString();
        }
        return data.toString();
    }
}
