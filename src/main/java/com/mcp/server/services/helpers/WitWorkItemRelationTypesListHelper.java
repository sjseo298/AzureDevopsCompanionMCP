package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class WitWorkItemRelationTypesListHelper {
    private final AzureDevOpsClientService client;
    public WitWorkItemRelationTypesListHelper(AzureDevOpsClientService client) {
        this.client = client;
    }
    /**
     * Lista los tipos de relación entre work items a nivel organización
     */
    public Map<String,Object> listRelationTypes(String apiVersion) {
        String path = "_apis/wit/workitemrelationtypes?api-version=" + apiVersion;
        // No requiere project ni team
        return client.getWorkApi("", null, "wit/workitemrelationtypes?api-version=" + apiVersion);
    }
}
