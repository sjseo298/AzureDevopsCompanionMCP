package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class WitWorkItemTypesGetHelper {
    private final AzureDevOpsClientService client;
    public WitWorkItemTypesGetHelper(AzureDevOpsClientService client) {
        this.client = client;
    }
    /**
     * Obtiene el detalle de un tipo de work item en un proyecto
     */
    public Map<String,Object> getType(String project, String type, String apiVersion) {
        String path = String.format("_apis/wit/workitemtypes/%s?api-version=%s", type, apiVersion);
        return client.getWorkApi(project, null, "wit/workitemtypes/" + type + "?api-version=" + apiVersion);
    }
}
