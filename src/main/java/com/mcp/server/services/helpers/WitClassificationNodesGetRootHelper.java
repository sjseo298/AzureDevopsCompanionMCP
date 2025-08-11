package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import java.util.List;
import java.util.Map;

/**
 * Helper para obtener los nodos raíz de clasificación (areas e iterations) a nivel proyecto.
 */
public class WitClassificationNodesGetRootHelper {
    private final AzureDevOpsClientService azureService;

    public WitClassificationNodesGetRootHelper(AzureDevOpsClientService azureService) {
        this.azureService = azureService;
    }

    public void validate(String project) {
        if (project == null || project.isBlank()) throw new IllegalArgumentException("Parámetro 'project' es obligatorio.");
    }

    public Map<String,Object> fetchRootNodes(String project, String team, String apiVersion) {
        return azureService.getWitApiWithQuery(project, team, "classificationnodes", null, apiVersion);
    }

    public String formatRootNodesResponse(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Respuesta vacía)";
        if (data.containsKey("value") && data.get("value") instanceof List) {
            List<?> list = (List<?>) data.get("value");
            StringBuilder sb = new StringBuilder("=== Root Classification Nodes ===\n\n");
            int i = 1;
            for (Object o : list) {
                if (o instanceof Map) {
                    Map<?,?> m = (Map<?,?>) o;
                    sb.append(i++).append(". ");
                    Object name = m.get("name");
                    Object type = m.get("structureType");
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
