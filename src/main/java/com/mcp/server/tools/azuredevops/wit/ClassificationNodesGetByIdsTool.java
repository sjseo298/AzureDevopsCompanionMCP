package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_classification_nodes_get_by_ids
 * Obtiene nodos de clasificación por IDs (query param ids=...).
 */
@Component
public class ClassificationNodesGetByIdsTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_classification_nodes_get_by_ids";
    private static final String DESC = "Obtiene nodos de clasificación por IDs a nivel de proyecto.";
    private static final String API_VERSION_OVERRIDE = "7.2-preview";

    @Autowired
    public ClassificationNodesGetByIdsTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String, Object>) base.get("properties");
        props.put("ids", Map.of("type","string","description","Lista de IDs separados por coma"));
        base.put("required", List.of("project","ids"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        String project = getProject(arguments);
        String team = getTeam(arguments);
        String ids = Optional.ofNullable(arguments.get("ids")).map(Object::toString).map(String::trim).orElse("");
        if (ids.isEmpty()) return error("Parámetro 'ids' es obligatorio (lista separada por coma)");
        Map<String,String> query = new LinkedHashMap<>();
        query.put("ids", ids);
        Map<String,Object> resp = azureService.getWitApiWithQuery(project, team, "classificationnodes", query, API_VERSION_OVERRIDE);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return success(format(resp));
    }

    private String format(Map<String,Object> data) {
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
