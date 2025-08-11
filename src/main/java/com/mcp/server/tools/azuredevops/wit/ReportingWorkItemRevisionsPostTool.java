package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitReportingHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_reporting_workitemrevisions_post
 * Obtiene revisiones (reporting) vía POST con filtros en body.
 * Endpoint: POST /{project}/_apis/wit/reporting/workitemrevisions?api-version=7.2-preview.2
 */
public class ReportingWorkItemRevisionsPostTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_reporting_workitemrevisions_post";
    private static final String DESC = "Obtiene revisiones (POST) enviando filtros extensos en el cuerpo.";
    private static final String API_VERSION = "7.2-preview.2";

    private final WitReportingHelper helper;

    public ReportingWorkItemRevisionsPostTool(AzureDevOpsClientService svc) {
        super(svc);
        this.helper = new WitReportingHelper(svc);
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String,Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String,Object>) base.get("properties");
        props.put("fields", Map.of("type","string","description","Campos separados por comas"));
        props.put("types", Map.of("type","string","description","Tipos separados por comas"));
        props.put("includeDeleted", Map.of("type","boolean"));
        props.put("includeIdentityRef", Map.of("type","boolean"));
        props.put("includeLatestOnly", Map.of("type","boolean"));
        props.put("includeTagRef", Map.of("type","boolean"));
        return base;
    }

    @Override
    protected Map<String,Object> executeInternal(Map<String,Object> args) {
        String project = getProject(args);
        Map<String,Object> body = helper.buildRevisionsPostBody(args);
        Map<String,Object> resp = helper.fetchWorkItemRevisionsPost(project, body);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        return success(helper.formatWorkItemRevisionsPostResponse(resp));
    }
}
