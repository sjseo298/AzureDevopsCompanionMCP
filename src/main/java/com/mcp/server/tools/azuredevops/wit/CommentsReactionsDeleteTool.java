package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_comments_reactions_delete
 * Elimina una reacción de un comentario.
 */
@Component
public class CommentsReactionsDeleteTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_comments_reactions_delete";
    private static final String DESC = "Elimina una reacción (like/dislike/heart/hooray/smile/confused) de un comentario.";
    private static final String API_VERSION_OVERRIDE = "7.2-preview.1";

    @Autowired
    public CommentsReactionsDeleteTool(AzureDevOpsClientService service) { super(service); }

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
        String wi = Objects.toString(arguments.get("workItemId"));
        String ci = Objects.toString(arguments.get("commentId"));
        String type = Objects.toString(arguments.get("type")).toLowerCase(Locale.ROOT);
        String endpoint = "workItems/" + wi + "/comments/" + ci + "/reactions/" + type;
        Map<String,Object> resp = azureService.deleteWitApi(project, team, endpoint, API_VERSION_OVERRIDE);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return success("Reacción eliminada: " + type + " -> " + resp.toString());
    }
}
