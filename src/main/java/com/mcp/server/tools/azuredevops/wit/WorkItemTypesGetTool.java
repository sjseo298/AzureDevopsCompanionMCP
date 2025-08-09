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
 * Tool MCP: azuredevops_wit_work_item_types_get
 * Obtiene detalle de un tipo de work item.
 */
@Component
public class WorkItemTypesGetTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_work_item_types_get";
    private static final String DESC = "Obtiene detalle de un tipo de work item (name, referenceName, description, color, icon, fields).";
    private static final String API_VERSION = "7.2-preview";

    @Autowired
    public WorkItemTypesGetTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String,Object>) base.get("properties");
        props.put("type", Map.of("type","string","description","Nombre del tipo de work item (ej: 'User Story')"));
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
        Map<String,Object> resp = azureService.getWitApiWithQuery(project, null, "workitemtypes/"+type, null, API_VERSION);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        return success(format(resp));
    }

    @SuppressWarnings("unchecked")
    private String format(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Sin datos)";
        StringBuilder sb = new StringBuilder();
        sb.append(data.getOrDefault("name","?"))
          .append(" (ref=").append(data.getOrDefault("referenceName","?"))
          .append(") color=").append(data.getOrDefault("color","-"))
          .append(" disabled=").append(data.getOrDefault("isDisabled", false));
        Object desc = data.get("description");
        if (desc != null) sb.append("\nDesc: ").append(desc);
        Object icon = data.get("icon");
        if (icon instanceof Map) {
            Map<String,Object> ic = (Map<String,Object>) icon;
            sb.append("\nIcon: id=").append(ic.get("id")).append(" url=").append(ic.get("url"));
        }
        Object fields = data.get("fields");
        if (fields instanceof List) {
            List<?> list = (List<?>) fields;
            sb.append("\nFields ("+list.size()+"): ");
            int c=0; StringBuilder line = new StringBuilder();
            for (Object o : list) {
                if (o instanceof Map) {
                    Map<String,Object> m = (Map<String,Object>) o;
                    if (line.length()>0) line.append(", ");
                    line.append(m.getOrDefault("referenceName", m.get("name"))); c++;
                    if (c>=25) { line.append(", ..."); break; }
                }
            }
            sb.append(line);
        }
        return sb.toString();
    }
}
