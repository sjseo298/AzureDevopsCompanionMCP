package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool MCP: azuredevops_wit_work_item_types_field_get
 * Obtiene detalle de un campo de un tipo de work item.
 */
@Component
public class WorkItemTypesFieldGetTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_work_item_types_field_get";
    private static final String DESC = "Obtiene detalle (scoped) de un campo de un tipo de work item con enriquecimiento opcional (global metadata + picklist).";
    private static final String API_VERSION = "7.2-preview"; // scoped default

    @Autowired
    public WorkItemTypesFieldGetTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String,Object>) base.get("properties");
        props.put("type", Map.of("type","string","description","Nombre del tipo de work item"));
        props.put("field", Map.of("type","string","description","referenceName del campo (ej: System.Title)"));
        props.put("summary", Map.of("type","boolean","description","Vista resumida (ref,name,required,projectType,globalType,picklistCount)"));
        props.put("showPicklistItems", Map.of("type","boolean","description","Con summary incluye valores de picklist"));
        props.put("noEnrich", Map.of("type","boolean","description","Si true, no consulta metadata global ni picklist"));
        props.put("raw", Map.of("type","boolean","description","Devuelve JSON crudo del endpoint scoped (ignora enrichment)"));
        props.put("apiVersion", Map.of("type","string","description","Override api-version scoped (default "+API_VERSION+")"));
        @SuppressWarnings("unchecked") List<String> req = new ArrayList<>((List<String>) base.get("required"));
        if (!req.contains("type")) req.add("type");
        if (!req.contains("field")) req.add("field");
        base.put("required", req);
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado");
        String project = getProject(arguments);
        Object typeObj = arguments.get("type");
        Object fieldObj = arguments.get("field");
        if (typeObj == null || typeObj.toString().trim().isEmpty()) return error("'type' es requerido");
        if (fieldObj == null || fieldObj.toString().trim().isEmpty()) return error("'field' es requerido");
        String type = typeObj.toString().trim();
        String field = fieldObj.toString().trim();
        String apiOverride = strOrNull(arguments.get("apiVersion"));
        Map<String,Object> resp = azureService.getWitApiWithQuery(project, null, "workitemtypes/"+type+"/fields/"+field, null, apiOverride!=null?apiOverride:API_VERSION);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        boolean raw = getBool(arguments.get("raw"));
        boolean noEnrich = getBool(arguments.get("noEnrich"));
        boolean summary = getBool(arguments.get("summary"));
        boolean showPickItems = getBool(arguments.get("showPicklistItems"));
        if (raw) {
            return Map.of("isError", false, "raw", resp);
        }
        if (noEnrich) {
            return success(formatScoped(resp, summary));
        }
        // Enriquecimiento
        Map<String,Object> global = azureService.getCoreApi("wit/fields/"+field, Map.of("api-version","7.2-preview"));
        String gErr = tryFormatRemoteError(global);
        if (gErr != null) {
            // si error global, seguimos con scoped simple
            return success(formatScoped(resp, summary));
        }
        Object pickId = global.get("picklistId");
        Map<String,Object> pick = null;
        if (pickId != null) {
            pick = azureService.getCoreApi("work/processes/lists/"+pickId, Map.of("api-version","7.2-preview.1"));
        }
        return success(formatEnriched(resp, global, pick, summary, showPickItems));
    }

    private String formatScoped(Map<String,Object> scoped, boolean summary) {
        if (scoped == null || scoped.isEmpty()) return "(Sin datos)";
        if (summary) {
            return new StringBuilder()
                .append(scoped.get("referenceName"))
                .append(" | name=").append(scoped.get("name"))
                .append(" | required=").append(scoped.get("alwaysRequired"))
                .append(" | projectType=").append(scoped.get("type"))
                .toString();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Reference: ").append(scoped.get("referenceName"));
        sb.append("\nNombre: ").append(scoped.get("name"));
        sb.append("\nProjectType: ").append(scoped.get("type"));
        Object req = scoped.get("alwaysRequired"); if (req!=null) sb.append("\nRequired: ").append(req);
        Object ro = scoped.get("readOnly"); if (ro!=null) sb.append("\nReadOnly: ").append(ro);
        Object help = scoped.get("helpText"); if (help!=null) sb.append("\nHelp: ").append(help);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String formatEnriched(Map<String,Object> scoped, Map<String,Object> global, Map<String,Object> pick, boolean summary, boolean showPickItems) {
        if (scoped == null || scoped.isEmpty()) return "(Sin datos)";
        List<Object> pickItems = null;
        if (pick != null) {
            Object items = pick.get("items");
            if (items instanceof List) pickItems = (List<Object>) items;
        }
        if (summary) {
            StringBuilder sb = new StringBuilder();
            sb.append(scoped.get("referenceName"))
              .append(" | name=").append(scoped.get("name"))
              .append(" | required=").append(scoped.get("alwaysRequired"))
              .append(" | projectType=").append(scoped.get("type"))
              .append(" | globalType=").append(global.get("type"))
              .append(" | globalUsage=").append(global.get("usage"))
              .append(" | picklistCount=").append(pickItems!=null?pickItems.size():0);
            if (showPickItems && pickItems!=null) {
                sb.append("\nvals: ");
                int c=0; for (Object v: pickItems){ sb.append(v); if(++c>=40){ sb.append(" …"); break;} sb.append(", "); }
            }
            return sb.toString();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Reference: ").append(scoped.get("referenceName"));
        sb.append("\nNombre: ").append(scoped.get("name"));
        sb.append("\nProjectType: ").append(scoped.get("type"));
        sb.append("\nGlobalType: ").append(global.get("type"));
        sb.append("\nGlobalUsage: ").append(global.get("usage"));
        sb.append("\nReadOnly(Global): ").append(global.get("readOnly"));
        Object req = scoped.get("alwaysRequired"); if (req!=null) sb.append("\nRequired: ").append(req);
        Object pickId = global.get("picklistId"); if (pickId!=null) sb.append("\nPicklistId: ").append(pickId);
        if (pickItems != null) {
            sb.append("\nPicklistItems(").append(pickItems.size()).append("): ");
            int c=0; for (Object v: pickItems){ sb.append(v); if(++c>=60){ sb.append(" …"); break;} sb.append(", "); }
        }
        Object help = scoped.get("helpText"); if (help!=null) sb.append("\nHelp: ").append(help);
        return sb.toString();
    }

    private boolean getBool(Object o) { return o instanceof Boolean ? (Boolean) o : (o!=null && "true".equalsIgnoreCase(o.toString())); }
    private String strOrNull(Object o) { return (o==null||o.toString().isBlank())?null:o.toString(); }
}
