package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_classification_nodes_get
 * Obtiene un nodo de clasificación (áreas o iteraciones) o el root si no se especifica path.
 */
@Component
public class ClassificationNodesGetTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_classification_nodes_get";
    private static final String DESC = "Obtiene nodos de clasificación (areas/iterations) a nivel de proyecto.";

    // Según doc local: usar 7.2-preview (sin .1) para evitar respuestas no-JSON
    private static final String API_VERSION_OVERRIDE = "7.2-preview";

    @Autowired
    public ClassificationNodesGetTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>();
        base.putAll(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String, Object>) base.get("properties");
        Map<String,Object> groupProp = new LinkedHashMap<>();
        groupProp.put("type", "string");
        groupProp.put("description", "'areas' o 'iterations'");
        groupProp.put("enum", List.of("areas", "iterations"));
        props.put("group", groupProp);
        props.put("path", Map.of("type","string","description","Ruta del nodo (opcional)"));
        base.put("required", List.of("project","group"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        String project = getProject(arguments);
        if (project == null || project.isBlank()) return error("Parámetro 'project' es obligatorio.");
        String team = getTeam(arguments);
        Object groupObj = arguments.get("group");
        if (groupObj == null) return error("Parámetro 'group' es obligatorio (areas|iterations).");
        String group = groupObj.toString().trim().toLowerCase(Locale.ROOT);
        if (!"areas".equals(group) && !"iterations".equals(group)) {
            return error("Parámetro 'group' inválido. Valores permitidos: areas, iterations.");
        }
        String path = Optional.ofNullable(arguments.get("path")).map(Object::toString).filter(s -> !s.isBlank()).orElse(null);
        String endpoint = path == null ? ("classificationnodes/" + group) : ("classificationnodes/" + group + "/" + path);

        // Forzar api-version=7.2-preview como en el script validado para evitar respuestas no-JSON
        Map<String,Object> resp = azureService.getWitApiWithQuery(project, team, endpoint, null, API_VERSION_OVERRIDE);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return success(format(resp));
    }

    private String format(Map<String,Object> data) {
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
