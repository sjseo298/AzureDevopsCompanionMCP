package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Herramienta MCP para listar proyectos de Azure DevOps (Core API /_apis/projects).
 */
@Component
public class ProjectsTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_core_get_projects";
    private static final String DESC = "Obtiene la lista de proyectos de la organización";

    @Autowired
    public ProjectsTool(AzureDevOpsClientService service) { super(service); }

    @Override
    public String getName() { return NAME; }

    @Override
    public String getDescription() { return DESC; }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        // Sin 'project' requerido. Validar opcionales.
        Object state = args.get("state");
        if (state != null && !state.toString().isBlank()) {
            String s = state.toString().trim();
            if (!Set.of("WellFormed","CreatePending","Deleted").contains(s)) {
                throw new IllegalArgumentException("'state' debe ser uno de: WellFormed|CreatePending|Deleted");
            }
        }
        Object top = args.get("top");
        if (top != null) {
            try {
                int t = Integer.parseInt(top.toString());
                if (t < 1 || t > 1000) throw new IllegalArgumentException("'top' debe estar entre 1 y 1000");
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("'top' debe ser numérico");
            }
        }
    }

    @Override
    public Map<String, Object> getInputSchema() {
        // Sin parámetros obligatorios; permitimos filtros opcionales: state, top, continuationToken
        Map<String,Object> base = new HashMap<>();
        base.put("type", "object");
        Map<String,Object> props = new HashMap<>();
        props.put("state", Map.of("type", "string", "description", "Filtro por estado del proyecto (WellFormed, CreatePending, Deleted)"));
        props.put("top", Map.of("type", "integer", "minimum", 1, "maximum", 1000, "description", "Número máximo de resultados"));
        props.put("continuationToken", Map.of("type", "string", "description", "Token de continuación para paginación"));
        base.put("properties", props);
        base.put("required", List.of());
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        Map<String,String> query = new LinkedHashMap<>();
        Object state = arguments.get("state");
        if (state != null && !state.toString().isBlank()) query.put("stateFilter", state.toString().trim());
        Object top = arguments.get("top");
        if (top != null) query.put("$top", String.valueOf(top));
        Object cont = arguments.get("continuationToken");
        if (cont != null && !cont.toString().isBlank()) query.put("continuationToken", cont.toString().trim());
        // api-version específica para Projects
        query.put("api-version", "7.2-preview.4");

        Map<String,Object> resp = azureService.getCoreApi("projects", query);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        // Formateo simple si es lista
        Object count = resp.get("count");
        Object value = resp.get("value");
        if (count instanceof Number && value instanceof List) {
            StringBuilder sb = new StringBuilder();
            @SuppressWarnings("unchecked") List<Map<String,Object>> items = (List<Map<String,Object>>) value;
            int idx = 1;
            for (Map<String,Object> it : items) {
                sb.append(idx++).append(") ")
                  .append(String.valueOf(it.getOrDefault("name", "<sin nombre>")))
                  .append(" [").append(String.valueOf(it.getOrDefault("id", "?"))).append("]\n");
            }
            if (sb.length() == 0) sb.append("Sin resultados");
            return success(sb.toString());
        }
        return Map.of("isError", false, "raw", resp);
    }
}
