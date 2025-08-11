package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Helper para reporting de work item links y revisiones en Azure DevOps.
 */
@Component
public class WitReportingHelper {
    private final AzureDevOpsClientService azureService;

    public WitReportingHelper(AzureDevOpsClientService svc) {
        this.azureService = svc;
    }

    // --- WorkItemLinks GET ---
    public Map<String,String> buildLinksQuery(Object linkTypes, Object types, Object startDateTime, Object continuationToken) {
        Map<String,String> query = new LinkedHashMap<>();
        if (notEmpty(linkTypes)) query.put("linkTypes", linkTypes.toString().trim());
        if (notEmpty(types)) query.put("types", types.toString().trim());
        if (notEmpty(startDateTime)) query.put("startDateTime", startDateTime.toString().trim());
        if (notEmpty(continuationToken)) query.put("continuationToken", continuationToken.toString().trim());
        return query;
    }

    public Map<String,Object> fetchWorkItemLinks(String project, Map<String,String> query) {
        return azureService.getWitApiWithQuery(project, null, "reporting/workitemlinks", query.isEmpty()? null : query, "7.2-preview.3");
    }

    public String formatWorkItemLinksResponse(Map<String,Object> resp) {
        @SuppressWarnings("unchecked") List<Map<String,Object>> values = (List<Map<String,Object>>) resp.get("values");
        Object isLast = resp.get("isLastBatch");
        Object nextLink = resp.get("nextLink");
        StringBuilder sb = new StringBuilder();
        if (values == null) {
            sb.append("Sin datos.");
        } else {
            sb.append("Links recibidos: ").append(values.size()).append('\n');
            int max = Math.min(values.size(), 10);
            for (int i=0;i<max;i++) {
                Map<String,Object> v = values.get(i);
                sb.append(i+1).append(") ")
                  .append(v.get("rel"))
                  .append(" ")
                  .append(v.get("sourceId"))
                  .append(" -> ")
                  .append(v.get("targetId"));
                Object changed = v.get("changedDate");
                if (changed != null) sb.append(" | ").append(changed);
                sb.append('\n');
            }
            if (values.size() > max) sb.append("... ("+(values.size()-max)+" más)\n");
        }
        if (isLast != null) sb.append("isLastBatch=").append(isLast).append('\n');
        if (nextLink != null) sb.append("Tiene siguiente lote (usar continuationToken extraído del nextLink).\n");
        return sb.toString();
    }

    // --- WorkItemRevisions GET ---
    public Map<String,String> buildRevisionsGetQuery(Map<String,Object> args) {
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
        return query;
    }

    public Map<String,Object> fetchWorkItemRevisionsGet(String project, Map<String,String> query) {
        return azureService.getWitApiWithQuery(project, null, "reporting/workitemrevisions", query.isEmpty()? null : query, "7.2-preview.2");
    }

    public String formatWorkItemRevisionsGetResponse(Map<String,Object> resp) {
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
        return sb.toString();
    }

    // --- WorkItemRevisions POST ---
    public Map<String,Object> buildRevisionsPostBody(Map<String,Object> args) {
        Map<String,Object> body = new LinkedHashMap<>();
        putList(body, "fields", args.get("fields"));
        putList(body, "types", args.get("types"));
        putBool(body, "includeDeleted", args.get("includeDeleted"));
        putBool(body, "includeIdentityRef", args.get("includeIdentityRef"));
        putBool(body, "includeLatestOnly", args.get("includeLatestOnly"));
        putBool(body, "includeTagRef", args.get("includeTagRef"));
        return body;
    }

    public Map<String,Object> fetchWorkItemRevisionsPost(String project, Map<String,Object> body) {
        return azureService.postWitApi(project, null, "reporting/workitemrevisions", body.isEmpty()? Map.of(): body, "7.2-preview.2");
    }

    public String formatWorkItemRevisionsPostResponse(Map<String,Object> resp) {
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
        return sb.toString();
    }

    // --- Utilidades ---
    private boolean notEmpty(Object o) { return o != null && !o.toString().trim().isEmpty(); }
    private void putIf(Map<String,String> q, String key, Object val) { if (val != null) { String s = val.toString().trim(); if (!s.isEmpty()) q.put(key, s); } }
    private void bool(Map<String,String> q, String key, Object val) { if (val instanceof Boolean b && b) q.put(key, "true"); }
    private void putList(Map<String,Object> body, String key, Object val) {
        if (val == null) return; String s = val.toString().trim(); if (s.isEmpty()) return;
        List<String> list = new ArrayList<>();
        for (String part : s.split(",")) { String p = part.trim(); if (!p.isEmpty()) list.add(p); }
        if (!list.isEmpty()) body.put(key, list);
    }
    private void putBool(Map<String,Object> body, String key, Object val) { if (val instanceof Boolean b && b) body.put(key, true); }
}
