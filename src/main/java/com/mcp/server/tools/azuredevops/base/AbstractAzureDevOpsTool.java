package com.mcp.server.tools.azuredevops.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.base.McpTool;
import com.mcp.server.protocol.types.Tool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractAzureDevOpsTool implements McpTool {

    protected final AzureDevOpsClientService azureService;
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    protected AbstractAzureDevOpsTool(AzureDevOpsClientService azureService) {
        this.azureService = azureService; // puede ser null en tests unitarios de validación
    }

    protected boolean isProjectRequired() { return true; }

    @Override
    public final Map<String, Object> execute(Map<String, Object> arguments) {
        try {
            validateCommon(arguments);
            Map<String,Object> out = executeInternal(arguments);
            return normalizeOutput(out);
        } catch (IllegalArgumentException e) {
            return error("Parámetros inválidos: " + e.getMessage());
        } catch (Exception e) {
            return error("Error al ejecutar herramienta: " + e.getMessage());
        }
    }

    protected abstract Map<String,Object> executeInternal(Map<String,Object> arguments);

    protected void validateCommon(Map<String,Object> args) {
        if (isProjectRequired()) {
            Object project = args.get("project");
            if (project == null || project.toString().trim().isEmpty()) {
                throw new IllegalArgumentException("El parámetro 'project' es requerido");
            }
        }
    }

    protected String getProject(Map<String,Object> args) {
        if (!isProjectRequired()) {
            Object p = args.get("project");
            if (p == null) return null;
            String s = p.toString().trim();
            return s.isEmpty() ? null : s;
        }
        return args.get("project").toString().trim();
    }
    protected String getTeam(Map<String,Object> args) {
        Object t = args.get("team");
        if (t == null) return null;
        String s = t.toString().trim();
        return s.isEmpty() ? null : s;
    }

    protected Map<String,Object> error(String msg) {
        Map<String,Object> m = new HashMap<>();
        m.put("isError", true);
        m.put("error", msg);
    // Incluir mensaje también en content para visualización uniforme en el cliente
    m.put("content", java.util.List.of(java.util.Map.of("type","text","text", msg)));
        return m;
    }

    protected Map<String,Object> success(String text) {
        return Map.of(
            "isError", false,
            "content", List.of(Map.of("type", "text", "text", text))
        );
    }

    protected String toJson(Object obj) {
        if (obj == null) return "null";
        try { return JSON.writeValueAsString(obj); }
        catch (Exception e) { return String.valueOf(obj); }
    }

    protected Map<String,Object> rawSuccess(Object raw) {
        return Map.of(
            "isError", false,
            "raw", raw,
            "content", List.of(Map.of("type","text","text", toJson(raw)))
        );
    }

    protected Map<String,Object> normalizeOutput(Map<String,Object> out) {
        if (out == null) return out;
    Object content = out.get("content");
    boolean hasContentList = content instanceof java.util.List;
    if (hasContentList) return out;

    // Prefer mostrar JSON del 'raw' si existe; si no, del 'result'; si no, del objeto completo
    Object raw = out.get("raw");
    Object result = out.get("result");
    Object source = raw != null ? raw : (result != null ? result : out);
    Map<String,Object> m = new java.util.HashMap<>(out);
    m.put("content", java.util.List.of(java.util.Map.of("type","text","text", toJson(source))));
    return m;
    }

    protected Map<String,Object> createBaseSchema() {
        Map<String,Object> props = new HashMap<>();
        props.put("project", Map.of("type", "string", "description", "Nombre o ID del proyecto Azure DevOps"));
        props.put("team", Map.of("type", "string", "description", "Nombre o ID del equipo (opcional)"));
        Map<String,Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", isProjectRequired() ? List.of("project") : List.of());
        return schema;
    }

    protected Map<String,Object> finalizeSchema(Map<String,Object> schema) { return schema; }

    public abstract String getName();
    public abstract String getDescription();

    public Map<String,Object> getInputSchema() { return finalizeSchema(createBaseSchema()); }

    @Override
    public Tool getToolDefinition() {
        return Tool.builder()
            .name(getName())
            .description(getDescription())
            .inputSchema(getInputSchema())
            .build();
    }

    protected String tryFormatRemoteError(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return null;
        if (data.containsKey("error")) {
            Object e = data.get("error");
            return "Error remoto: " + (e == null ? "(desconocido)" : e.toString());
        }
        boolean isHttpErr = Boolean.TRUE.equals(data.get("isHttpError"));
        Object message = data.get("message");
        Object typeKey = data.get("typeKey");
        Object typeName = data.get("typeName");
        Object httpStatus = data.get("httpStatus");
        Object httpReason = data.get("httpReason");
        if (isHttpErr || message != null || typeKey != null || typeName != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Error remoto");
            if (httpStatus != null) {
                sb.append(" (HTTP ").append(httpStatus);
                if (httpReason != null) sb.append(" - ").append(httpReason);
                sb.append(")");
            }
            if (message != null) sb.append(": ").append(message);
            if (typeKey != null || typeName != null) {
                sb.append(" (type: ").append(typeKey != null ? typeKey : typeName).append(")");
            }
            Object errorCode = data.get("errorCode");
            if (errorCode != null) sb.append(" [code: ").append(errorCode).append("]");
            return sb.toString();
        }
        return null;
    }
}
