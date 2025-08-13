package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Helper para listar categorías de tipos de work item en un proyecto.
 * Endpoint: GET /{project}/_apis/wit/workitemtypecategories?api-version={ver}
 */
@Component
public class WitWorkItemTypeCategoriesListHelper {
    private final AzureDevOpsClientService client;
    public WitWorkItemTypeCategoriesListHelper(AzureDevOpsClientService client) { this.client = client; }

    public Map<String,Object> listCategories(String project, String apiVersion) {
        java.util.Map<String,String> q = new java.util.LinkedHashMap<>();
        if (apiVersion != null && !apiVersion.isBlank()) q.put("api-version", apiVersion);
        return client.getWitApiWithQuery(project, null, "workitemtypecategories", q, apiVersion);
    }
}
