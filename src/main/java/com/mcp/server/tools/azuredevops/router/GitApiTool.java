package com.mcp.server.tools.azuredevops.router;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class GitApiTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_git_api";
    private static final String DESC = "Router universal Git REST 7.2 (cobertura completa). operation: catálogo dinámico desde src/main/resources/git-endpoints-7.2.json.";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final EndpointCatalog CATALOG = loadCatalog();

    @Autowired
    public GitApiTool(AzureDevOpsClientService svc) {
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
    protected boolean isProjectRequired() {
        return true;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) base.get("properties");

        props.put("operation", Map.of(
                "type", "string",
                "enum", CATALOG.operations,
                "description", "Operación Git REST 7.2 a ejecutar (catálogo completo)."
        ));

        for (String param : CATALOG.pathParamNames) {
            String desc = switch (param) {
                case "repositoryId" -> "Path param repositoryId";
                case "repositoryNameOrId" -> "Path param repositoryNameOrId";
                case "pullRequestId" -> "Path param pullRequestId";
                case "reviewerId" -> "Path param reviewerId";
                case "threadId" -> "Path param threadId";
                case "commentId" -> "Path param commentId";
                case "iterationId" -> "Path param iterationId";
                case "statusId" -> "Path param statusId";
                case "labelIdOrName" -> "Path param labelIdOrName";
                case "fileName" -> "Path param fileName";
                case "favoriteId" -> "Path param favoriteId";
                case "pushId" -> "Path param pushId";
                case "commitId" -> "Path param commitId";
                case "sha1" -> "Path param sha1";
                case "objectId" -> "Path param objectId";
                case "cherryPickId" -> "Path param cherryPickId";
                case "revertId" -> "Path param revertId";
                case "mergeOperationId" -> "Path param mergeOperationId";
                case "importRequestId" -> "Path param importRequestId";
                case "forkSyncOperationId" -> "Path param forkSyncOperationId";
                case "collectionId" -> "Path param collectionId";
                default -> "Path param " + param;
            };
            props.put(param, Map.of("type", "string", "description", desc));
        }

        props.put("repositoryName", Map.of("type", "string", "description", "Alias para repositoryNameOrId/repositoryId cuando aplique"));
        props.put("query", Map.of("type", "string", "description", "Query string simple (a=b&c=d)"));
        props.put("queryJson", Map.of("type", "string", "description", "Query como JSON object. Ej: {\"$top\":10,\"searchCriteria.status\":\"active\"}"));
        props.put("bodyJson", Map.of("type", "string", "description", "Body JSON crudo para POST/PUT/PATCH"));
        props.put("contentType", Map.of("type", "string", "description", "Override Content-Type request (default application/json cuando bodyJson está presente)"));
        props.put("accept", Map.of("type", "string", "description", "Override Accept request"));
        props.put("responseType", Map.of("type", "string", "description", "auto|json|text|binary (default auto)"));
        props.put("outputPath", Map.of("type", "string", "description", "Si la respuesta es binaria, guarda el archivo localmente"));
        props.put("includeBase64", Map.of("type", "boolean", "description", "En respuesta binaria, incluir dataBase64 (por defecto true si no hay outputPath)"));
        props.put("maxBase64Chars", Map.of("type", "integer", "description", "Truncar dataBase64 a este máximo"));
        props.put("apiVersion", Map.of("type", "string", "description", "Override api-version"));
        props.put("raw", Map.of("type", "boolean", "description", "Devuelve JSON crudo"));

        base.put("required", List.of("project", "operation"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        String operation = str(arguments, "operation");
        if (operation.isBlank()) return error("'operation' es requerido");

        EndpointSpec spec = CATALOG.byOperation.get(operation);
        if (spec == null) {
            return error("Operación Git no soportada: " + operation + ". Catálogo disponible: " + CATALOG.operations.size() + " operaciones.");
        }

        try {
            String project = str(arguments, "project");
            if (spec.requiresProject && project.isBlank()) {
                return error("'project' es requerido para operation=" + operation);
            }

            String resolvedPath = resolvePathTemplate(spec, arguments);
            Map<String, String> query = buildQuery(arguments);
            Object body = parseBodyJsonIfPresent(arguments);

            String apiVersion = str(arguments, "apiVersion");
            if (apiVersion.isBlank()) apiVersion = spec.apiVersion;

            HttpMethod method = HttpMethod.valueOf(spec.method);
            MediaType contentType = parseMediaType(str(arguments, "contentType"));
            if (contentType == null && body != null) contentType = MediaType.APPLICATION_JSON;
            MediaType accept = parseMediaType(str(arguments, "accept"));

            boolean binaryResponse = "binary".equalsIgnoreCase(str(arguments, "responseType"));

            Map<String, Object> resp = azureService.exchangeGitApi(
                    project,
                    method,
                    resolvedPath,
                    query,
                    body,
                    apiVersion,
                    contentType,
                    accept,
                    binaryResponse
            );

            String remoteErr = tryFormatRemoteError(resp);
            if (remoteErr != null) return error(remoteErr);

            Map<String, Object> result = postProcessResponse(arguments, spec, resolvedPath, apiVersion, resp);

            if (parseBool(arguments.get("raw"))) return rawSuccess(result);
            return Map.of("isError", false, "result", result);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        } catch (Exception e) {
            return error("Error ejecutando operación git API: " + e.getMessage());
        }
    }

    private Map<String, Object> postProcessResponse(Map<String, Object> args,
                                                    EndpointSpec spec,
                                                    String resolvedPath,
                                                    String apiVersion,
                                                    Map<String, Object> resp) throws Exception {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("operation", spec.operation);
        out.put("method", spec.method);
        out.put("path", resolvedPath);
        out.put("apiVersion", apiVersion);
        out.put("doc", spec.doc);

        if (resp == null || resp.isEmpty()) {
            out.put("response", Map.of());
            return out;
        }

        String base64 = strObj(resp.get("data"));
        if (base64.isBlank()) {
            out.put("response", resp);
            return out;
        }

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

        Map<String, Object> binaryInfo = new LinkedHashMap<>();
        binaryInfo.put("contentType", Objects.toString(resp.get("contentType"), MediaType.APPLICATION_OCTET_STREAM_VALUE));
        binaryInfo.put("bytes", bytes.length);
        binaryInfo.put("downloadedAt", OffsetDateTime.now().toString());
        if (savedToPath != null) binaryInfo.put("savedToPath", savedToPath);

        if (includeBase64) {
            Integer max = parseInt(args.get("maxBase64Chars"));
            if (max != null && max > 0 && base64.length() > max) {
                binaryInfo.put("dataBase64", base64.substring(0, max));
                binaryInfo.put("base64Truncated", true);
                binaryInfo.put("base64ReturnedChars", max);
                binaryInfo.put("base64TotalChars", base64.length());
            } else {
                binaryInfo.put("dataBase64", base64);
                binaryInfo.put("base64Truncated", false);
            }
        } else {
            binaryInfo.put("base64Included", false);
        }

        out.put("response", binaryInfo);
        return out;
    }

    private Object parseBodyJsonIfPresent(Map<String, Object> args) throws Exception {
        String bodyJson = str(args, "bodyJson");
        if (bodyJson.isBlank()) return null;
        return JSON.readValue(bodyJson, Object.class);
    }

    private Map<String, String> buildQuery(Map<String, Object> args) throws Exception {
        Map<String, String> query = new LinkedHashMap<>();

        String raw = str(args, "query");
        if (!raw.isBlank()) {
            for (String part : raw.split("&")) {
                if (part == null || part.isBlank()) continue;
                int eq = part.indexOf('=');
                String k = eq >= 0 ? part.substring(0, eq) : part;
                String v = eq >= 0 ? part.substring(eq + 1) : "";
                String key = URLDecoder.decode(k, StandardCharsets.UTF_8);
                String value = URLDecoder.decode(v, StandardCharsets.UTF_8);
                if (!key.isBlank()) query.put(key, value);
            }
        }

        String queryJson = str(args, "queryJson");
        if (!queryJson.isBlank()) {
            Object parsed = JSON.readValue(queryJson, Object.class);
            if (!(parsed instanceof Map<?, ?> m)) {
                throw new IllegalArgumentException("'queryJson' debe ser un objeto JSON");
            }
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (e.getKey() == null) continue;
                query.put(e.getKey().toString(), stringifyQueryValue(e.getValue()));
            }
        }

        return query;
    }

    private String stringifyQueryValue(Object value) throws Exception {
        if (value == null) return "";
        if (value instanceof String s) return s;
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        return JSON.writeValueAsString(value);
    }

    private String resolvePathTemplate(EndpointSpec spec, Map<String, Object> args) {
        String path = spec.pathTemplate;
        for (String param : spec.pathParams) {
            String value = resolvePathParamValue(param, args);
            if (value.isBlank()) {
                throw new IllegalArgumentException("Falta path parameter '" + param + "' para operation=" + spec.operation);
            }
            path = path.replace("{" + param + "}", value);
        }
        return path;
    }

    private String resolvePathParamValue(String param, Map<String, Object> args) {
        return switch (param) {
            case "repositoryId" -> firstNonBlank(str(args, "repositoryId"), str(args, "repositoryNameOrId"), str(args, "repositoryName"));
            case "repositoryNameOrId" -> firstNonBlank(str(args, "repositoryNameOrId"), str(args, "repositoryId"), str(args, "repositoryName"));
            default -> str(args, param);
        };
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return "";
    }

    private MediaType parseMediaType(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return MediaType.parseMediaType(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("MediaType inválido: " + value);
        }
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

    private String str(Map<String, Object> args, String key) {
        Object v = args.get(key);
        return v == null ? "" : v.toString().trim();
    }

    private String strObj(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private static EndpointCatalog loadCatalog() {
        try (InputStream in = GitApiTool.class.getClassLoader().getResourceAsStream("git-endpoints-7.2.json")) {
            if (in == null) {
                throw new IllegalStateException("No se encontró recurso git-endpoints-7.2.json");
            }
            Map<String, Object> root = JSON.readValue(in, new TypeReference<>() {});
            Object entriesObj = root.get("entries");
            if (!(entriesObj instanceof List<?> entries)) {
                throw new IllegalStateException("Formato inválido de git-endpoints-7.2.json: falta 'entries' array");
            }

            Map<String, EndpointSpec> byOperation = new LinkedHashMap<>();
            Set<String> pathParams = new LinkedHashSet<>();

            for (Object item : entries) {
                if (!(item instanceof Map<?, ?> m)) continue;

                String operation = asString(m.get("operation"));
                String method = asString(m.get("method")).toUpperCase(Locale.ROOT);
                String pathTemplate = asString(m.get("pathTemplate"));
                List<String> params = asStringList(m.get("pathParams"));
                boolean requiresProject = Boolean.TRUE.equals(m.get("requiresProject"));
                String apiVersion = asString(m.get("apiVersion"));
                String href = asString(m.get("href"));
                String doc = asString(m.get("doc"));

                if (operation.isBlank() || method.isBlank() || pathTemplate.isBlank()) continue;

                EndpointSpec spec = new EndpointSpec(operation, method, pathTemplate, params, requiresProject, apiVersion, href, doc);
                byOperation.put(operation, spec);
                pathParams.addAll(params);
            }

            return new EndpointCatalog(
                    byOperation,
                    new ArrayList<>(byOperation.keySet()),
                    new ArrayList<>(pathParams)
            );
        } catch (Exception e) {
            throw new RuntimeException("No se pudo cargar catálogo Git 7.2", e);
        }
    }

    private static String asString(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private static List<String> asStringList(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        List<String> out = new ArrayList<>();
        for (Object item : list) {
            if (item == null) continue;
            String v = item.toString().trim();
            if (!v.isBlank()) out.add(v);
        }
        return out;
    }

    private record EndpointSpec(
            String operation,
            String method,
            String pathTemplate,
            List<String> pathParams,
            boolean requiresProject,
            String apiVersion,
            String href,
            String doc
    ) {}

    private record EndpointCatalog(
            Map<String, EndpointSpec> byOperation,
            List<String> operations,
            List<String> pathParamNames
    ) {}
}
