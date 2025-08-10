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
 * Tool MCP: azuredevops_wit_work_item_types_field_list
 * Lista campos de un tipo de work item.
 */
@Component
public class WorkItemTypesFieldListTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_work_item_types_field_list";
    private static final String DESC = "Lista campos de un tipo de work item con filtros (--required-only, filtro por reference/name) y resumen.";
    private static final String API_VERSION = "7.2-preview";

    @Autowired
    public WorkItemTypesFieldListTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String,Object>) base.get("properties");
    props.put("type", Map.of("type","string","description","Nombre del tipo de work item (ej: Bug, 'User Story')"));
    props.put("requiredOnly", Map.of("type","boolean","description","Si true, solo campos alwaysRequired=true"));
    props.put("filterRef", Map.of("type","string","description","Substring (case-insensitive) a buscar en referenceName"));
    props.put("filterName", Map.of("type","string","description","Substring (case-insensitive) a buscar en name"));
    props.put("summary", Map.of("type","boolean","description","Si true, devuelve lista resumida (ref, name, required, helpText<=90)") );
    props.put("showPicklistItems", Map.of("type","boolean","description","Incluir valores de picklist en salida (puede ser largo)"));
    props.put("raw", Map.of("type","boolean","description","Si true y sin filtros/flags devuelve JSON crudo del endpoint base"));
    props.put("apiVersion", Map.of("type","string","description","Override api-version para la llamada base (default "+API_VERSION+")"));
        @SuppressWarnings("unchecked") List<String> req = new ArrayList<>((List<String>) base.get("required"));
        if (!req.contains("type")) req.add("type");
        base.put("required", req);
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado");
        String project = getProject(arguments);
        Object typeObj = arguments.get("type");
        if (typeObj == null || typeObj.toString().trim().isEmpty()) return error("'type' es requerido");
        String type = typeObj.toString().trim();
        String apiOverride = strOrNull(arguments.get("apiVersion"));
        Map<String,Object> resp = azureService.getWitApiWithQuery(project, null, "workitemtypes/"+type+"/fields", null, apiOverride!=null?apiOverride:API_VERSION);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        // Condición raw similar al script: solo si no hay filtros / flags adicionales
        boolean requiredOnly = getBool(arguments.get("requiredOnly"));
        boolean hasFilter = strOrNull(arguments.get("filterRef"))!=null || strOrNull(arguments.get("filterName"))!=null;
        boolean summary = getBool(arguments.get("summary"));
        boolean showPickItems = getBool(arguments.get("showPicklistItems"));
        boolean raw = getBool(arguments.get("raw"));
        if (raw && !requiredOnly && !hasFilter && !summary && !showPickItems) {
            // devolver crudo
            return Map.of("isError", false, "raw", resp);
        }
        return success(format(resp, arguments));
    }

    @SuppressWarnings("unchecked")
    private String format(Map<String,Object> data, Map<String,Object> args) {
        if (data == null || data.isEmpty()) return "(Sin datos)";
        Object val = data.get("value");
        if (!(val instanceof List)) return data.toString();
        List<?> list = (List<?>) val;
        if (list.isEmpty()) return "(Sin resultados)";
        boolean requiredOnly = getBool(args.get("requiredOnly"));
        String filterRef = strOrNull(args.get("filterRef"));
        String filterName = strOrNull(args.get("filterName"));
        boolean summary = getBool(args.get("summary"));
        boolean showPickItems = getBool(args.get("showPicklistItems"));
        // Enriquecimiento: global field metadata + picklists
        List<Map<String,Object>> enriched = new ArrayList<>();
        Map<String,Map<String,Object>> cacheGlobal = new LinkedHashMap<>();
        Map<String,Map<String,Object>> cachePick = new LinkedHashMap<>();
        List<Map<String,Object>> filtered = new ArrayList<>();
        for (Object o : list) {
            if (!(o instanceof Map)) continue;
            Map<String,Object> m = (Map<String,Object>) o;
            if (requiredOnly && !Boolean.TRUE.equals(m.get("alwaysRequired"))) continue;
            String ref = String.valueOf(m.getOrDefault("referenceName",""));
            String name = String.valueOf(m.getOrDefault("name",""));
            if (filterRef != null && !ref.toLowerCase().contains(filterRef.toLowerCase())) continue;
            if (filterName != null && !name.toLowerCase().contains(filterName.toLowerCase())) continue;
            // Enriquecer
            Map<String,Object> copy = new LinkedHashMap<>(m);
            Map<String,Object> g = cacheGlobal.computeIfAbsent(ref, r -> azureService.getCoreApi("wit/fields/"+r, Map.of("api-version","7.2-preview")));
            if (g != null && !g.isEmpty()) {
                copy.put("globalType", g.get("type"));
                copy.put("usage", g.get("usage"));
                copy.put("readOnly", g.get("readOnly"));
                Object picklistId = g.get("picklistId");
                if (picklistId != null) copy.put("picklistId", picklistId);
                if (picklistId != null && picklistId.toString().length()>0) {
                    Map<String,Object> pk = cachePick.computeIfAbsent(picklistId.toString(), pid -> azureService.getCoreApi("work/processes/lists/"+pid, Map.of("api-version","7.2-preview.1")));
                    if (pk != null && !pk.isEmpty()) {
                        Object items = pk.get("items");
                        if (items instanceof List) {
                            copy.put("picklistCount", ((List<?>) items).size());
                            if (showPickItems) copy.put("picklistItems", items);
                        }
                        copy.put("picklistName", pk.get("name"));
                        copy.put("picklistType", pk.get("type"));
                    }
                }
            }
            filtered.add(copy);
        }
        List<Map<String,Object>> target = filtered.isEmpty() ? (List<Map<String,Object>>) (requiredOnly || filterRef!=null || filterName!=null ? filtered : filtered) : filtered; // siempre enriquecidos
        if (summary) {
            StringBuilder sb = new StringBuilder();
            sb.append("Campos ("+target.size()+")\n");
            int i=1;
            for (Map<String,Object> m : target) {
                String ref = String.valueOf(m.getOrDefault("referenceName","?"));
                String nm = String.valueOf(m.getOrDefault("name","?"));
                boolean req = Boolean.TRUE.equals(m.get("alwaysRequired"));
                String help = String.valueOf(m.getOrDefault("helpText",""));
                if (help.length()>90) help = help.substring(0,90)+"…";
                sb.append(i++).append(") ").append(ref).append(" | ").append(nm).append(" | required=").append(req);
                Object gType = m.get("globalType");
                if (gType!=null) sb.append(" | gType=").append(gType);
                Object pkCount = m.get("picklistCount");
                if (pkCount!=null) sb.append(" | picklistItems=").append(pkCount);
                if (!help.isEmpty()) sb.append(" | ").append(help);
                if (showPickItems) {
                    Object items = m.get("picklistItems");
                    if (items instanceof List) {
                        sb.append("\n    vals: ");
                        int c=0; for (Object it : (List<?>) items) { sb.append(it); if (++c>=30) { sb.append(" …"); break;} sb.append(", "); }
                    }
                }
                sb.append('\n');
                if (i>100) { sb.append("... ("+(target.size()-100)+" más)\n"); break; }
            }
            return sb.toString();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Campos ("+target.size()+"):\n");
        int i=1;
        for (Object o : target) {
            if (o instanceof Map) {
                Map<String,Object> m = (Map<String,Object>) o;
                sb.append(i++).append(") ")
                  .append(m.getOrDefault("referenceName","?"))
                  .append(" | name=").append(m.getOrDefault("name","?"))
                  .append(" | required=").append(m.getOrDefault("alwaysRequired", false));
                Object gType = m.get("globalType"); if (gType!=null) sb.append(" | gType=").append(gType);
                Object usage = m.get("usage"); if (usage!=null) sb.append(" | usage=").append(usage);
                Object ro = m.get("readOnly"); if (ro!=null) sb.append(" | readOnly=").append(ro);
                Object pkId = m.get("picklistId"); if (pkId!=null) sb.append(" | picklistId=").append(pkId);
                Object pkCount = m.get("picklistCount"); if (pkCount!=null) sb.append(" | picklistItems=").append(pkCount);
                if (showPickItems) {
                    Object items = m.get("picklistItems");
                    if (items instanceof List) {
                        sb.append("\n    vals: ");
                        int c=0; for (Object it : (List<?>) items) { sb.append(it); if (++c>=50) { sb.append(" …"); break;} sb.append(", "); }
                    }
                }
                sb.append('\n');
                if (i>100) { sb.append("... ("+(target.size()-100)+" más)\n"); break; }
            }
        }
        return sb.toString();
    }

    private boolean getBool(Object o) { return o instanceof Boolean ? (Boolean) o : (o!=null && "true".equalsIgnoreCase(o.toString())); }
    private String strOrNull(Object o) { return (o==null||o.toString().isBlank())?null:o.toString(); }
}
