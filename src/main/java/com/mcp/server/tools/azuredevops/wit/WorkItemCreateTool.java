package com.mcp.server.tools.azuredevops.wit;


import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitWorkItemCreateHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Tool MCP: azuredevops_wit_work_item_create
 * Derivado de scripts/curl/wit/work_items_create.sh
 * Endpoint: POST /{project}/_apis/wit/workitems/${type}?api-version=7.2-preview
 */
@Component
public class WorkItemCreateTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_work_item_create";
    private static final String DESC = "Crea un work item en un proyecto Azure DevOps, permitiendo campos extra, herencia y relaciones.";
    private static final String DEFAULT_API_VERSION = "7.2-preview";

    private final WitWorkItemCreateHelper helper;

    public WorkItemCreateTool(AzureDevOpsClientService service) {
        super(service);
        this.helper = new WitWorkItemCreateHelper(service);
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    // Permitimos omitir project si se pasa parentId (se inferirá). La validación específica se hace en executeInternal.
    @Override
    protected boolean isProjectRequired() { return false; }

    @Override
    public Map<String,Object> getInputSchema() {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("project", Map.of("type","string","description","Nombre o ID del proyecto"));
        props.put("type", Map.of("type","string","description","Tipo de work item (Bug, User Story, etc.)"));
        props.put("title", Map.of("type","string","description","Título del work item"));
        props.put("area", Map.of("type","string","description","AreaPath (opcional)"));
        props.put("iteration", Map.of("type","string","description","IterationPath (opcional)"));
        props.put("description", Map.of("type","string","description","Descripción Markdown (opcional)"));
        props.put("state", Map.of("type","string","description","Estado inicial (opcional)"));
        props.put("parentId", Map.of("type","integer","description","ID de work item padre (opcional)"));
        props.put("fields", Map.of("type","string","description","Campos extra k=v separados por coma (opcional)"));
        props.put("relations", Map.of("type","string","description","Relaciones extra tipo:id[:comentario] separados por coma (opcional)"));
        props.put("apiVersion", Map.of("type","string","description","Versión de la API", "default", DEFAULT_API_VERSION));
        props.put("raw", Map.of("type","boolean","description","Devuelve JSON crudo de la respuesta"));
        props.put("validateOnly", Map.of("type","boolean","description","Valida sin persistir"));
        props.put("bypassRules", Map.of("type","boolean","description","Bypass rules"));
        props.put("suppressNotifications", Map.of("type","boolean","description","Suprime notificaciones"));
    props.put("debug", Map.of("type","boolean","description","Imprime JSON Patch (stderr)"));
    props.put("noDiagnostic", Map.of("type","boolean","description","No genera diagnóstico extendido de errores"));
        return Map.of(
            "type","object",
            "properties", props,
            // project ya no es obligatorio si hay parentId para inferir
            "required", List.of("type","title")
        );
    }

    @Override
    protected Map<String,Object> executeInternal(Map<String,Object> args) {
        String project = Objects.toString(args.get("project"),"");
        String type = Objects.toString(args.get("type"),"");
        String title = Objects.toString(args.get("title"),"");
    // Permitimos project vacío si se provee parentId (se intentará inferir del padre)
        Object parentIdObj = args.get("parentId");
        if (((project == null || project.isEmpty()) && parentIdObj == null) || type.isEmpty() || title.isEmpty()) {
            return error("Faltan parámetros obligatorios: type, title y project (o parentId para inferirlo)");
        }
        try {
            Map<String,Object> resp = helper.createWorkItem(args);
            // Manejo de error local (inferir proyecto u otros)
            if (Boolean.TRUE.equals(resp.get("isError")) && resp.get("message") != null) {
                return error(resp.get("message").toString());
            }
            String formattedErr = tryFormatRemoteError(resp);
            boolean raw = Boolean.TRUE.equals(args.get("raw"));
            boolean validateOnly = Boolean.TRUE.equals(args.get("validateOnly"));

            if (formattedErr != null) {
                // Adjuntar diagnóstico si existe
                Object diag = resp.get("diagnostic");
                if (diag instanceof String && !((String)diag).isBlank()) {
                    return success(formattedErr + "\n" + diag);
                }
                return success(formattedErr);
            }

            if (raw) {
                return Map.of("isError", false, "raw", resp);
            }
            // Formateo resumido
            Object id = resp.get("id");
            Object rev = resp.get("rev");
            Object fields = resp.get("fields");
            String titleOut = null; String stateOut = null;
            if (fields instanceof Map) {
                Object t = ((Map<?,?>)fields).get("System.Title");
                if (t != null) titleOut = t.toString();
                Object st = ((Map<?,?>)fields).get("System.State");
                if (st != null) stateOut = st.toString();
            }
            Object url = resp.get("url");
            StringBuilder sb = new StringBuilder();
            sb.append("Work item ");
            if (validateOnly) sb.append("(validateOnly) ");
            sb.append("creado: ID ").append(id != null ? id : "?");
            if (rev != null) sb.append(" Rev ").append(rev);
            if (titleOut != null) sb.append("\nTitle: ").append(titleOut);
            if (stateOut != null) sb.append("\nState: ").append(stateOut);
            if (url != null) sb.append("\nURL: ").append(url);
            Object diag = resp.get("diagnostic");
            if (diag instanceof String && !((String)diag).isBlank()) {
                sb.append("\n").append(diag);
            }
            return success(sb.toString());
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
