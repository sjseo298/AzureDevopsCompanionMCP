package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitWorkItemUpdateHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_work_item_update
 * Derivado de scripts/curl/wit/work_items_update.sh
 * Endpoint: PATCH /{project}/_apis/wit/workitems/{id}?api-version=7.2-preview
 */
public class WorkItemUpdateTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_work_item_update";
    private static final String DESC = "Actualiza un work item (add/replace/remove, shortcuts de campos, relaciones, re-parenting y diagnóstico).";
    private static final String DEFAULT_API_VERSION = "7.2-preview";

    private final WitWorkItemUpdateHelper helper;

    public WorkItemUpdateTool(AzureDevOpsClientService service) {
        super(service);
        this.helper = new WitWorkItemUpdateHelper(service);
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String,Object> getInputSchema() {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("project", Map.of("type","string","description","Nombre o ID del proyecto"));
        props.put("id", Map.of("type","integer","description","ID numérico del work item"));
    // Aliases con guiones para paridad con script bash
    props.put("api-version", Map.of("type","string","description","Alias de apiVersion (script: --api-version)"));
        props.put("add", Map.of("type","string","description","Campos op add k=v separados por coma"));
        props.put("replace", Map.of("type","string","description","Campos op replace k=v separados por coma"));
        props.put("remove", Map.of("type","string","description","Campos (ref) separados por coma para op remove"));
        props.put("state", Map.of("type","string","description","Atajo System.State"));
        props.put("title", Map.of("type","string","description","Atajo System.Title"));
        props.put("description", Map.of("type","string","description","Atajo System.Description"));
        props.put("area", Map.of("type","string","description","Atajo System.AreaPath"));
        props.put("iteration", Map.of("type","string","description","Atajo System.IterationPath"));
        props.put("parentId", Map.of("type","integer","description","ID del work item padre para re-parenting"));
    props.put("parent", Map.of("type","integer","description","Alias de parentId (script: --parent)"));
        props.put("relations", Map.of("type","string","description","Relaciones extra tipo:id[:comentario] separadas por coma"));
        props.put("apiVersion", Map.of("type","string","description","Versión API a usar","default", DEFAULT_API_VERSION));
        props.put("validateOnly", Map.of("type","boolean","description","validateOnly=true (no persiste)"));
    props.put("validate-only", Map.of("type","boolean","description","Alias de validateOnly (script: --validate-only)"));
        props.put("bypassRules", Map.of("type","boolean","description","bypassRules=true"));
    props.put("bypass-rules", Map.of("type","boolean","description","Alias de bypassRules (script: --bypass-rules)"));
        props.put("suppressNotifications", Map.of("type","boolean","description","suppressNotifications=true"));
    props.put("suppress-notifications", Map.of("type","boolean","description","Alias de suppressNotifications (script: --suppress-notifications)"));
        props.put("debug", Map.of("type","boolean","description","Imprime JSON Patch (stderr)"));
        props.put("noDiagnostic", Map.of("type","boolean","description","Desactiva diagnóstico enriquecido"));
    props.put("no-diagnostic", Map.of("type","boolean","description","Alias de noDiagnostic (script: --no-diagnostic)"));
        props.put("raw", Map.of("type","boolean","description","Devuelve JSON crudo"));
        return Map.of(
            "type","object",
            "properties", props,
            "required", List.of("project","id")
        );
    }

    @Override
    protected Map<String,Object> executeInternal(Map<String,Object> args) {
        String project = Objects.toString(args.get("project"), "");
        Object idObj = args.get("id");
        if (project.isEmpty() || idObj == null) return error("Faltan project o id");
        try {
            // Normalizar aliases hyphen->camel
            mapFlagAlias(args, "api-version", "apiVersion");
            mapFlagAlias(args, "parent", "parentId");
            mapFlagAlias(args, "validate-only", "validateOnly");
            mapFlagAlias(args, "bypass-rules", "bypassRules");
            mapFlagAlias(args, "suppress-notifications", "suppressNotifications");
            mapFlagAlias(args, "no-diagnostic", "noDiagnostic");
            Map<String,Object> resp = helper.update(args);
            String formattedErr = tryFormatRemoteError(resp);
            boolean raw = Boolean.TRUE.equals(args.get("raw"));
            boolean validateOnly = Boolean.TRUE.equals(args.get("validateOnly"));
            // Paridad con script: si raw=true devolver siempre JSON crudo aunque sea error
            if (raw) return Map.of("isError", false, "raw", resp);
            if (formattedErr != null) {
                Object diag = resp.get("diagnostic");
                if (diag instanceof String && !((String)diag).isBlank()) {
                    return success(formattedErr + "\n" + diag);
                }
                return success(formattedErr);
            }
            // Resumen
            Object id = resp.get("id");
            Object rev = resp.get("rev");
            String title = null; String state = null; Object url = resp.get("url");
            Object fields = resp.get("fields");
            if (fields instanceof Map<?,?> fm) {
                Object t = fm.get("System.Title"); if (t != null) title = t.toString();
                Object st = fm.get("System.State"); if (st != null) state = st.toString();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Work item ");
            if (validateOnly) sb.append("(validateOnly) ");
            sb.append("actualizado: ID ").append(id != null ? id : "?");
            if (rev != null) sb.append(" Rev ").append(rev);
            if (title != null) sb.append("\nTitle: ").append(title);
            if (state != null) sb.append("\nState: ").append(state);
            if (url != null) sb.append("\nURL: ").append(url);
            Object diag = resp.get("diagnostic");
            if (diag instanceof String s && !s.isBlank()) sb.append("\n").append(s);
            return success(sb.toString());
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    private void mapFlagAlias(Map<String,Object> args, String alias, String target) {
        if (args.containsKey(target)) return;
        if (args.containsKey(alias)) args.put(target, args.get(alias));
    }
}
