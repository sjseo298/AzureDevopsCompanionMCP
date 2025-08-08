package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_comment_reactions_engaged_users_list
 * Lista usuarios que reaccionaron a un comentario en un work item.
 */
@Component
public class CommentReactionsEngagedUsersListTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_comment_reactions_engaged_users_list";
    private static final String DESC = "Lista usuarios que han reaccionado a un comentario de un work item.";
    private static final String API_VERSION_OVERRIDE = "7.2-preview";

    @Autowired
    public CommentReactionsEngagedUsersListTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String, Object>) base.get("properties");
        props.put("workItemId", Map.of("type","integer","description","ID del work item"));
        props.put("commentId", Map.of("type","integer","description","ID del comentario"));
        base.put("required", List.of("project","workItemId","commentId"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        String project = getProject(arguments);
        String team = getTeam(arguments);
        Object wiObj = arguments.get("workItemId");
        Object ciObj = arguments.get("commentId");
        if (wiObj == null || !wiObj.toString().matches("\\d+")) return error("'workItemId' es requerido y debe ser numérico");
        if (ciObj == null || !ciObj.toString().matches("\\d+")) return error("'commentId' es requerido y debe ser numérico");
        String wi = wiObj.toString();
        String ci = ciObj.toString();
        String endpoint = "workItems/" + wi + "/comments/" + ci + "/reactions/users";
        Map<String,Object> resp = azureService.getWitApiWithQuery(project, team, endpoint, null, API_VERSION_OVERRIDE);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return success(format(resp));
    }

    private String format(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Sin datos)";
        Object val = data.get("value");
        if (val instanceof List) {
            List<?> list = (List<?>) val;
            if (list.isEmpty()) return "(Sin usuarios/reacciones)";
            // Detectar tipo de elementos
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
