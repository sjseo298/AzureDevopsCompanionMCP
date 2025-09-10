package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Helper para revisiones de work items en Azure DevOps.
 */
@Component
public class WitRevisionsHelper {
    private final AzureDevOpsClientService azureService;

    public WitRevisionsHelper(AzureDevOpsClientService svc) {
        this.azureService = svc;
    }

    public void validateId(Object id) {
        if (id == null || !id.toString().matches("\\d+")) throw new IllegalArgumentException("'id' es requerido y numérico");
    }
    public void validateRev(Object rev) {
        if (rev == null || !rev.toString().matches("\\d+")) throw new IllegalArgumentException("'rev' es requerido y numérico");
    }

    public Map<String,String> buildGetQuery(Object expand) {
        Map<String,String> query = new LinkedHashMap<>();
        putIf(query, "$expand", expand);
        return query;
    }
    public Map<String,String> buildListQuery(Object expand, Object skip, Object top) {
        Map<String,String> query = new LinkedHashMap<>();
        putIf(query, "$expand", expand);
        putIf(query, "$skip", skip);
        putIf(query, "$top", top);
        return query;
    }

    public Map<String,Object> fetchRevision(String project, Object id, Object rev, Map<String,String> query) {
        return azureService.getWitApiWithQuery(project, null, "workItems/"+id+"/revisions/"+rev, query.isEmpty()? null : query, "7.2-preview.3");
    }
    public Map<String,Object> fetchRevisionsList(String project, Object id, Map<String,String> query) {
        return azureService.getWitApiWithQuery(project, null, "workItems/"+id+"/revisions", query.isEmpty()? null : query, "7.2-preview.3");
    }

    public String formatRevisionResponse(Map<String,Object> resp) {
        @SuppressWarnings("unchecked") Map<String,Object> fields = (Map<String,Object>) resp.get("fields");
        Object title = fields != null ? fields.get("System.Title") : null;
        Object state = fields != null ? fields.get("System.State") : null;
        Object type = fields != null ? fields.get("System.WorkItemType") : null;
        StringBuilder sb = new StringBuilder();
        sb.append("Revision rev=").append(resp.get("rev")).append(" id=").append(resp.get("id"));
        if (title != null) sb.append(" title=").append(title);
        if (state != null) sb.append(" state=").append(state);
        if (type != null) sb.append(" type=").append(type);
        return sb.toString();
    }

    public String formatRevisionsListResponse(Map<String,Object> resp) {
        Object count = resp.get("count");
        @SuppressWarnings("unchecked") List<Map<String,Object>> value = (List<Map<String,Object>>) resp.get("value");
        StringBuilder sb = new StringBuilder();
        sb.append("Revisiones count=").append(count).append('\n');
        if (value != null && !value.isEmpty()) {
            // Aumentado de 5 a 50 elementos para mostrar más revisiones
            int max = Math.min(50, value.size());
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
        return sb.toString();
    }

    private void putIf(Map<String,String> q, String key, Object val) { if (val != null) { String s = val.toString().trim(); if (!s.isEmpty()) q.put(key, s); } }
}
