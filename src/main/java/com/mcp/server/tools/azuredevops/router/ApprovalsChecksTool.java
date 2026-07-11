package com.mcp.server.tools.azuredevops.router;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ApprovalsChecksTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_approvals_checks";
    private static final String DESC = "Operaciones Approvals and Checks. operation: check_configurations_list|check_configurations_add|approvals_get|approvals_update|pipeline_permissions_get|pipeline_permissions_update_resource|pipeline_permissions_update_resources|evaluations_get|evaluations_evaluate.";

    private static final String API_CHECKS = "7.2-preview.1";
    private static final String API_APPROVALS = "7.2-preview.2";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    public ApprovalsChecksTool(AzureDevOpsClientService svc) {
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
                "enum", List.of(
                        "check_configurations_list",
                        "check_configurations_add",
                        "approvals_get",
                        "approvals_update",
                        "pipeline_permissions_get",
                        "pipeline_permissions_update_resource",
                        "pipeline_permissions_update_resources",
                        "evaluations_get",
                        "evaluations_evaluate"
                ),
                "description", "Operación a ejecutar"
        ));

        props.put("approvalId", Map.of("type", "string", "description", "ID de aprobación (approvals_get)"));
        props.put("checkSuiteId", Map.of("type", "string", "description", "ID del check suite (evaluations_get)"));
        props.put("resourceType", Map.of("type", "string", "description", "Tipo de recurso para pipeline permissions"));
        props.put("resourceId", Map.of("type", "string", "description", "ID de recurso para pipeline permissions/check configurations"));
        props.put("expand", Map.of("type", "string", "description", "Valor para $expand"));
        props.put("bodyJson", Map.of("type", "string", "description", "Body JSON crudo para operaciones POST/PATCH"));
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
                case "check_configurations_list" -> opCheckConfigurationsList(arguments);
                case "check_configurations_add" -> opCheckConfigurationsAdd(arguments);
                case "approvals_get" -> opApprovalsGet(arguments);
                case "approvals_update" -> opApprovalsUpdate(arguments);
                case "pipeline_permissions_get" -> opPipelinePermissionsGet(arguments);
                case "pipeline_permissions_update_resource" -> opPipelinePermissionsUpdateResource(arguments);
                case "pipeline_permissions_update_resources" -> opPipelinePermissionsUpdateResources(arguments);
                case "evaluations_get" -> opEvaluationsGet(arguments);
                case "evaluations_evaluate" -> opEvaluationsEvaluate(arguments);
                default -> error("Operación no soportada: " + op);
            };
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        } catch (Exception e) {
            return error("Error ejecutando approvals/checks: " + e.getMessage());
        }
    }

    private Map<String, Object> opCheckConfigurationsList(Map<String, Object> args) {
        String project = requireProject(args, "check_configurations_list");
        Map<String, String> q = new LinkedHashMap<>();
        putIfNotBlank(q, "$expand", str(args, "expand"));
        putIfNotBlank(q, "resourceType", str(args, "resourceType"));
        putIfNotBlank(q, "resourceId", str(args, "resourceId"));
        Map<String, Object> resp = azureService.getPipelinesApiWithQuery(project, "checks/configurations", q, apiVersion(args, false));
        return done(args, resp);
    }

    private Map<String, Object> opCheckConfigurationsAdd(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "check_configurations_add");
        Object body = parseBodyJsonOrThrow(args, "check_configurations_add");
        Map<String, Object> resp = azureService.postPipelinesApiWithQuery(project, "checks/configurations", null, body, apiVersion(args, false), MediaType.APPLICATION_JSON);
        return done(args, resp);
    }

    private Map<String, Object> opApprovalsGet(Map<String, Object> args) {
        String project = requireProject(args, "approvals_get");
        String approvalId = requireString(args, "approvalId");
        Map<String, String> q = new LinkedHashMap<>();
        putIfNotBlank(q, "$expand", str(args, "expand"));
        Map<String, Object> resp = azureService.getPipelinesApiWithQuery(project, "approvals/" + approvalId, q, apiVersion(args, true));
        return done(args, resp);
    }

    private Map<String, Object> opApprovalsUpdate(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "approvals_update");
        Object body = parseBodyJsonOrThrow(args, "approvals_update");
        Map<String, Object> resp = azureService.exchangeDevAreaApi(
                project,
                "pipelines",
                HttpMethod.PATCH,
                "approvals",
                null,
                body,
                apiVersion(args, true),
                MediaType.APPLICATION_JSON,
                null,
                false
        );
        return done(args, resp);
    }

    private Map<String, Object> opPipelinePermissionsGet(Map<String, Object> args) {
        String project = requireProject(args, "pipeline_permissions_get");
        String resourceType = requireString(args, "resourceType");
        String resourceId = requireString(args, "resourceId");
        Map<String, Object> resp = azureService.getPipelinesApiWithQuery(project, "pipelinepermissions/" + resourceType + "/" + resourceId, null, apiVersion(args, false));
        return done(args, resp);
    }

    private Map<String, Object> opPipelinePermissionsUpdateResource(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "pipeline_permissions_update_resource");
        String resourceType = requireString(args, "resourceType");
        String resourceId = requireString(args, "resourceId");
        Object body = parseBodyJsonOrThrow(args, "pipeline_permissions_update_resource");
        Map<String, Object> resp = azureService.exchangeDevAreaApi(
                project,
                "pipelines",
                HttpMethod.PATCH,
                "pipelinepermissions/" + resourceType + "/" + resourceId,
                null,
                body,
                apiVersion(args, false),
                MediaType.APPLICATION_JSON,
                null,
                false
        );
        return done(args, resp);
    }

    private Map<String, Object> opPipelinePermissionsUpdateResources(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "pipeline_permissions_update_resources");
        Object body = parseBodyJsonOrThrow(args, "pipeline_permissions_update_resources");
        Map<String, Object> resp = azureService.exchangeDevAreaApi(
                project,
                "pipelines",
                HttpMethod.PATCH,
                "pipelinepermissions",
                null,
                body,
                apiVersion(args, false),
                MediaType.APPLICATION_JSON,
                null,
                false
        );
        return done(args, resp);
    }

    private Map<String, Object> opEvaluationsGet(Map<String, Object> args) {
        String project = requireProject(args, "evaluations_get");
        String checkSuiteId = requireString(args, "checkSuiteId");
        Map<String, String> q = new LinkedHashMap<>();
        putIfNotBlank(q, "$expand", str(args, "expand"));
        Map<String, Object> resp = azureService.getPipelinesApiWithQuery(project, "checks/runs/" + checkSuiteId, q, apiVersion(args, false));
        return done(args, resp);
    }

    private Map<String, Object> opEvaluationsEvaluate(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "evaluations_evaluate");
        Map<String, String> q = new LinkedHashMap<>();
        putIfNotBlank(q, "$expand", str(args, "expand"));
        Object body = parseBodyJsonOrThrow(args, "evaluations_evaluate");
        Map<String, Object> resp = azureService.postPipelinesApiWithQuery(project, "checks/runs", q, body, apiVersion(args, false), MediaType.APPLICATION_JSON);
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

    private String apiVersion(Map<String, Object> args, boolean approvals) {
        String v = str(args, "apiVersion");
        if (!v.isBlank()) return v;
        return approvals ? API_APPROVALS : API_CHECKS;
    }

    private void putIfNotBlank(Map<String, String> q, String key, String value) {
        if (value != null && !value.isBlank()) q.put(key, value);
    }

    private String str(Map<String, Object> args, String key) {
        Object v = args.get(key);
        return v == null ? "" : v.toString().trim();
    }

    private boolean parseBool(Object value) {
        if (value instanceof Boolean b) return b;
        if (value == null) return false;
        String s = value.toString().trim().toLowerCase(Locale.ROOT);
        return "true".equals(s) || "1".equals(s) || "yes".equals(s) || "si".equals(s) || "sí".equals(s);
    }
}
