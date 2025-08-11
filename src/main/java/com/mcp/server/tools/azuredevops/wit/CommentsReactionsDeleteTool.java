package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitCommentsReactionsDeleteHelper;
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

    private final WitCommentsReactionsDeleteHelper helper;

    @Autowired
    public CommentsReactionsDeleteTool(AzureDevOpsClientService service) {
        super(service);
        this.helper = new WitCommentsReactionsDeleteHelper(service);
    }

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
        try {
            helper.validate(project, wiObj, ciObj, typeObj);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
        String wi = wiObj.toString();
        String ci = ciObj.toString();
        String type = typeObj.toString().toLowerCase();
        Map<String,Object> resp = helper.deleteReaction(project, team, wi, ci, type, API_VERSION_OVERRIDE);
        return success(helper.formatDeleteResponse(resp, type));
    }
}
