package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_comments_reactions_list
 * Lista reacciones de un comentario.
 */
@Component
public class CommentsReactionsListTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_comments_reactions_list";
    private static final String DESC = "Lista reacciones (tipo y conteo) de un comentario.";
    private static final String API_VERSION_OVERRIDE = "7.2-preview.1";

    @Autowired
    public CommentsReactionsListTool(AzureDevOpsClientService service) { super(service); }

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
        String wi = Objects.toString(arguments.get("workItemId"));
        String ci = Objects.toString(arguments.get("commentId"));
        String endpoint = "workItems/" + wi + "/comments/" + ci + "/reactions";
        Map<String,Object> resp = azureService.getWitApiWithQuery(project, team, endpoint, null, API_VERSION_OVERRIDE);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return success(format(resp));
    }

    private String format(Map<String,Object> data) {
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
