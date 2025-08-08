package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Herramienta MCP para listar equipos (teams) de un proyecto.
 * Usa endpoint Core: GET /_apis/projects/{projectId}/teams
 */
@Component
public class TeamsTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_core_get_teams";
    private static final String DESC = "Obtiene equipos de un proyecto (requiere projectId)";

    @Autowired
    public TeamsTool(AzureDevOpsClientService service) { super(service); }

    @Override
    public String getName() { return NAME; }

    @Override
    public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new HashMap<>();
        base.put("type", "object");
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("projectId", Map.of("type", "string", "description", "ID del proyecto (GUID)"));
        props.put("top", Map.of("type", "integer", "minimum", 1, "maximum", 1000));
        props.put("skip", Map.of("type", "integer", "minimum", 0));
        base.put("properties", props);
        base.put("required", List.of("projectId"));
        return base;
    }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        Object pid = args.get("projectId");
        if (pid == null || pid.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("'projectId' es requerido (GUID)");
        }
        String s = pid.toString().trim();
        if (!s.matches("[0-9a-fA-F-]{36}")) {
            throw new IllegalArgumentException("'projectId' debe ser GUID de 36 chars");
        }
        Object top = args.get("top");
        if (top != null) {
            try { int t = Integer.parseInt(top.toString()); if (t < 1 || t > 1000) throw new IllegalArgumentException("'top' debe estar entre 1 y 1000"); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("'top' debe ser numérico"); }
        }
        Object skip = args.get("skip");
        if (skip != null) {
            try { int k = Integer.parseInt(skip.toString()); if (k < 0) throw new IllegalArgumentException("'skip' debe ser >= 0"); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("'skip' debe ser numérico"); }
        }
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        String projectId = arguments.get("projectId").toString().trim();
        Map<String,String> query = new LinkedHashMap<>();
        Object top = arguments.get("top");
        if (top != null) query.put("$top", String.valueOf(top));
        Object skip = arguments.get("skip");
        if (skip != null) query.put("$skip", String.valueOf(skip));
        // api-version específica para Teams
        query.put("api-version", "7.2-preview.3");

        Map<String,Object> resp = azureService.getCoreApi("projects/" + projectId + "/teams", query);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        Object count = resp.get("count"); Object value = resp.get("value");
        if (count instanceof Number && value instanceof List) {
            StringBuilder sb = new StringBuilder();
            @SuppressWarnings("unchecked") List<Map<String,Object>> items = (List<Map<String,Object>>) value;
            int idx = 1; for (Map<String,Object> it : items) {
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
