package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Helper para actualizar nodos de clasificación (areas/iterations) a nivel proyecto.
 */
public class WitClassificationNodesUpdateHelper {
    private final AzureDevOpsClientService azureService;

    public WitClassificationNodesUpdateHelper(AzureDevOpsClientService azureService) {
        this.azureService = azureService;
    }

    public void validate(String project, String group, String path) {
        if (project == null || project.isBlank()) throw new IllegalArgumentException("Parámetro 'project' es obligatorio.");
        if (!"areas".equals(group) && !"iterations".equals(group)) throw new IllegalArgumentException("Parámetro 'group' inválido. Valores: areas, iterations.");
        if (path == null || path.isBlank()) throw new IllegalArgumentException("Parámetro 'path' es obligatorio.");
    }

    public String buildEndpoint(String group, String path) {
        return "classificationnodes/" + group + "/" + path;
    }

    public Map<String,Object> buildBody(Object nameArg) {
        Map<String,Object> body = new LinkedHashMap<>();
        if (nameArg != null && !nameArg.toString().isBlank()) body.put("name", nameArg.toString());
        return body;
    }

    public Map<String,Object> patchNode(String project, String team, String endpoint, Map<String,Object> body, String apiVersion) {
        return azureService.patchWitApi(project, team, endpoint, body, apiVersion);
    }

    public String formatNodeResponse(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Respuesta vacía)";
        String name = Objects.toString(data.get("name"), null);
        Object id = data.get("id");
        String type = Objects.toString(data.get("structureType"), null);
        StringBuilder sb = new StringBuilder("=== Classification Node (PATCH) ===\n\n");
        if (id != null) sb.append("Id: ").append(id).append('\n');
        if (name != null) sb.append("Name: ").append(name).append('\n');
        if (type != null) sb.append("Type: ").append(type).append('\n');
        return sb.toString();
    }
}
