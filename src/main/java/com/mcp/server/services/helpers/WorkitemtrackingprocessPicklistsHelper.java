package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import java.util.*;

/**
 * Helper para obtener picklists (listas de selección) a nivel organización en Azure DevOps.
 */
public class WorkitemtrackingprocessPicklistsHelper {
    private final AzureDevOpsClientService azureService;
    private static final String API_VERSION = "7.2-preview.1";

    public WorkitemtrackingprocessPicklistsHelper(AzureDevOpsClientService svc) {
        this.azureService = svc;
    }

    public void validateId(Object id) {
        if (id == null || id.toString().trim().isEmpty()) throw new IllegalArgumentException("'id' es requerido");
    }

    public Map<String,Object> fetchPicklist(Object id) {
        String path = "work/processes/lists/" + id.toString().trim();
        Map<String,String> q = new LinkedHashMap<>();
        q.put("api-version", API_VERSION);
        return azureService.getCoreApi(path, q);
    }

    /**
     * Devuelve el JSON crudo del picklist
     */
    public Map<String,Object> getPicklist(String id, boolean raw) {
        return fetchPicklist(id);
    }

    public String formatPicklistResponse(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(sin datos)";
        StringBuilder sb = new StringBuilder();
        sb.append("Picklist: ").append(data.getOrDefault("name","(sin nombre)"));
        Object id = data.get("id"); if (id != null) sb.append(" [").append(id).append("]");
        Object type = data.get("type"); if (type != null) sb.append(" | type=").append(type);
        @SuppressWarnings("unchecked") List<Object> items = (List<Object>) data.get("items");
        if (items != null) {
            sb.append("\nItems (" ).append(items.size()).append("):");
            int i=1; for (Object it : items) {
                sb.append("\n  ").append(i++).append(") ").append(it);
            }
        }
        return sb.toString();
    }
}
