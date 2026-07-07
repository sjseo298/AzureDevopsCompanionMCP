package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Helper para listar tipos de work item disponibles en un proyecto.
 * Endpoint: GET /{project}/_apis/wit/workitemtypes?api-version=7.2-preview
 */
@Component
public class WitWorkItemTypesListHelper {
    private final AzureDevOpsClientService client;

    public WitWorkItemTypesListHelper(AzureDevOpsClientService client) {
        this.client = client;
    }

    /**
     * Lista todos los tipos de work item disponibles en un proyecto.
     */
    public Map<String, Object> listTypes(String project, String apiVersion) {
        Map<String, String> q = new LinkedHashMap<>();
        if (apiVersion != null && !apiVersion.isBlank()) {
            q.put("api-version", apiVersion);
        }
        return client.getWitApiWithQuery(project, null, "workitemtypes", q, apiVersion);
    }

    /**
     * Extrae solo los nombres de los tipos de work item de la respuesta de la API.
     */
    public List<String> getTypeNames(Map<String, Object> response) {
        List<String> names = new ArrayList<>();
        if (response == null || response.isEmpty()) return names;
        Object value = response.get("value");
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                if (item instanceof Map) {
                    Object name = ((Map<?, ?>) item).get("name");
                    if (name != null) names.add(name.toString());
                }
            }
        }
        return names;
    }
}
