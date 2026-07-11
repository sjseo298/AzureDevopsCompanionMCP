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
public class ReleaseTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_release";
    private static final String DESC = "Operaciones Release (vsrm). operation: definitions_list|releases_list|releases_get|releases_create|approvals_list.";

    private static final String API_DEFINITIONS = "7.2-preview.4";
    private static final String API_RELEASES = "7.2-preview.9";
    private static final String API_APPROVALS = "7.2-preview.3";

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    public ReleaseTool(AzureDevOpsClientService svc) {
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
                "enum", List.of("definitions_list", "releases_list", "releases_get", "releases_create", "approvals_list"),
                "description", "Operación a ejecutar"
        ));

        props.put("releaseId", Map.of("type", "integer", "description", "ID del release (releases_get)"));
        props.put("top", Map.of("type", "integer", "description", "Límite"));
        props.put("continuationToken", Map.of("type", "integer", "description", "Paginación"));
        props.put("searchText", Map.of("type", "string", "description", "Texto de búsqueda"));
        props.put("definitionId", Map.of("type", "integer", "description", "Filtro por definición"));
        props.put("isDeleted", Map.of("type", "boolean", "description", "Incluir eliminados"));
        props.put("statusFilter", Map.of("type", "string", "description", "Filtro de estado"));
        props.put("queryOrder", Map.of("type", "string", "description", "Orden de consulta"));
        props.put("expand", Map.of("type", "string", "description", "Valor para $expand"));

        props.put("bodyJson", Map.of("type", "string", "description", "Body JSON crudo (releases_create)"));
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
                case "definitions_list" -> opDefinitionsList(arguments);
                case "releases_list" -> opReleasesList(arguments);
                case "releases_get" -> opReleasesGet(arguments);
                case "releases_create" -> opReleasesCreate(arguments);
                case "approvals_list" -> opApprovalsList(arguments);
                default -> error("Operación no soportada: " + op);
            };
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        } catch (Exception e) {
            return error("Error ejecutando release: " + e.getMessage());
        }
    }

    private Map<String, Object> opDefinitionsList(Map<String, Object> args) {
        String project = requireProject(args, "definitions_list");
        Map<String, String> q = new LinkedHashMap<>();
        putIfNotBlank(q, "$expand", str(args, "expand"));
        putInt(q, "$top", args.get("top"));
        putInt(q, "continuationToken", args.get("continuationToken"));
        putInt(q, "definitionIdFilter", args.get("definitionId"));
        putIfNotBlank(q, "searchText", str(args, "searchText"));
        if (args.get("isDeleted") != null) q.put("isDeleted", String.valueOf(parseBool(args.get("isDeleted"))));
        putIfNotBlank(q, "queryOrder", str(args, "queryOrder"));

        Map<String, Object> resp = azureService.getReleaseApiWithQuery(project, "definitions", q, apiVersion(args, API_DEFINITIONS));
        return done(args, resp);
    }

    private Map<String, Object> opReleasesList(Map<String, Object> args) {
        String project = requireProject(args, "releases_list");
        Map<String, String> q = new LinkedHashMap<>();
        putIfNotBlank(q, "$expand", str(args, "expand"));
        putInt(q, "$top", args.get("top"));
        putInt(q, "continuationToken", args.get("continuationToken"));
        putInt(q, "definitionId", args.get("definitionId"));
        putIfNotBlank(q, "searchText", str(args, "searchText"));
        if (args.get("isDeleted") != null) q.put("isDeleted", String.valueOf(parseBool(args.get("isDeleted"))));
        putIfNotBlank(q, "statusFilter", str(args, "statusFilter"));
        putIfNotBlank(q, "queryOrder", str(args, "queryOrder"));

        Map<String, Object> resp = azureService.getReleaseApiWithQuery(project, "releases", q, apiVersion(args, API_RELEASES));
        return done(args, resp);
    }

    private Map<String, Object> opReleasesGet(Map<String, Object> args) {
        String project = requireProject(args, "releases_get");
        Integer releaseId = requireInt(args, "releaseId");
        Map<String, String> q = new LinkedHashMap<>();
        putIfNotBlank(q, "$expand", str(args, "expand"));
        Map<String, Object> resp = azureService.getReleaseApiWithQuery(project, "releases/" + releaseId, q, apiVersion(args, API_RELEASES));
        return done(args, resp);
    }

    private Map<String, Object> opReleasesCreate(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "releases_create");
        Object body = parseBodyJsonOrThrow(args, "releases_create");
        Map<String, Object> resp = azureService.postReleaseApiWithQuery(project, "releases", null, body, apiVersion(args, API_RELEASES), MediaType.APPLICATION_JSON);
        return done(args, resp);
    }

    private Map<String, Object> opApprovalsList(Map<String, Object> args) {
        String project = requireProject(args, "approvals_list");
        Map<String, String> q = new LinkedHashMap<>();
        putInt(q, "top", args.get("top"));
        putInt(q, "continuationToken", args.get("continuationToken"));
        putIfNotBlank(q, "statusFilter", str(args, "statusFilter"));
        putIfNotBlank(q, "queryOrder", str(args, "queryOrder"));
        Map<String, Object> resp = azureService.getReleaseApiWithQuery(project, "approvals", q, apiVersion(args, API_APPROVALS));
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

    private String apiVersion(Map<String, Object> args, String fallback) {
        String v = str(args, "apiVersion");
        return v.isBlank() ? fallback : v;
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
