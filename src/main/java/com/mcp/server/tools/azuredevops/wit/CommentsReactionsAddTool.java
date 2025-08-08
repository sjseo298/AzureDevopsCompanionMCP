package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_comments_reactions_add
 * Agrega una reacción a un comentario.
 */
@Component
public class CommentsReactionsAddTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_comments_reactions_add";
    private static final String DESC = "Agrega una reacción (like/dislike/heart/hooray/smile/confused) a un comentario.";
    private static final String API_VERSION_OVERRIDE = "7.2-preview.1";

    @Autowired
    public CommentsReactionsAddTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String, Object>) base.get("properties");
        props.put("workItemId", Map.of("type","integer","description","ID del work item"));
        props.put("commentId", Map.of("type","integer","description","ID del comentario"));
        props.put("type", Map.of("type","string","enum", List.of("like","dislike","heart","hooray","smile","confused"), "description","Tipo de reacción"));
        base.put("required", List.of("project","workItemId","commentId","type"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        String project = getProject(arguments);
        String team = getTeam(arguments);
        Object wiObj = arguments.get("workItemId");
        Object ciObj = arguments.get("commentId");
        Object typeObj = arguments.get("type");
        if (wiObj == null || !wiObj.toString().matches("\\d+")) return error("'workItemId' es requerido y debe ser numérico");
        if (ciObj == null || !ciObj.toString().matches("\\d+")) return error("'commentId' es requerido y debe ser numérico");
        if (typeObj == null) return error("'type' es requerido");
        String type = typeObj.toString().toLowerCase(Locale.ROOT);
        if (!List.of("like","dislike","heart","hooray","smile","confused").contains(type)) {
            return error("'type' inválido. Valores: like, dislike, heart, hooray, smile, confused");
        }
        String wi = wiObj.toString();
        String ci = ciObj.toString();
        String endpoint = "workItems/" + wi + "/comments/" + ci + "/reactions/" + type;
        Map<String,Object> resp = azureService.putWitApi(project, team, endpoint, Map.of(), API_VERSION_OVERRIDE);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        Object count = resp.get("count");
        Object me = resp.get("isCurrentUserEngaged");
        return success("Reacción agregada: " + type + " (count=" + (count!=null?count:1) + ", me=" + Boolean.TRUE.equals(me) + ")");
    }
}
