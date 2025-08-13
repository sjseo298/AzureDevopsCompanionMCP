package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class WitWorkItemTypeStatesListHelper {
    private final AzureDevOpsClientService client;
    public WitWorkItemTypeStatesListHelper(AzureDevOpsClientService client) { this.client = client; }

    /**
     * Lista los estados definidos para un tipo de work item en un proyecto.
     * Endpoint: GET /{project}/_apis/wit/workitemtypes/{type}/states?api-version={ver}
     */
    public Map<String,Object> listStates(String project, String type, String apiVersion) {
        // Usamos getWitApiWithQuery para que api-version sea query real y evitar '?' en segmentos
        java.util.Map<String,String> q = new java.util.LinkedHashMap<>();
        if (apiVersion != null && !apiVersion.isBlank()) q.put("api-version", apiVersion);
        return client.getWitApiWithQuery(project, null, "workitemtypes/" + type + "/states", q, apiVersion);
    }
}
