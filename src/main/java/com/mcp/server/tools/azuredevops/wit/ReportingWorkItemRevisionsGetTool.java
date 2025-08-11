package com.mcp.server.tools.azuredevops.wit;


import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitReportingHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_reporting_workitemrevisions_get
 * Obtiene revisiones de work items (reporting) vía GET con filtros y paginación.
 * Endpoint: GET /{project}/_apis/wit/reporting/workitemrevisions?api-version=7.2-preview.2
 */
@Component
public class ReportingWorkItemRevisionsGetTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_reporting_workitemrevisions_get";
    private static final String DESC = "Obtiene revisiones (GET) con filtros/paginación (reporting).";
    private static final String API_VERSION = "7.2-preview.2";

    private final WitReportingHelper helper;

    public ReportingWorkItemRevisionsGetTool(AzureDevOpsClientService svc) {
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
        props.put("startDateTime", Map.of("type","string","description","ISO8601 startDateTime"));
        props.put("continuationToken", Map.of("type","string","description","Token de continuación"));
        props.put("expand", Map.of("type","string","description","none|fields"));
        props.put("maxPageSize", Map.of("type","integer","description","$maxPageSize"));
        props.put("includeDeleted", Map.of("type","boolean"));
        props.put("includeIdentityRef", Map.of("type","boolean"));
        props.put("includeLatestOnly", Map.of("type","boolean"));
        props.put("includeTagRef", Map.of("type","boolean"));
        props.put("includeDiscussionChangesOnly", Map.of("type","boolean"));
        return base;
    }

    @Override
    protected Map<String,Object> executeInternal(Map<String,Object> args) {
        String project = getProject(args);
        Map<String,String> query = helper.buildRevisionsGetQuery(args);
        Map<String,Object> resp = helper.fetchWorkItemRevisionsGet(project, query);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        return success(helper.formatWorkItemRevisionsGetResponse(resp));
    }
}
