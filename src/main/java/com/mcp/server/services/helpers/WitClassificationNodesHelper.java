package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Helper para operaciones de nodos de clasificación (areas/iterations) a nivel proyecto.
 */
@Component
public class WitClassificationNodesHelper {
    private final AzureDevOpsClientService azureService;

    public WitClassificationNodesHelper(AzureDevOpsClientService azureService) {
        this.azureService = azureService;
    }

    public void validateCreateOrUpdate(String project, String group, String path) {
        if (project == null || project.isBlank()) throw new IllegalArgumentException("Parámetro 'project' es obligatorio.");
        if (!"areas".equals(group) && !"iterations".equals(group)) throw new IllegalArgumentException("Parámetro 'group' inválido. Valores: areas, iterations.");
        if (path == null || path.isBlank()) throw new IllegalArgumentException("Parámetro 'path' es obligatorio.");
    }

    public String resolveName(String path, Object nameArg) {
        if (nameArg != null && !nameArg.toString().isBlank()) return nameArg.toString();
        String[] parts = path.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : path;
    }

    public Map<String,Object> buildBody(String name) {
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("name", name);
        return body;
    }

    public Map<String,Object> putNode(String project, String team, String group, String path, Map<String,Object> body, String apiVersion) {
        String endpoint = "classificationnodes/" + group + "/" + path;
        return azureService.putWitApi(project, team, endpoint, body, apiVersion);
    }

    public String formatNodeResponse(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Respuesta vacía)";
        String name = Objects.toString(data.get("name"), null);
        Object id = data.get("id");
        String type = Objects.toString(data.get("structureType"), null);
        StringBuilder sb = new StringBuilder("=== Classification Node (PUT) ===\n\n");
        if (id != null) sb.append("Id: ").append(id).append('\n');
        if (name != null) sb.append("Name: ").append(name).append('\n');
        if (type != null) sb.append("Type: ").append(type).append('\n');
        return sb.toString();
    }
}
