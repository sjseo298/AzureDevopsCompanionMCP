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
    private static final String DESC = "Obtiene detalle de un campo de un tipo de work item (referenceName, name, type, required, readOnly).";
    private static final String API_VERSION = "7.2-preview";

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
        Map<String,Object> resp = azureService.getWitApiWithQuery(project, null, "workitemtypes/"+type+"/fields/"+field, null, API_VERSION);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        return success(format(resp));
    }

    private String format(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Sin datos)";
        StringBuilder sb = new StringBuilder();
        sb.append("Reference: ").append(data.get("referenceName"));
        sb.append("\nNombre: ").append(data.get("name"));
        sb.append("\nTipo: ").append(data.get("type"));
        Object req = data.get("alwaysRequired");
        if (req != null) sb.append("\nRequired: ").append(req);
        Object ro = data.get("readOnly");
        if (ro != null) sb.append("\nReadOnly: ").append(ro);
        Object help = data.get("helpText");
        if (help != null) sb.append("\nHelp: ").append(help);
        return sb.toString();
    }
}
