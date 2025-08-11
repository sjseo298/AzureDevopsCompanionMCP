package com.mcp.server.tools.azuredevops.wit;


import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitRevisionsHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_revisions_list
 * Lista revisiones de un work item específico.
 * Endpoint: GET /{project}/_apis/wit/workItems/{id}/revisions?api-version=7.2-preview.3
 */
@Component
public class RevisionsListTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_revisions_list";
    private static final String DESC = "Lista revisiones de un work item (hasta 5 mostradas).";
    private static final String API_VERSION = "7.2-preview.3";

    private final WitRevisionsHelper helper;

    public RevisionsListTool(AzureDevOpsClientService svc) {
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
        props.put("expand", Map.of("type","string","description","none|relations|fields|links|all"));
        props.put("skip", Map.of("type","integer","description","$skip"));
        props.put("top", Map.of("type","integer","description","$top"));
    @SuppressWarnings("unchecked") List<String> originalReq = (List<String>) base.get("required");
    List<String> req = new ArrayList<>(originalReq);
    if (!req.contains("id")) req.add("id");
    base.put("required", req);
        return base;
    }

    @Override
    protected Map<String,Object> executeInternal(Map<String,Object> args) {
        String project = getProject(args);
        Object idObj = args.get("id");
        try {
            helper.validateId(idObj);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
        Map<String,String> query = helper.buildListQuery(args.get("expand"), args.get("skip"), args.get("top"));
        Map<String,Object> resp = helper.fetchRevisionsList(project, idObj, query);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        return success(helper.formatRevisionsListResponse(resp));
    }
}
