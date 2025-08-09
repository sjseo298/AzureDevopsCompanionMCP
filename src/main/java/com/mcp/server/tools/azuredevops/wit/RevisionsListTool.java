package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_revisions_list
 * Lista revisiones de un work item específico.
 * Endpoint: GET /{project}/_apis/wit/workItems/{id}/revisions?api-version=7.2-preview.3
 */
public class RevisionsListTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_revisions_list";
    private static final String DESC = "Lista revisiones de un work item (hasta 5 mostradas).";
    private static final String API_VERSION = "7.2-preview.3";

    public RevisionsListTool(AzureDevOpsClientService svc) { super(svc); }
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
        @SuppressWarnings("unchecked") List<String> req = (List<String>) base.get("required");
        if (!req.contains("id")) req.add("id");
        return base;
    }

    @Override
    protected Map<String,Object> executeInternal(Map<String,Object> args) {
        String project = getProject(args);
        Object idObj = args.get("id");
        if (idObj == null || !idObj.toString().matches("\\d+")) return error("'id' es requerido y numérico");
        String id = idObj.toString();
        Map<String,String> query = new LinkedHashMap<>();
        putIf(query, "$expand", args.get("expand"));
        putIf(query, "$skip", args.get("skip"));
        putIf(query, "$top", args.get("top"));
        Map<String,Object> resp = azureService.getWitApiWithQuery(project,null,"workItems/"+id+"/revisions", query.isEmpty()? null : query, API_VERSION);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        Object count = resp.get("count");
        @SuppressWarnings("unchecked") List<Map<String,Object>> value = (List<Map<String,Object>>) resp.get("value");
        StringBuilder sb = new StringBuilder();
        sb.append("Revisiones count=").append(count).append('\n');
        if (value != null && !value.isEmpty()) {
            int max = Math.min(5, value.size());
            for (int i=0;i<max;i++) {
                Map<String,Object> v = value.get(i);
                Object rev = v.get("rev");
                @SuppressWarnings("unchecked") Map<String,Object> fields = (Map<String,Object>) v.get("fields");
                Object title = fields != null ? fields.get("System.Title") : null;
                Object state = fields != null ? fields.get("System.State") : null;
                sb.append(i+1).append(") rev=").append(rev).append(" title=").append(title).append(" state=").append(state).append('\n');
            }
            if (value.size()>max) sb.append("... ("+(value.size()-max)+" más)\n");
        }
        return success(sb.toString());
    }

    private void putIf(Map<String,String> q, String key, Object val) { if (val != null) { String s = val.toString().trim(); if (!s.isEmpty()) q.put(key, s); } }
}
