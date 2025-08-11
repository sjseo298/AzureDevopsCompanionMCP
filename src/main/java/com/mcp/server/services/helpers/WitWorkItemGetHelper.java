package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class WitWorkItemGetHelper {
    private final AzureDevOpsClientService client;
    public WitWorkItemGetHelper(AzureDevOpsClientService client) {
        this.client = client;
    }
    /**
     * Obtiene un work item por ID y proyecto
     */
    public Map<String,Object> getWorkItem(String project, int id, String fields, String apiVersion) {
        String path = String.format("_apis/wit/workitems/%d?api-version=%s", id, apiVersion);
        if (fields != null && !fields.isEmpty()) {
            path += "&fields=" + fields;
        }
        return client.getWorkApi(project, null, "wit/workitems/" + id + "?api-version=" + apiVersion + (fields != null && !fields.isEmpty() ? "&fields=" + fields : ""));
    }
}
