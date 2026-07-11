package com.mcp.server.tools.azuredevops.router;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class EnvironmentsTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_environments";
    private static final String DESC = "Operaciones Environments. operation: list|get|create|update|delete.";
    private static final String API_VERSION = "7.2-preview.1";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    public EnvironmentsTool(AzureDevOpsClientService svc) {
        super(svc);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESC;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) base.get("properties");

        props.put("operation", Map.of(
                "type", "string",
                "enum", List.of("list", "get", "create", "update", "delete"),
                "description", "Operación a ejecutar"
        ));

        props.put("environmentId", Map.of("type", "integer", "description", "ID del environment (get/update/delete)"));
        props.put("name", Map.of("type", "string", "description", "Filtro por nombre exacto (list)"));
        props.put("top", Map.of("type", "integer", "description", "Límite (list)"));
        props.put("continuationToken", Map.of("type", "string", "description", "Paginación (list)"));
        props.put("expands", Map.of("type", "string", "description", "Expansión (get)"));

        props.put("bodyJson", Map.of("type", "string", "description", "Body JSON crudo para create/update"));
        props.put("apiVersion", Map.of("type", "string", "description", "Override api-version"));
        props.put("raw", Map.of("type", "boolean", "description", "Devuelve JSON crudo"));

        base.put("required", List.of("project", "operation"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        String op = str(arguments, "operation");
        if (op.isBlank()) return error("'operation' es requerido");

        try {
            return switch (op) {
                case "list" -> opList(arguments);
                case "get" -> opGet(arguments);
                case "create" -> opCreate(arguments);
                case "update" -> opUpdate(arguments);
                case "delete" -> opDelete(arguments);
                default -> error("Operación no soportada: " + op);
            };
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        } catch (Exception e) {
            return error("Error ejecutando environments: " + e.getMessage());
        }
    }

    private Map<String, Object> opList(Map<String, Object> args) {
        String project = requireProject(args, "list");
        Map<String, String> q = new LinkedHashMap<>();
        putInt(q, "$top", args.get("top"));
        putIfNotBlank(q, "continuationToken", str(args, "continuationToken"));
        putIfNotBlank(q, "name", str(args, "name"));
        Map<String, Object> resp = azureService.getPipelinesApiWithQuery(project, "environments", q, apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opGet(Map<String, Object> args) {
        String project = requireProject(args, "get");
        Integer environmentId = requireInt(args, "environmentId");
        Map<String, String> q = new LinkedHashMap<>();
        putIfNotBlank(q, "expands", str(args, "expands"));
        Map<String, Object> resp = azureService.getPipelinesApiWithQuery(project, "environments/" + environmentId, q, apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opCreate(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "create");
        Object body = parseBodyJsonOrThrow(args, "create");
        Map<String, Object> resp = azureService.postPipelinesApiWithQuery(project, "environments", null, body, apiVersion(args), MediaType.APPLICATION_JSON);
        return done(args, resp);
    }

    private Map<String, Object> opUpdate(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "update");
        Integer environmentId = requireInt(args, "environmentId");
        Object body = parseBodyJsonOrThrow(args, "update");
        Map<String, Object> resp = azureService.patchPipelinesApiWithQuery(project, "environments/" + environmentId, null, body, apiVersion(args), MediaType.APPLICATION_JSON);
        return done(args, resp);
    }

    private Map<String, Object> opDelete(Map<String, Object> args) {
        String project = requireProject(args, "delete");
        Integer environmentId = requireInt(args, "environmentId");
        Map<String, Object> resp = azureService.deletePipelinesApiWithQuery(project, "environments/" + environmentId, null, apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> done(Map<String, Object> args, Map<String, Object> resp) {
        String err = tryFormatRemoteError(resp);
        if (err != null) return error(err);
        if (parseBool(args.get("raw"))) return rawSuccess(resp);
        return Map.of("isError", false, "result", resp);
    }

    private String requireProject(Map<String, Object> args, String op) {
        String p = str(args, "project");
        if (p.isBlank()) throw new IllegalArgumentException("'project' es requerido para " + op);
        return p;
    }

    private Integer requireInt(Map<String, Object> args, String key) {
        Integer n = parseInt(args.get(key));
        if (n == null) throw new IllegalArgumentException("'" + key + "' es requerido y numérico");
        return n;
    }

    private Object parseBodyJsonOrThrow(Map<String, Object> args, String op) throws Exception {
        String bodyJson = str(args, "bodyJson");
        if (bodyJson.isBlank()) {
            throw new IllegalArgumentException("'bodyJson' es requerido para " + op);
        }
        return JSON.readValue(bodyJson, Object.class);
    }

    private String apiVersion(Map<String, Object> args) {
        String v = str(args, "apiVersion");
        return v.isBlank() ? API_VERSION : v;
    }

    private void putInt(Map<String, String> q, String key, Object value) {
        Integer n = parseInt(value);
        if (n != null) q.put(key, String.valueOf(n));
    }

    private void putIfNotBlank(Map<String, String> q, String key, String value) {
        if (value != null && !value.isBlank()) q.put(key, value);
    }

    private String str(Map<String, Object> args, String key) {
        Object v = args.get(key);
        return v == null ? "" : v.toString().trim();
    }

    private Integer parseInt(Object value) {
        if (value == null) return null;
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean parseBool(Object value) {
        if (value instanceof Boolean b) return b;
        if (value == null) return false;
        String s = value.toString().trim().toLowerCase(Locale.ROOT);
        return "true".equals(s) || "1".equals(s) || "yes".equals(s) || "si".equals(s) || "sí".equals(s);
    }
}
