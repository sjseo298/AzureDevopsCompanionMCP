package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_reporting_workitemrevisions_get
 * Obtiene revisiones de work items (reporting) vía GET con filtros y paginación.
 * Endpoint: GET /{project}/_apis/wit/reporting/workitemrevisions?api-version=7.2-preview.2
 */
public class ReportingWorkItemRevisionsGetTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_reporting_workitemrevisions_get";
    private static final String DESC = "Obtiene revisiones (GET) con filtros/paginación (reporting).";
    private static final String API_VERSION = "7.2-preview.2";

    public ReportingWorkItemRevisionsGetTool(AzureDevOpsClientService svc) { super(svc); }

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
        Map<String,String> query = new LinkedHashMap<>();
        putIf(query, "fields", args.get("fields"));
        putIf(query, "types", args.get("types"));
        putIf(query, "startDateTime", args.get("startDateTime"));
        putIf(query, "continuationToken", args.get("continuationToken"));
        putIf(query, "$expand", args.get("expand"));
        putIf(query, "$maxPageSize", args.get("maxPageSize"));
        bool(query, "includeDeleted", args.get("includeDeleted"));
        bool(query, "includeIdentityRef", args.get("includeIdentityRef"));
        bool(query, "includeLatestOnly", args.get("includeLatestOnly"));
        bool(query, "includeTagRef", args.get("includeTagRef"));
        bool(query, "includeDiscussionChangesOnly", args.get("includeDiscussionChangesOnly"));

        Map<String,Object> resp = azureService.getWitApiWithQuery(project,null,"reporting/workitemrevisions", query.isEmpty()? null : query, API_VERSION);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);

        @SuppressWarnings("unchecked") List<Map<String,Object>> values = (List<Map<String,Object>>) resp.get("values");
        Object continuation = resp.get("continuationToken");
        Object isLast = resp.get("isLastBatch");
        StringBuilder sb = new StringBuilder();
        int total = values != null ? values.size() : 0;
        sb.append("Revisiones recibidas: ").append(total).append('\n');
        if (values != null && !values.isEmpty()) {
            int max = Math.min(5, values.size());
            for (int i=0;i<max;i++) {
                Map<String,Object> v = values.get(i);
                sb.append(i+1).append(") id=").append(v.get("id")).append(" rev=").append(v.get("rev")).append('\n');
            }
            if (values.size()>max) sb.append("... ("+(values.size()-max)+" más)\n");
        }
        if (continuation != null) sb.append("continuationToken=").append(continuation).append('\n');
        if (isLast != null) sb.append("isLastBatch=").append(isLast).append('\n');
        return success(sb.toString());
    }

    private void putIf(Map<String,String> q, String key, Object val) { if (val != null) { String s = val.toString().trim(); if (!s.isEmpty()) q.put(key, s); } }
    private void bool(Map<String,String> q, String key, Object val) { if (val instanceof Boolean b && b) q.put(key, "true"); }
}
