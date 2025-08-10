package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_work_item_get
 * Derivado de scripts/curl/wit/work_items_get.sh y api_doc/wit_sections/work_items.md (operación Get Work Item).
 * Permite recuperar un work item por ID con filtros opcionales (fields, $expand, asOf) y modo raw.
 */
@Component
public class WorkItemGetTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_work_item_get";
    private static final String DESC = "Obtiene un work item por ID (campos clave y conteos de relaciones/links) con soporte de filtros y modo raw.";
    private static final String API_VERSION = "7.2-preview";

    @Autowired
    public WorkItemGetTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String,Object>) base.get("properties");
        props.put("id", Map.of("type","integer","description","ID numérico del work item"));
        props.put("fields", Map.of("type","string","description","Lista separada por comas de referenceNames a devolver"));
        props.put("expand", Map.of("type","string","description","None|Relations|Links|All (se envía como $expand)"));
        props.put("asOf", Map.of("type","string","description","Fecha/hora ISO para estado histórico"));
        props.put("apiVersion", Map.of("type","string","description","Override api-version (default "+API_VERSION+")"));
        props.put("raw", Map.of("type","boolean","description","Devuelve JSON crudo del endpoint"));
        @SuppressWarnings("unchecked") List<String> req = new ArrayList<>((List<String>) base.get("required"));
        if (!req.contains("id")) req.add("id");
        base.put("required", req);
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado");
        String project = getProject(arguments);
        Object idObj = arguments.get("id");
        if (idObj == null) return error("'id' es requerido");
        String idStr = idObj.toString().trim();
        if (idStr.isEmpty()) return error("'id' es requerido");
        // Validar que parezca entero
        if (!idStr.matches("\\d+")) return error("'id' debe ser numérico");

        Map<String,String> query = new LinkedHashMap<>();
        String fields = str(arguments.get("fields"));
        if (fields != null && !fields.isBlank()) query.put("fields", fields);
        String expand = str(arguments.get("expand"));
        if (expand != null && !expand.isBlank()) query.put("$expand", expand);
        String asOf = str(arguments.get("asOf"));
        if (asOf != null && !asOf.isBlank()) query.put("asOf", asOf);
        String apiOverride = str(arguments.get("apiVersion"));

        Map<String,Object> resp = azureService.getWitApiWithQuery(project, null, "workitems/"+idStr, query.isEmpty()?null:query, apiOverride!=null?apiOverride:API_VERSION);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
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
        Map<String,Object> fields = null;
        Object fObj = data.get("fields");
        if (fObj instanceof Map) fields = (Map<String,Object>) fObj;
        String title = fields != null ? str(fields.get("System.Title")) : null;
        String state = fields != null ? str(fields.get("System.State")) : null;
        String wtype = fields != null ? str(fields.get("System.WorkItemType")) : null;
        String assigned = null;
        Object assignedObj = fields != null ? fields.get("System.AssignedTo") : null;
        if (assignedObj instanceof Map) {
            Object disp = ((Map<String,Object>) assignedObj).get("displayName");
            if (disp != null) assigned = disp.toString();
        } else if (assignedObj != null) {
            assigned = assignedObj.toString();
        }
        int relCount = 0; int linkCount = 0;
        Object rels = data.get("relations");
        if (rels instanceof List) relCount = ((List<?>) rels).size();
        Object links = data.get("_links");
        if (links instanceof Map) linkCount = ((Map<?,?>) links).size();
        StringBuilder sb = new StringBuilder();
        sb.append("#").append(id).append(" rev=").append(rev)
          .append(" | type=").append(wtype)
          .append(" | state=").append(state)
          .append(" | title=").append(title);
        if (assigned != null) sb.append(" | assigned=").append(assigned);
        if (relCount>0) sb.append(" | relations=").append(relCount);
        if (linkCount>0) sb.append(" | links=").append(linkCount);
        // Fecha de cambio
        String changed = fields != null ? str(fields.get("System.ChangedDate")) : null;
        if (changed != null) sb.append("\nChanged: ").append(changed);
        // Mostrar un subset de campos adicionales si pocos campos filtrados (heurística simple)
        if (fields != null && fields.size() <= 12) {
            sb.append("\nCampos:");
            int c=0; for (Map.Entry<String,Object> e : fields.entrySet()) {
                String k = e.getKey(); if (k.startsWith("System.")) continue; // omitir system para no saturar
                sb.append("\n - ").append(k).append(" = ").append(shortVal(e.getValue()));
                if (++c>=40) break;
            }
        }
        return sb.toString();
    }

    private String shortVal(Object v) { if (v==null) return "null"; String s=v.toString(); return s.length()>160? s.substring(0,157)+"…" : s; }
    private boolean getBool(Object o) { return o instanceof Boolean ? (Boolean) o : (o!=null && "true".equalsIgnoreCase(o.toString())); }
    private String str(Object o) { return o==null?null:o.toString(); }
}
