package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Helper para obtener nodos de clasificación por IDs (query param ids=...).
 */
public class WitClassificationNodesGetByIdsHelper {
    private final AzureDevOpsClientService azureService;

    public WitClassificationNodesGetByIdsHelper(AzureDevOpsClientService azureService) {
        this.azureService = azureService;
    }

    public void validate(String project, String ids) {
        if (project == null || project.isBlank()) throw new IllegalArgumentException("Parámetro 'project' es obligatorio.");
        if (ids == null || ids.isBlank()) throw new IllegalArgumentException("Parámetro 'ids' es obligatorio (lista separada por coma)");
    }

    public Map<String,Object> fetchNodes(String project, String team, String ids, String apiVersion) {
        Map<String,String> query = new LinkedHashMap<>();
        query.put("ids", ids);
        return azureService.getWitApiWithQuery(project, team, "classificationnodes", query, apiVersion);
    }

    public String formatNodesResponse(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Respuesta vacía)";
        if (data.containsKey("value") && data.get("value") instanceof List) {
            List<?> list = (List<?>) data.get("value");
            StringBuilder sb = new StringBuilder("=== Classification Nodes (by ids) ===\n\n");
            int i = 1;
            for (Object o : list) {
                if (o instanceof Map) {
                    Map<?,?> m = (Map<?,?>) o;
                    sb.append(i++).append(". ");
                    Object id = m.get("id");
                    Object name = m.get("name");
                    Object type = m.get("structureType");
                    if (id != null) sb.append("[").append(id).append("] ");
                    sb.append(name != null ? name : "(sin nombre)");
                    if (type != null) sb.append(" [").append(type).append("]");
                    sb.append('\n');
                }
            }
            return sb.toString();
        }
        return data.toString();
    }
}
