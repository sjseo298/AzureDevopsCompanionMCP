package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_comments_list
 * Lista comentarios de un work item.
 */
@Component
public class CommentsListTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_comments_list";
    private static final String DESC = "Lista comentarios de un work item.";
    private static final String API_VERSION_OVERRIDE = "7.0-preview.3";

    @Autowired
    public CommentsListTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String, Object>) base.get("properties");
        props.put("workItemId", Map.of("type","integer","description","ID del work item"));
        base.put("required", List.of("project","workItemId"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        String project = getProject(arguments);
        String team = getTeam(arguments);
        String wi = Objects.toString(arguments.get("workItemId"));
        String endpoint = "workItems/" + wi + "/comments";
        Map<String,Object> resp = azureService.getWitApiWithQuery(project, team, endpoint, null, API_VERSION_OVERRIDE);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return success(format(resp));
    }

    private String format(Map<String,Object> data) {
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
