package com.mcp.server.tools.azuredevops.wit;


import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitRevisionsHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_revisions_get
 * Obtiene una revisión específica de un work item.
 * Endpoint: GET /{project}/_apis/wit/workItems/{id}/revisions/{rev}?api-version=7.2-preview.3
 */
@Component
public class RevisionsGetTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_revisions_get";
    private static final String DESC = "Obtiene una revisión específica de un work item.";
    private static final String API_VERSION = "7.2-preview.3";

    private final WitRevisionsHelper helper;

    public RevisionsGetTool(AzureDevOpsClientService svc) {
        super(svc);
        this.helper = new WitRevisionsHelper(svc);
    }
    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String,Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String,Object>) base.get("properties");
        props.put("id", Map.of("type","integer","description","ID del work item"));
        props.put("rev", Map.of("type","integer","description","Número de la revisión"));
        props.put("expand", Map.of("type","string","description","none|relations|fields|links|all"));
    @SuppressWarnings("unchecked") List<String> originalReq = (List<String>) base.get("required");
    List<String> req = new ArrayList<>(originalReq);
    if (!req.contains("id")) req.add("id");
    if (!req.contains("rev")) req.add("rev");
    base.put("required", req);
        return base;
    }

    @Override
    protected Map<String,Object> executeInternal(Map<String,Object> args) {
        String project = getProject(args);
        Object idObj = args.get("id");
        Object revObj = args.get("rev");
        try {
            helper.validateId(idObj);
            helper.validateRev(revObj);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
        Map<String,String> query = helper.buildGetQuery(args.get("expand"));
        Map<String,Object> resp = helper.fetchRevision(project, idObj, revObj, query);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        return success(helper.formatRevisionResponse(resp));
    }
}
