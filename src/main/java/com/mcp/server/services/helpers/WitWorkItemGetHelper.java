package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import java.util.HashMap;
import java.util.Map;

public class WitWorkItemGetHelper {
    private final AzureDevOpsClientService client;
    public WitWorkItemGetHelper(AzureDevOpsClientService client) {
        this.client = client;
    }
    /**
     * Obtiene un work item por ID y proyecto
     */
    public Map<String,Object> getWorkItem(String project, int id, String fields, String apiVersion) {
        // Usar área WIT directamente para permitir project vacío: /_apis/wit/workitems/{id}
        Map<String,String> query = new HashMap<>();
        if (fields != null && !fields.isBlank()) query.put("fields", fields);
        if (apiVersion != null && !apiVersion.isBlank()) query.put("api-version", apiVersion);
        Map<String,Object> resp = client.getWitApiWithQuery((project == null || project.isBlank()) ? null : project, null, "workitems/" + id, query, apiVersion);
        // Normalizar: si vino error HTTP, retornará isHttpError, lo dejamos para manejo superior.
        return resp;
    }
}
