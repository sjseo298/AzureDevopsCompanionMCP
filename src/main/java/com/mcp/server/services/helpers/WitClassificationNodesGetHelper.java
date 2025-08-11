package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Helper para obtener nodos de clasificación (areas/iterations) a nivel proyecto.
 */
@Component
public class WitClassificationNodesGetHelper {
    private final AzureDevOpsClientService azureService;

    public WitClassificationNodesGetHelper(AzureDevOpsClientService azureService) {
        this.azureService = azureService;
    }

    public void validate(String project, String group) {
        if (project == null || project.isBlank()) throw new IllegalArgumentException("Parámetro 'project' es obligatorio.");
        if (group == null || (!"areas".equals(group) && !"iterations".equals(group))) throw new IllegalArgumentException("Parámetro 'group' inválido. Valores permitidos: areas, iterations.");
    }

    public String buildEndpoint(String group, String path) {
        if (path == null || path.isBlank()) return "classificationnodes/" + group;
        return "classificationnodes/" + group + "/" + path;
    }

    public Map<String,Object> fetchNode(String project, String team, String endpoint, String apiVersion) {
        return azureService.getWitApiWithQuery(project, team, endpoint, null, apiVersion);
    }

    public String formatNodeResponse(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Respuesta vacía)";
        if (data.containsKey("error")) return "Error remoto: " + data.get("error");
        String name = Objects.toString(data.get("name"), null);
        String structureType = Objects.toString(data.get("structureType"), null);
        Object id = data.get("id");
        Object hasChildren = data.get("hasChildren");
        if (name != null || structureType != null || id != null) {
            StringBuilder sb = new StringBuilder("=== Classification Node ===\n\n");
            if (id != null) sb.append("Id: ").append(id).append('\n');
            if (name != null) sb.append("Name: ").append(name).append('\n');
            if (structureType != null) sb.append("Type: ").append(structureType).append('\n');
            if (hasChildren != null) sb.append("HasChildren: ").append(hasChildren).append('\n');
            return sb.toString();
        }
        return data.toString();
    }
}
