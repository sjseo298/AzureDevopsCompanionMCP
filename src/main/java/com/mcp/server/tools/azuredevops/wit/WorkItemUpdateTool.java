package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_work_item_update
 * Derivado de scripts/curl/wit/work_items_update.sh y api_doc/wit_sections/work_items.md (operación Update).
 * Aplica operaciones JSON Patch (add/replace/remove) sobre un work item existente, con atajos y soporte de re-parenting y relaciones.
 * Incluye parámetros validateOnly, bypassRules, suppressNotifications (documentación oficial Work Items 7.2-preview).
 * Nota: Enriquecimiento detallado de RuleValidationErrors (script) pendiente de portar si se requiere.
 */
@Component
public class WorkItemUpdateTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_work_item_update";
    private static final String DESC = "Actualiza un work item (JSON Patch) soportando add/replace/remove, atajos de campos, herencia de padre y relaciones.";
    private static final String API_VERSION = "7.2-preview";

    @Autowired
    public WorkItemUpdateTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String,Object>) base.get("properties");
        props.put("id", Map.of("type","integer","description","ID del work item a actualizar"));
        props.put("add", Map.of("type","string","description","Lista k=v separada por comas (op add)"));
        props.put("replace", Map.of("type","string","description","Lista k=v separada por comas (op replace)"));
        props.put("remove", Map.of("type","string","description","Lista separada por comas de referenceNames para op remove"));
        props.put("state", Map.of("type","string","description","Atajo System.State (op add)"));
        props.put("title", Map.of("type","string","description","Atajo System.Title (op add)"));
        props.put("description", Map.of("type","string","description","Atajo System.Description (op add)"));
        props.put("area", Map.of("type","string","description","Atajo System.AreaPath (op add)"));
        props.put("iteration", Map.of("type","string","description","Atajo System.IterationPath (op add)"));
        props.put("parentId", Map.of("type","integer","description","ID de work item padre (Hierarchy-Reverse) con herencia Area/Iteration si faltan"));
        props.put("relations", Map.of("type","string","description","Relaciones extra tipo:id[:comentario] separadas por comas"));
        props.put("bypassRules", Map.of("type","boolean","description","Query param bypassRules=true"));
        props.put("suppressNotifications", Map.of("type","boolean","description","Query param suppressNotifications=true"));
        props.put("validateOnly", Map.of("type","boolean","description","Valida sin persistir (validateOnly=true)"));
        props.put("apiVersion", Map.of("type","string","description","Override api-version (default "+API_VERSION+")"));
        props.put("raw", Map.of("type","boolean","description","Devuelve JSON crudo de la respuesta"));
        @SuppressWarnings("unchecked") List<String> req = new ArrayList<>((List<String>) base.get("required"));
        if (!req.contains("id")) req.add("id");
        base.put("required", req);
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado");
        String project = getProject(arguments);
        String idStr = str(arguments.get("id"));
        if (idStr == null || !idStr.matches("\\d+")) return error("'id' es requerido y numérico");

        List<Map<String,Object>> patch = new ArrayList<>();
        addIfPresent(patch, "System.State", str(arguments.get("state")));
        addIfPresent(patch, "System.Title", str(arguments.get("title")));
        addIfPresent(patch, "System.Description", str(arguments.get("description")));
        addIfPresent(patch, "System.AreaPath", str(arguments.get("area")));
        addIfPresent(patch, "System.IterationPath", str(arguments.get("iteration")));

        parseKvList(patch, str(arguments.get("add")), "add");
        parseKvList(patch, str(arguments.get("replace")), "replace");
        String remove = str(arguments.get("remove"));
        if (remove != null && !remove.isBlank()) {
            for (String f : remove.split(",")) {
                if (f == null || f.isBlank()) continue;
                patch.add(Map.of("op","remove","path","/fields/"+f.trim()));
            }
        }

        String parentId = str(arguments.get("parentId"));
        Map<String,Object> parentJson = null;
        boolean areaProvided = str(arguments.get("area")) != null && !str(arguments.get("area")).isBlank();
        boolean iterProvided = str(arguments.get("iteration")) != null && !str(arguments.get("iteration")).isBlank();
        if (parentId != null && !parentId.isBlank()) {
            if (!parentId.matches("\\d+")) return error("'parentId' debe ser numérico");
            parentJson = azureService.getWitApiWithQuery(project, null, "workitems/"+parentId, Map.of("fields","System.AreaPath,System.IterationPath"), API_VERSION);
            if (parentJson != null && parentJson.get("fields") instanceof Map) {
                Map<String,Object> fields = (Map<String,Object>) parentJson.get("fields");
                if (!areaProvided) {
                    Object pArea = fields.get("System.AreaPath"); if (pArea != null) addIfPresent(patch, "System.AreaPath", pArea.toString());
                }
                if (!iterProvided) {
                    Object pIter = fields.get("System.IterationPath"); if (pIter != null) addIfPresent(patch, "System.IterationPath", pIter.toString());
                }
            }
        }

        List<Map<String,Object>> relations = new ArrayList<>();
        if (parentJson != null) {
            Object pUrl = parentJson.get("url");
            if (pUrl != null) relations.add(Map.of("rel","System.LinkTypes.Hierarchy-Reverse","url", pUrl.toString()));
        }
        String relationsSpec = str(arguments.get("relations"));
        if (relationsSpec != null && !relationsSpec.isBlank()) {
            Set<String> dedup = new HashSet<>();
            for (String spec : relationsSpec.split(",")) {
                if (spec.isBlank()) continue;
                String[] parts = spec.split(":",3);
                if (parts.length < 2) continue;
                String relType = parts[0].trim();
                String relId = parts[1].trim();
                String comment = parts.length == 3 ? parts[2].trim() : null;
                if (!relId.matches("\\d+")) continue;
                String key = relType+"|"+relId+"|"+(comment!=null?comment:"");
                if (!dedup.add(key)) continue;
                Map<String,Object> target = azureService.getWitApiWithQuery(project, null, "workitems/"+relId, Map.of("fields","System.Id"), API_VERSION);
                Object url = target != null ? target.get("url") : null; if (url == null) continue;
                Map<String,Object> rel = new LinkedHashMap<>();
                rel.put("rel", relType);
                rel.put("url", url.toString());
                if (comment != null && !comment.isBlank()) rel.put("attributes", Map.of("comment", comment));
                relations.add(rel);
            }
        }
        if (!relations.isEmpty()) {
            for (Map<String,Object> r : relations) {
                Map<String,Object> op = new LinkedHashMap<>();
                op.put("op","add");
                op.put("path","/relations/-");
                op.put("value", r);
                patch.add(op);
            }
        }

        Map<String,String> query = new LinkedHashMap<>();
        if (getBool(arguments.get("bypassRules"))) query.put("bypassRules","true");
        if (getBool(arguments.get("suppressNotifications"))) query.put("suppressNotifications","true");
        if (getBool(arguments.get("validateOnly"))) query.put("validateOnly","true");
        String apiOverride = str(arguments.get("apiVersion"));

        Map<String,Object> resp = azureService.patchWitApiWithQuery(project, null, "workitems/"+idStr, query.isEmpty()? null : query, patch, apiOverride!=null?apiOverride:API_VERSION, MediaType.valueOf("application/json-patch+json"));
        String remoteErr = tryFormatRemoteError(resp);
        if (remoteErr != null) return success(remoteErr);
        if (getBool(arguments.get("raw"))) {
            return Map.of("isError", false, "raw", resp);
        }
        return success(format(resp));
    }

    @SuppressWarnings("unchecked")
    private String format(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Sin datos)";
        Object id = data.get("id");
        Object rev = data.get("rev");
        Map<String,Object> fields = data.get("fields") instanceof Map ? (Map<String,Object>) data.get("fields") : null;
        String title = fields!=null ? str(fields.get("System.Title")) : null;
        String state = fields!=null ? str(fields.get("System.State")) : null;
        String type = fields!=null ? str(fields.get("System.WorkItemType")) : null;
        StringBuilder sb = new StringBuilder();
        sb.append("WorkItem actualizado id=").append(id).append(" rev=").append(rev)
          .append(" | type=").append(type)
          .append(" | state=").append(state)
          .append(" | title=").append(title);
        Object url = data.get("url"); if (url != null) sb.append("\n").append(url);
        return sb.toString();
    }

    private void addIfPresent(List<Map<String,Object>> patch, String ref, String value) {
        if (value == null || value.isBlank()) return;
        Map<String,Object> op = new LinkedHashMap<>();
        op.put("op", "add");
        op.put("path", "/fields/"+ref);
        op.put("value", value);
        patch.add(op);
    }
    private void parseKvList(List<Map<String,Object>> patch, String spec, String operation) {
        if (spec == null || spec.isBlank()) return;
        for (String pair : spec.split(",")) {
            if (pair.isBlank()) continue;
            int idx = pair.indexOf('='); if (idx <= 0) continue;
            String k = pair.substring(0,idx).trim(); if (k.isEmpty()) continue;
            String v = pair.substring(idx+1).trim();
            Map<String,Object> op = new LinkedHashMap<>();
            op.put("op", operation);
            op.put("path","/fields/"+k);
            op.put("value", v);
            patch.add(op);
        }
    }
    private boolean getBool(Object o) { return o instanceof Boolean ? (Boolean) o : (o!=null && "true".equalsIgnoreCase(o.toString())); }
    private String str(Object o) { return o==null?null:o.toString(); }
}
