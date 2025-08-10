package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_work_item_create
 * Derivado de scripts/curl/wit/work_items_create.sh y api_doc/wit_sections/work_items.md (operación Create).
 * Permite crear un work item mediante operaciones JSON Patch (add) soportando atajos y banderas de query.
 * Incluye parámetros adicionales documentados pero ausentes en el script (--validateOnly).
 */
@Component
public class WorkItemCreateTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_work_item_create";
    private static final String DESC = "Crea un work item (Title obligatorio) permitiendo state, description, fields extra, herencia de padre y relaciones.";
    private static final String API_VERSION = "7.2-preview";

    @Autowired
    public WorkItemCreateTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String,Object>) base.get("properties");
        props.put("type", Map.of("type","string","description","Tipo del work item (ej: Bug, 'User Story', Épica)"));
        props.put("title", Map.of("type","string","description","Título (System.Title)"));
        props.put("state", Map.of("type","string","description","Estado inicial (System.State)"));
        props.put("description", Map.of("type","string","description","Descripción Markdown (System.Description)"));
        props.put("fields", Map.of("type","string","description","Lista adicional k=v separada por comas (referenceNames)"));
        props.put("area", Map.of("type","string","description","AreaPath (System.AreaPath)"));
        props.put("iteration", Map.of("type","string","description","IterationPath (System.IterationPath)"));
        props.put("parentId", Map.of("type","integer","description","ID de work item padre (hereda Area/Iteration si no se pasan)"));
        props.put("relations", Map.of("type","string","description","Relaciones extra tipo:id[:comentario] separadas por comas"));
        props.put("bypassRules", Map.of("type","boolean","description","Query param bypassRules=true"));
        props.put("suppressNotifications", Map.of("type","boolean","description","Query param suppressNotifications=true"));
        props.put("validateOnly", Map.of("type","boolean","description","Valida sin persistir (validateOnly=true)"));
        props.put("apiVersion", Map.of("type","string","description","Override api-version (default "+API_VERSION+")"));
        props.put("raw", Map.of("type","boolean","description","Devuelve JSON crudo de la respuesta"));
        @SuppressWarnings("unchecked") List<String> req = new ArrayList<>((List<String>) base.get("required"));
        if (!req.contains("type")) req.add("type");
        if (!req.contains("title")) req.add("title");
        base.put("required", req);
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado");
        String project = getProject(arguments);
        String type = str(arguments.get("type"));
        String title = str(arguments.get("title"));
        if (type == null || type.isBlank()) return error("'type' es requerido");
        if (title == null || title.isBlank()) return error("'title' es requerido");

        List<Map<String,Object>> patch = new ArrayList<>();
        addField(patch, "System.Title", title);
        String state = str(arguments.get("state")); if (notBlank(state)) addField(patch, "System.State", state);
        String description = str(arguments.get("description")); if (notBlank(description)) addField(patch, "System.Description", description);
        String area = str(arguments.get("area")); if (notBlank(area)) addField(patch, "System.AreaPath", area);
        String iteration = str(arguments.get("iteration")); if (notBlank(iteration)) addField(patch, "System.IterationPath", iteration);

        String fieldsExtra = str(arguments.get("fields"));
        if (notBlank(fieldsExtra)) {
            for (String pair : fieldsExtra.split(",")) {
                if (pair.isBlank()) continue;
                int idx = pair.indexOf('=');
                if (idx <= 0) continue;
                String k = pair.substring(0, idx).trim();
                String v = pair.substring(idx+1).trim();
                if (!k.isEmpty()) addField(patch, k, v);
            }
        }

        String parentIdStr = str(arguments.get("parentId"));
        Map<String,Object> parentJson = null;
        if (notBlank(parentIdStr)) {
            if (!parentIdStr.matches("\\d+")) return error("'parentId' debe ser numérico");
            parentJson = azureService.getWitApiWithQuery(project, null, "workitems/"+parentIdStr, Map.of("fields","System.AreaPath,System.IterationPath"), API_VERSION);
            if (parentJson != null && parentJson.get("fields") instanceof Map) {
                if (!notBlank(area)) {
                    Object pArea = ((Map<?,?>) parentJson.get("fields")).get("System.AreaPath");
                    if (pArea != null) addField(patch, "System.AreaPath", pArea.toString());
                }
                if (!notBlank(iteration)) {
                    Object pIter = ((Map<?,?>) parentJson.get("fields")).get("System.IterationPath");
                    if (pIter != null) addField(patch, "System.IterationPath", pIter.toString());
                }
            }
        }

        String relationsSpec = str(arguments.get("relations"));
        List<Map<String,Object>> relations = new ArrayList<>();
        if (notBlank(parentIdStr) && parentJson != null) {
            Object parentUrl = parentJson.get("url");
            if (parentUrl != null) {
                relations.add(Map.of(
                    "rel", "System.LinkTypes.Hierarchy-Reverse",
                    "url", parentUrl.toString()
                ));
            }
        }
        if (notBlank(relationsSpec)) {
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
                Object url = target != null ? target.get("url") : null;
                if (url == null) continue;
                Map<String,Object> rel = new LinkedHashMap<>();
                rel.put("rel", relType);
                rel.put("url", url.toString());
                if (notBlank(comment)) rel.put("attributes", Map.of("comment", comment));
                relations.add(rel);
            }
        }
        if (!relations.isEmpty()) {
            for (Map<String,Object> r : relations) {
                Map<String,Object> op = new LinkedHashMap<>();
                op.put("op", "add");
                op.put("path", "/relations/-");
                op.put("value", r);
                patch.add(op);
            }
        }

        Map<String,String> query = new LinkedHashMap<>();
        if (getBool(arguments.get("bypassRules"))) query.put("bypassRules","true");
        if (getBool(arguments.get("suppressNotifications"))) query.put("suppressNotifications","true");
        if (getBool(arguments.get("validateOnly"))) query.put("validateOnly","true");
        String apiOverride = str(arguments.get("apiVersion"));

        String path = "workitems/$" + type;
        Map<String,Object> resp = azureService.postWitApiWithQuery(project, null, path, query.isEmpty()? null : query, patch, apiOverride!=null?apiOverride:API_VERSION, MediaType.valueOf("application/json-patch+json"));
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
        sb.append("WorkItem creado id=").append(id).append(" rev=").append(rev)
          .append(" | type=").append(type)
          .append(" | state=").append(state)
          .append(" | title=").append(title);
        Object url = data.get("url"); if (url != null) sb.append("\n").append(url);
        return sb.toString();
    }

    private void addField(List<Map<String,Object>> patch, String ref, Object value) {
        Map<String,Object> op = new LinkedHashMap<>();
        op.put("op", "add");
        op.put("path", "/fields/"+ref);
        op.put("value", value);
        patch.add(op);
    }
    private boolean notBlank(String s) { return s != null && !s.isBlank(); }
    private boolean getBool(Object o) { return o instanceof Boolean ? (Boolean) o : (o!=null && "true".equalsIgnoreCase(o.toString())); }
    private String str(Object o) { return o==null?null:o.toString(); }
}
