package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitCommentsListHelper;
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

    private final WitCommentsListHelper helper;

    @Autowired
    public CommentsListTool(AzureDevOpsClientService service) {
        super(service);
        this.helper = new WitCommentsListHelper(service);
    }

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
        Object wiObj = arguments.get("workItemId");
        try {
            helper.validate(project, wiObj);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
        String wi = wiObj.toString();
        Map<String,Object> resp = helper.fetchComments(project, team, wi, API_VERSION_OVERRIDE);
        return success(helper.formatCommentsResponse(resp));
    }
}
