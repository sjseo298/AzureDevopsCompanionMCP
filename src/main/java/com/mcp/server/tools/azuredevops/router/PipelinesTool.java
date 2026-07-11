package com.mcp.server.tools.azuredevops.router;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class PipelinesTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_pipelines";
    private static final String DESC = "Operaciones Pipelines. operation: list|get|create|runs_list|runs_get|run|logs_list|logs_get|preview|artifact_get.";
    private static final String API_VERSION = "7.2-preview.1";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    public PipelinesTool(AzureDevOpsClientService svc) {
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
                "enum", List.of("list", "get", "create", "runs_list", "runs_get", "run", "logs_list", "logs_get", "preview", "artifact_get"),
                "description", "Operación a ejecutar"
        ));

        props.put("pipelineId", Map.of("type", "integer", "description", "ID del pipeline"));
        props.put("runId", Map.of("type", "integer", "description", "ID del run"));
        props.put("logId", Map.of("type", "integer", "description", "ID del log"));
        props.put("artifactName", Map.of("type", "string", "description", "Nombre del artifact para artifact_get"));

        props.put("pipelineVersion", Map.of("type", "integer", "description", "Versión específica del pipeline"));
        props.put("top", Map.of("type", "integer", "description", "Límite (list)"));
        props.put("continuationToken", Map.of("type", "string", "description", "Paginación (list)"));
        props.put("orderBy", Map.of("type", "string", "description", "Ordenamiento (list)"));
        props.put("expand", Map.of("type", "string", "description", "Valor para $expand"));

        props.put("bodyJson", Map.of("type", "string", "description", "Body JSON crudo para create/run/preview"));
        props.put("apiVersion", Map.of("type", "string", "description", "Override api-version"));
        props.put("raw", Map.of("type", "boolean", "description", "Devuelve JSON crudo"));

        props.put("responseType", Map.of("type", "string", "description", "auto|json|text|binary (default auto). Para logs_get/artifact_get."));
        props.put("outputPath", Map.of("type", "string", "description", "Si la respuesta es binaria, guarda archivo local"));
        props.put("includeBase64", Map.of("type", "boolean", "description", "En binario incluir dataBase64"));
        props.put("maxBase64Chars", Map.of("type", "integer", "description", "Máximo caracteres base64 a retornar"));

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
                case "runs_list" -> opRunsList(arguments);
                case "runs_get" -> opRunsGet(arguments);
                case "run" -> opRun(arguments);
                case "logs_list" -> opLogsList(arguments);
                case "logs_get" -> opLogsGet(arguments);
                case "preview" -> opPreview(arguments);
                case "artifact_get" -> opArtifactGet(arguments);
                default -> error("Operación no soportada: " + op);
            };
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        } catch (Exception e) {
            return error("Error ejecutando pipelines: " + e.getMessage());
        }
    }

    private Map<String, Object> opList(Map<String, Object> args) {
        String project = requireProject(args, "list");
        Map<String, String> q = new LinkedHashMap<>();
        putInt(q, "$top", args.get("top"));
        putIfNotBlank(q, "continuationToken", str(args, "continuationToken"));
        putIfNotBlank(q, "orderBy", str(args, "orderBy"));
        Map<String, Object> resp = azureService.getPipelinesApiWithQuery(project, "", q, apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opGet(Map<String, Object> args) {
        String project = requireProject(args, "get");
        Integer pipelineId = requireInt(args, "pipelineId");
        Map<String, String> q = new LinkedHashMap<>();
        putInt(q, "pipelineVersion", args.get("pipelineVersion"));
        Map<String, Object> resp = azureService.getPipelinesApiWithQuery(project, String.valueOf(pipelineId), q, apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opCreate(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "create");
        Object body = parseBodyJsonOrThrow(args, "create");
        Map<String, Object> resp = azureService.postPipelinesApiWithQuery(project, "", null, body, apiVersion(args), MediaType.APPLICATION_JSON);
        return done(args, resp);
    }

    private Map<String, Object> opRunsList(Map<String, Object> args) {
        String project = requireProject(args, "runs_list");
        Integer pipelineId = requireInt(args, "pipelineId");
        Map<String, Object> resp = azureService.getPipelinesApiWithQuery(project, pipelineId + "/runs", new LinkedHashMap<>(), apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opRunsGet(Map<String, Object> args) {
        String project = requireProject(args, "runs_get");
        Integer pipelineId = requireInt(args, "pipelineId");
        Integer runId = requireInt(args, "runId");
        Map<String, Object> resp = azureService.getPipelinesApiWithQuery(project, pipelineId + "/runs/" + runId, new LinkedHashMap<>(), apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opRun(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "run");
        Integer pipelineId = requireInt(args, "pipelineId");
        Map<String, String> q = new LinkedHashMap<>();
        putInt(q, "pipelineVersion", args.get("pipelineVersion"));
        Object body = parseBodyJsonIfPresent(args);
        Map<String, Object> resp = azureService.postPipelinesApiWithQuery(project, pipelineId + "/runs", q, body, apiVersion(args), MediaType.APPLICATION_JSON);
        return done(args, resp);
    }

    private Map<String, Object> opLogsList(Map<String, Object> args) {
        String project = requireProject(args, "logs_list");
        Integer pipelineId = requireInt(args, "pipelineId");
        Integer runId = requireInt(args, "runId");
        Map<String, String> q = new LinkedHashMap<>();
        putIfNotBlank(q, "$expand", str(args, "expand"));
        Map<String, Object> resp = azureService.getPipelinesApiWithQuery(project, pipelineId + "/runs/" + runId + "/logs", q, apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opLogsGet(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "logs_get");
        Integer pipelineId = requireInt(args, "pipelineId");
        Integer runId = requireInt(args, "runId");
        Integer logId = requireInt(args, "logId");
        Map<String, String> q = new LinkedHashMap<>();
        putIfNotBlank(q, "$expand", str(args, "expand"));
        return runMaybeBinary(args, project, pipelineId + "/runs/" + runId + "/logs/" + logId, q);
    }

    private Map<String, Object> opPreview(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "preview");
        Integer pipelineId = requireInt(args, "pipelineId");
        Map<String, String> q = new LinkedHashMap<>();
        putInt(q, "pipelineVersion", args.get("pipelineVersion"));
        Object body = parseBodyJsonIfPresent(args);
        Map<String, Object> resp = azureService.postPipelinesApiWithQuery(project, pipelineId + "/preview", q, body, apiVersion(args), MediaType.APPLICATION_JSON);
        return done(args, resp);
    }

    private Map<String, Object> opArtifactGet(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "artifact_get");
        Integer pipelineId = requireInt(args, "pipelineId");
        Integer runId = requireInt(args, "runId");
        String artifactName = requireString(args, "artifactName");
        Map<String, String> q = new LinkedHashMap<>();
        q.put("artifactName", artifactName);
        putIfNotBlank(q, "$expand", str(args, "expand"));
        return runMaybeBinary(args, project, pipelineId + "/runs/" + runId + "/artifacts", q);
    }

    private Map<String, Object> runMaybeBinary(Map<String, Object> args, String project, String path, Map<String, String> query) throws Exception {
        boolean binary = "binary".equalsIgnoreCase(str(args, "responseType"));
        Map<String, Object> resp = azureService.exchangeDevAreaApi(
                project,
                "pipelines",
                HttpMethod.GET,
                path,
                query,
                null,
                apiVersion(args),
                null,
                null,
                binary
        );
        if (!binary) return done(args, resp);

        String err = tryFormatRemoteError(resp);
        if (err != null) return error(err);
        return doneBinary(args, resp);
    }

    private Map<String, Object> doneBinary(Map<String, Object> args, Map<String, Object> resp) throws Exception {
        String base64 = strObj(resp.get("data"));
        if (base64.isBlank()) return done(args, resp);

        byte[] bytes = Base64.getDecoder().decode(base64);
        String outputPath = str(args, "outputPath");
        String savedToPath = null;
        if (!outputPath.isBlank()) {
            Path outPath = Path.of(outputPath).toAbsolutePath().normalize();
            Path parent = outPath.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.write(outPath, bytes);
            savedToPath = outPath.toString();
        }

        boolean includeBase64 = args.containsKey("includeBase64")
                ? parseBool(args.get("includeBase64"))
                : savedToPath == null;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("contentType", strObj(resp.get("contentType")).isBlank() ? MediaType.APPLICATION_OCTET_STREAM_VALUE : strObj(resp.get("contentType")));
        out.put("bytes", bytes.length);
        out.put("downloadedAt", OffsetDateTime.now().toString());
        if (savedToPath != null) out.put("savedToPath", savedToPath);

        if (includeBase64) {
            Integer max = parseInt(args.get("maxBase64Chars"));
            if (max != null && max > 0 && base64.length() > max) {
                out.put("dataBase64", base64.substring(0, max));
                out.put("base64Truncated", true);
                out.put("base64ReturnedChars", max);
                out.put("base64TotalChars", base64.length());
            } else {
                out.put("dataBase64", base64);
                out.put("base64Truncated", false);
            }
        } else {
            out.put("base64Included", false);
        }

        if (parseBool(args.get("raw"))) return rawSuccess(out);
        return Map.of("isError", false, "result", out);
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

    private String requireString(Map<String, Object> args, String key) {
        String s = str(args, key);
        if (s.isBlank()) throw new IllegalArgumentException("'" + key + "' es requerido");
        return s;
    }

    private Object parseBodyJsonOrThrow(Map<String, Object> args, String op) throws Exception {
        String bodyJson = str(args, "bodyJson");
        if (bodyJson.isBlank()) {
            throw new IllegalArgumentException("'bodyJson' es requerido para " + op);
        }
        return JSON.readValue(bodyJson, Object.class);
    }

    private Object parseBodyJsonIfPresent(Map<String, Object> args) throws Exception {
        String bodyJson = str(args, "bodyJson");
        if (bodyJson.isBlank()) return Map.of();
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

    private String strObj(Object value) {
        return value == null ? "" : value.toString().trim();
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
