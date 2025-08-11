package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import java.util.List;
import java.util.Map;

/**
 * Helper para listar comentarios de un work item.
 */
public class WitCommentsListHelper {
    private final AzureDevOpsClientService azureService;

    public WitCommentsListHelper(AzureDevOpsClientService azureService) {
        this.azureService = azureService;
    }

    public void validate(String project, Object wiObj) {
        if (project == null || project.isBlank()) throw new IllegalArgumentException("Parámetro 'project' es obligatorio.");
        if (wiObj == null || !wiObj.toString().matches("\\d+")) throw new IllegalArgumentException("'workItemId' es requerido y debe ser numérico");
    }

    public Map<String,Object> fetchComments(String project, String team, String wi, String apiVersion) {
        String endpoint = "workItems/" + wi + "/comments";
        return azureService.getWitApiWithQuery(project, team, endpoint, null, apiVersion);
    }

    public String formatCommentsResponse(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Sin comentarios)";
        Object container = data.get("comments");
        if (!(container instanceof List)) container = data.get("value");
        if (container instanceof List) {
            List<?> list = (List<?>) container;
            if (list.isEmpty()) return "(Sin comentarios)";
            StringBuilder sb = new StringBuilder("=== Comentarios ===\n\n");
            int i=1;
            for (Object o : list) {
                if (o instanceof Map) {
                    Map<?,?> m = (Map<?,?>) o;
                    Object id = m.get("id");
                    Object text = m.get("text");
                    String t = text == null ? "" : text.toString();
                    if (t.length() > 200) t = t.substring(0, 200) + "…";
                    sb.append(i++).append(") ").append(id != null ? id : "?").append(": ").append(t).append('\n');
                }
            }
            return sb.toString();
        }
        return data.toString();
    }
}
