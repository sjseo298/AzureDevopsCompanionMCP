package com.mcp.server.tools.azuredevops.wit;


import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitCommentsAddHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_comments_add
 * Agrega un comentario a un work item.
 */
@Component
public class CommentsAddTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_comments_add";
    private static final String DESC = "Agrega un comentario a un work item.";
    private static final String API_VERSION_OVERRIDE = "7.0-preview.3";

    private final WitCommentsAddHelper helper;

    @Autowired
    public CommentsAddTool(AzureDevOpsClientService service) {
        super(service);
        this.helper = new WitCommentsAddHelper(service);
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String, Object>) base.get("properties");
        props.put("workItemId", Map.of("type","integer","description","ID del work item"));
        props.put("text", Map.of("type","string","description","Texto del comentario"));
        base.put("required", List.of("project","workItemId","text"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        String project = getProject(arguments);
        String team = getTeam(arguments);
        Object wiObj = arguments.get("workItemId");
        Object textObj = arguments.get("text");
        try {
            helper.validate(project, wiObj, textObj);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
        String wi = wiObj.toString();
        String text = textObj.toString();
        Map<String,Object> resp = helper.addComment(project, team, wi, text, API_VERSION_OVERRIDE);
        return success(helper.formatAddResponse(resp));
    }
}
