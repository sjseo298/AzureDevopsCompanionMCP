package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import java.util.List;
import java.util.Map;

/**
 * Helper para listar usuarios que reaccionaron a un comentario de un work item.
 */
public class WitCommentReactionsEngagedUsersListHelper {
    private final AzureDevOpsClientService azureService;

    public WitCommentReactionsEngagedUsersListHelper(AzureDevOpsClientService azureService) {
        this.azureService = azureService;
    }

    public void validate(String project, Object wiObj, Object ciObj) {
        if (project == null || project.isBlank()) throw new IllegalArgumentException("Parámetro 'project' es obligatorio.");
        if (wiObj == null || !wiObj.toString().matches("\\d+")) throw new IllegalArgumentException("'workItemId' es requerido y debe ser numérico");
        if (ciObj == null || !ciObj.toString().matches("\\d+")) throw new IllegalArgumentException("'commentId' es requerido y debe ser numérico");
    }

    public Map<String,Object> fetchEngagedUsers(String project, String team, String wi, String ci, String apiVersion) {
        String endpoint = "workItems/" + wi + "/comments/" + ci + "/reactions/users";
        return azureService.getWitApiWithQuery(project, team, endpoint, null, apiVersion);
    }

    public String formatEngagedUsersResponse(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Sin datos)";
        Object val = data.get("value");
        if (val instanceof List) {
            List<?> list = (List<?>) val;
            if (list.isEmpty()) return "(Sin usuarios/reacciones)";
            Object first = list.get(0);
            if (first instanceof Map && ((Map<?,?>) first).containsKey("displayName")) {
                StringBuilder sb = new StringBuilder("=== Usuarios que reaccionaron ===\n\n");
                int i=1;
                for (Object o : list) {
                    if (o instanceof Map) {
                        Map<?,?> m = (Map<?,?>) o;
                        Object dn = m.get("displayName");
                        Object un = m.get("uniqueName");
                        sb.append(i++).append(". ")
                          .append(dn != null ? dn : "(sin nombre)");
                        if (un != null) sb.append(" <").append(un).append(">");
                        sb.append('\n');
                    }
                }
                return sb.toString();
            } else if (first instanceof Map && ((Map<?,?>) first).containsKey("type")) {
                StringBuilder sb = new StringBuilder("=== Resumen de reacciones ===\n\n");
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
        }
        return data.toString();
    }
}
