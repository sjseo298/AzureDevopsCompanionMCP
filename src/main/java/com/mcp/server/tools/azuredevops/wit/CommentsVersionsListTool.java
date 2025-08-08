package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_comments_versions_list
 * Lista las versiones de un comentario de un work item.
 */
@Component
public class CommentsVersionsListTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_comments_versions_list";
    private static final String DESC = "Lista las versiones de un comentario de un work item.";
    private static final String API_VERSION_OVERRIDE = "7.2-preview.3";

    @Autowired
    public CommentsVersionsListTool(AzureDevOpsClientService service) { super(service); }

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
        String endpoint = "workItems/" + wi + "/comments/" + ci + "/versions";
        Map<String,Object> resp = azureService.getWitApiWithQuery(project, team, endpoint, null, API_VERSION_OVERRIDE);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        if (resp.isEmpty()) return success("(Sin versiones o no disponible en esta organización)");
        return success(format(resp));
    }

    private String format(Map<String,Object> data) {
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
