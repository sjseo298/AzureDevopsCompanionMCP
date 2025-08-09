package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_revisions_get
 * Obtiene una revisión específica de un work item.
 * Endpoint: GET /{project}/_apis/wit/workItems/{id}/revisions/{rev}?api-version=7.2-preview.3
 */
public class RevisionsGetTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_revisions_get";
    private static final String DESC = "Obtiene una revisión específica de un work item.";
    private static final String API_VERSION = "7.2-preview.3";

    public RevisionsGetTool(AzureDevOpsClientService svc) { super(svc); }
    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String,Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String,Object>) base.get("properties");
        props.put("id", Map.of("type","integer","description","ID del work item"));
        props.put("rev", Map.of("type","integer","description","Número de la revisión"));
        props.put("expand", Map.of("type","string","description","none|relations|fields|links|all"));
        @SuppressWarnings("unchecked") List<String> req = (List<String>) base.get("required");
        if (!req.contains("id")) req.add("id");
        if (!req.contains("rev")) req.add("rev");
        return base;
    }

    @Override
    protected Map<String,Object> executeInternal(Map<String,Object> args) {
        String project = getProject(args);
        Object idObj = args.get("id");
        Object revObj = args.get("rev");
        if (idObj == null || !idObj.toString().matches("\\d+")) return error("'id' es requerido y numérico");
        if (revObj == null || !revObj.toString().matches("\\d+")) return error("'rev' es requerido y numérico");
        String id = idObj.toString();
        String rev = revObj.toString();
        Map<String,String> query = new LinkedHashMap<>();
        putIf(query, "$expand", args.get("expand"));
        Map<String,Object> resp = azureService.getWitApiWithQuery(project,null,"workItems/"+id+"/revisions/"+rev, query.isEmpty()? null : query, API_VERSION);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        @SuppressWarnings("unchecked") Map<String,Object> fields = (Map<String,Object>) resp.get("fields");
        Object title = fields != null ? fields.get("System.Title") : null;
        Object state = fields != null ? fields.get("System.State") : null;
        Object type = fields != null ? fields.get("System.WorkItemType") : null;
        StringBuilder sb = new StringBuilder();
        sb.append("Revision rev=").append(resp.get("rev")).append(" id=").append(resp.get("id"));
        if (title != null) sb.append(" title=").append(title);
        if (state != null) sb.append(" state=").append(state);
        if (type != null) sb.append(" type=").append(type);
        return success(sb.toString());
    }

    private void putIf(Map<String,String> q, String key, Object val) { if (val != null) { String s = val.toString().trim(); if (!s.isEmpty()) q.put(key, s); } }
}
