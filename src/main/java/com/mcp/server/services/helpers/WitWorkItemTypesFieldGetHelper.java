package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import java.util.Map;
import java.util.HashMap;

public class WitWorkItemTypesFieldGetHelper {
    private final AzureDevOpsClientService client;
    private final WitFieldsGlobalGetHelper fieldsGlobalHelper;
    private final WorkitemtrackingprocessPicklistsHelper picklistsHelper;

    public WitWorkItemTypesFieldGetHelper(AzureDevOpsClientService client) {
        this.client = client;
        this.fieldsGlobalHelper = new WitFieldsGlobalGetHelper(client);
        this.picklistsHelper = new WorkitemtrackingprocessPicklistsHelper(client);
    }

    public Map<String, Object> getField(String project, String type, String field, String apiVersion, boolean enrich, boolean summary, boolean showPicklistItems, boolean raw) {
        Map<String, Object> result = new HashMap<>();
        String path = String.format("_apis/wit/workitemtypes/%s/fields/%s?api-version=%s", type, field, apiVersion);
        Map<String, Object> scoped = client.getWorkApi(project, null, path);
        if (raw) return scoped;
        if (!enrich) {
            if (summary) {
                result.put("ref", scoped.get("referenceName"));
                result.put("name", scoped.get("name"));
                result.put("required", scoped.get("alwaysRequired"));
                result.put("projectType", scoped.get("type"));
                return result;
            } else {
                return scoped;
            }
        }
        Map<String, Object> global = fieldsGlobalHelper.getFieldGlobal(field, true);
        Object picklistId = global.get("picklistId");
        Map<String, Object> picklist = null;
        if (picklistId != null && picklistId instanceof String && !((String)picklistId).isEmpty()) {
            picklist = picklistsHelper.getPicklist((String)picklistId, true);
        }
        // Merge scoped + global + picklist
        result.putAll(scoped);
        result.put("globalType", global.get("type"));
        result.put("globalUsage", global.get("usage"));
        result.put("globalReadOnly", global.get("readOnly"));
        result.put("picklistId", global.get("picklistId"));
        if (picklist != null && picklist.containsKey("items")) {
            result.put("picklistName", picklist.get("name"));
            result.put("picklistType", picklist.get("type"));
            result.put("picklistItems", picklist.get("items"));
        }
        if (summary) {
            result = buildSummary(result, showPicklistItems);
        }
        return result;
    }

    private Map<String, Object> buildSummary(Map<String, Object> merged, boolean showPicklistItems) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("ref", merged.get("referenceName"));
        summary.put("name", merged.get("name"));
        summary.put("required", merged.get("alwaysRequired"));
        summary.put("projectType", merged.get("type"));
        summary.put("globalType", merged.get("globalType"));
        summary.put("globalUsage", merged.get("globalUsage"));
        summary.put("globalReadOnly", merged.get("globalReadOnly"));
        boolean hasPicklist = merged.get("picklistItems") instanceof java.util.List;
        summary.put("hasPicklist", hasPicklist);
        summary.put("picklistCount", hasPicklist ? ((java.util.List<?>)merged.get("picklistItems")).size() : 0);
        if (showPicklistItems && hasPicklist) {
            summary.put("picklistItems", merged.get("picklistItems"));
        }
        return summary;
    }
}
