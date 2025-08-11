package com.mcp.server.tools.azuredevops.wit;


import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitCommentsVersionsGetHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_comments_versions_get
 * Obtiene una versión específica de un comentario.
 */
@Component
public class CommentsVersionsGetTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_comments_versions_get";
    private static final String DESC = "Obtiene una versión específica de un comentario.";
    private static final String API_VERSION_OVERRIDE = "7.2-preview.3";

    private final WitCommentsVersionsGetHelper helper;

    @Autowired
    public CommentsVersionsGetTool(AzureDevOpsClientService service) {
        super(service);
        this.helper = new WitCommentsVersionsGetHelper(service);
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String, Object>) base.get("properties");
        props.put("workItemId", Map.of("type","integer","description","ID del work item"));
        props.put("commentId", Map.of("type","integer","description","ID del comentario"));
        props.put("version", Map.of("type","integer","description","Número de versión"));
        base.put("required", List.of("project","workItemId","commentId","version"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        String project = getProject(arguments);
        String team = getTeam(arguments);
        Object wiObj = arguments.get("workItemId");
        Object ciObj = arguments.get("commentId");
        Object verObj = arguments.get("version");
        try {
            helper.validate(project, wiObj, ciObj, verObj);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
        String wi = wiObj.toString();
        String ci = ciObj.toString();
        String ver = verObj.toString();
        Map<String,Object> resp = helper.fetchVersion(project, team, wi, ci, ver, API_VERSION_OVERRIDE);
        if (resp.isEmpty()) return success("(Versión no disponible o endpoint no habilitado en esta organización)");
        return success(helper.formatVersionResponse(resp));
    }
}
