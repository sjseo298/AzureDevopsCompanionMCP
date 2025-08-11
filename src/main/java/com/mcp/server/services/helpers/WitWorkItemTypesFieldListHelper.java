package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class WitWorkItemTypesFieldListHelper {
    private final AzureDevOpsClientService client;
    public WitWorkItemTypesFieldListHelper(AzureDevOpsClientService client) {
        this.client = client;
    }
    /**
     * Lista los campos de un tipo de work item en un proyecto
     */
    public Map<String,Object> listFields(String project, String type, boolean requiredOnly, boolean summary, boolean showPicklistItems, String apiVersion) {
        String path = String.format("_apis/wit/workitemtypes/%s/fields?api-version=%s", type, apiVersion);
        Map<String,Object> resp = client.getWorkApi(project, null, path);
        // El enriquecimiento y filtrado se delega a la tool
        return resp;
    }
}
