package com.mcp.server.tools.azuredevops.base;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.base.McpTool;
import com.mcp.server.protocol.types.Tool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clase base para herramientas Azure DevOps proporcionando:
 * - Validación estándar de parámetros
 * - Manejo uniforme de errores
 * - Esquema base (project, team)
 * - Utilidades de formateo simple
 */
public abstract class AbstractAzureDevOpsTool implements McpTool {

    protected final AzureDevOpsClientService azureService;

    protected AbstractAzureDevOpsTool(AzureDevOpsClientService azureService) {
        this.azureService = azureService; // puede ser null en tests unitarios de validación
    }

    @Override
    public final Map<String, Object> execute(Map<String, Object> arguments) {
        try {
            validateCommon(arguments);
            return executeInternal(arguments);
        } catch (IllegalArgumentException e) {
            return error("Parámetros inválidos: " + e.getMessage());
        } catch (Exception e) {
            return error("Error al ejecutar herramienta: " + e.getMessage());
        }
    }

    protected abstract Map<String,Object> executeInternal(Map<String,Object> arguments);

    protected void validateCommon(Map<String,Object> args) {
        Object project = args.get("project");
        if (project == null || project.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("El parámetro 'project' es requerido");
        }
    }

    protected String getProject(Map<String,Object> args) { return args.get("project").toString().trim(); }
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
        return m;
    }

    protected Map<String,Object> success(String text) {
        return Map.of(
            "isError", false,
            "content", List.of(Map.of("type", "text", "text", text))
        );
    }

    /**
     * Esquema base (project obligatorio, team opcional). Subclases pueden añadir propiedades.
     */
    protected Map<String,Object> createBaseSchema() {
        return Map.of(
            "type", "object",
            "properties", new HashMap<>(Map.of(
                "project", Map.of("type", "string", "description", "Nombre o ID del proyecto Azure DevOps"),
                "team", Map.of("type", "string", "description", "Nombre o ID del equipo (opcional)")
            )),
            "required", List.of("project")
        );
    }

    /** Permite a la subclase mutar el schema base antes de devolverlo. */
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

    /**
     * Intenta detectar y formatear un error remoto estándar de Azure DevOps (HTTP error JSON).
     * Si no hay error detectable, devuelve null.
     */
    @SuppressWarnings("unchecked")
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
