package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_classification_nodes_update
 * Actualiza un nodo de clasificación (áreas/iteraciones) vía PATCH.
 */
@Component
public class ClassificationNodesUpdateTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_classification_nodes_update";
    private static final String DESC = "Actualiza un nodo de clasificación (areas/iterations) a nivel de proyecto.";
    private static final String API_VERSION_OVERRIDE = "7.2-preview"; // doc local

    @Autowired
    public ClassificationNodesUpdateTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String, Object>) base.get("properties");
        Map<String,Object> groupProp = new LinkedHashMap<>();
        groupProp.put("type", "string");
        groupProp.put("description", "'areas' o 'iterations'");
        groupProp.put("enum", List.of("areas", "iterations"));
        props.put("group", groupProp);
        props.put("path", Map.of("type","string","description","Ruta del nodo (obligatoria)"));
        props.put("name", Map.of("type","string","description","Nuevo nombre del nodo (opcional)"));
        base.put("required", List.of("project","group","path"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        String project = getProject(arguments);
        if (project == null || project.isBlank()) return error("Parámetro 'project' es obligatorio.");
        String team = getTeam(arguments);
        String group = Optional.ofNullable(arguments.get("group")).map(Object::toString).map(s -> s.trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (!"areas".equals(group) && !"iterations".equals(group)) return error("Parámetro 'group' inválido. Valores: areas, iterations.");
        String path = Optional.ofNullable(arguments.get("path")).map(Object::toString).filter(s -> !s.isBlank()).orElse(null);
        if (path == null) return error("Parámetro 'path' es obligatorio.");
        String endpoint = "classificationnodes/" + group + "/" + path;

        Map<String,Object> body = new LinkedHashMap<>();
        String name = Optional.ofNullable(arguments.get("name")).map(Object::toString).filter(s -> !s.isBlank()).orElse(null);
        if (name != null) body.put("name", name);

        Map<String,Object> resp = azureService.patchWitApi(project, team, endpoint, body.isEmpty()? Map.of(): body, API_VERSION_OVERRIDE);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return success(format(resp));
    }

    private String format(Map<String,Object> data) {
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
