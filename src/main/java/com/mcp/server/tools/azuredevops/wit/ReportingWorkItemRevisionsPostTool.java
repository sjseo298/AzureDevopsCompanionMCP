package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
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

    public ReportingWorkItemRevisionsPostTool(AzureDevOpsClientService svc) { super(svc); }

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
        Map<String,Object> body = new LinkedHashMap<>();
        putList(body, "fields", args.get("fields"));
        putList(body, "types", args.get("types"));
        putBool(body, "includeDeleted", args.get("includeDeleted"));
        putBool(body, "includeIdentityRef", args.get("includeIdentityRef"));
        putBool(body, "includeLatestOnly", args.get("includeLatestOnly"));
        putBool(body, "includeTagRef", args.get("includeTagRef"));

        Map<String,Object> resp = azureService.postWitApi(project,null,"reporting/workitemrevisions", body.isEmpty()? Map.of(): body, API_VERSION);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);

        @SuppressWarnings("unchecked") List<Map<String,Object>> values = (List<Map<String,Object>>) resp.get("values");
        Object continuation = resp.get("continuationToken");
        Object isLast = resp.get("isLastBatch");
        int total = values != null ? values.size() : 0;
        StringBuilder sb = new StringBuilder();
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

    private void putList(Map<String,Object> body, String key, Object val) {
        if (val == null) return; String s = val.toString().trim(); if (s.isEmpty()) return;
        List<String> list = new ArrayList<>();
        for (String part : s.split(",")) { String p = part.trim(); if (!p.isEmpty()) list.add(p); }
        if (!list.isEmpty()) body.put(key, list);
    }
    private void putBool(Map<String,Object> body, String key, Object val) { if (val instanceof Boolean b && b) body.put(key, true); }
}
