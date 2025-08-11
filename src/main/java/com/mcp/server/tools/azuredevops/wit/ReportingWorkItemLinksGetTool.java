package com.mcp.server.tools.azuredevops.wit;


import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitReportingHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_reporting_workitemlinks_get
 * Obtiene enlaces (work item links) para reporting con filtros y paginación.
 * Endpoint: GET /{project}/_apis/wit/reporting/workitemlinks?api-version=7.2-preview.3
 */
@Component
public class ReportingWorkItemLinksGetTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_reporting_workitemlinks_get";
    private static final String DESC = "Obtiene vínculos entre work items (reporting) con filtros y continuationToken.";
    private static final String API_VERSION = "7.2-preview.3";

    private final WitReportingHelper helper;

    public ReportingWorkItemLinksGetTool(AzureDevOpsClientService svc) {
        super(svc);
        this.helper = new WitReportingHelper(svc);
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String,Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String,Object>) base.get("properties");
        props.put("linkTypes", Map.of("type","string","description","Lista separada por comas de tipos de vínculo"));
        props.put("types", Map.of("type","string","description","Lista separada por comas de tipos de work item"));
        props.put("startDateTime", Map.of("type","string","description","ISO8601 startDateTime"));
        props.put("continuationToken", Map.of("type","string","description","Token para siguiente lote"));
        return base; // project requerido según convención interna
    }

    @Override
    protected Map<String,Object> executeInternal(Map<String,Object> args) {
        String project = getProject(args);
        Map<String,String> query = helper.buildLinksQuery(args.get("linkTypes"), args.get("types"), args.get("startDateTime"), args.get("continuationToken"));
        Map<String,Object> resp = helper.fetchWorkItemLinks(project, query);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        return success(helper.formatWorkItemLinksResponse(resp));
    }
}
