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
    private static final String DESC = "Lista campos de un tipo de work item (referenceName, name, required).";
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
        Map<String,Object> resp = azureService.getWitApiWithQuery(project, null, "workitemtypes/"+type+"/fields", null, API_VERSION);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        return success(format(resp));
    }

    @SuppressWarnings("unchecked")
    private String format(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Sin datos)";
        Object val = data.get("value");
        if (!(val instanceof List)) return data.toString();
        List<?> list = (List<?>) val;
        if (list.isEmpty()) return "(Sin resultados)";
        StringBuilder sb = new StringBuilder();
        sb.append("Campos ("+list.size()+"):\n");
        int i=1;
        for (Object o : list) {
            if (o instanceof Map) {
                Map<String,Object> m = (Map<String,Object>) o;
                sb.append(i++).append(") ")
                  .append(m.getOrDefault("referenceName","?"))
                  .append(" | name=").append(m.getOrDefault("name","?"))
                  .append(" | required=").append(m.getOrDefault("alwaysRequired", m.getOrDefault("readOnly", false)))
                  .append('\n');
                if (i>50) { sb.append("... ("+(list.size()-50)+" más)\n"); break; }
            }
        }
        return sb.toString();
    }
}
