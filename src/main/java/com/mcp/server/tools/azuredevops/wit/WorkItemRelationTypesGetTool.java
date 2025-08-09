package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tool MCP: azuredevops_wit_work_item_relation_types_get
 * Obtiene un tipo de relación específico (GET _apis/wit/workitemrelationtypes/{referenceName}).
 */
@Component
public class WorkItemRelationTypesGetTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_work_item_relation_types_get";
    private static final String DESC = "Obtiene un tipo de relación de work item por referenceName.";
    private static final String API_VERSION = "7.2-preview";

    @Autowired
    public WorkItemRelationTypesGetTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new java.util.LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String,Object>) base.get("properties");
        props.put("reference", Map.of("type","string","description","referenceName del tipo de relación (ej: System.LinkTypes.Hierarchy-Forward)"));
        @SuppressWarnings("unchecked") List<String> existing = (List<String>) base.get("required");
        List<String> req = new ArrayList<>(existing);
        if (!req.contains("reference")) req.add("reference");
        base.put("required", req);
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado");
        Object refObj = arguments.get("reference");
        if (refObj == null || refObj.toString().trim().isEmpty()) return error("'reference' es requerido");
        String reference = refObj.toString().trim();
        Map<String,Object> resp = azureService.getWitApiWithQuery(null, null, "workitemrelationtypes/"+reference, null, API_VERSION);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        return success(format(resp));
    }

    @SuppressWarnings("unchecked")
    private String format(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Sin datos)";
        StringBuilder sb = new StringBuilder();
        sb.append("Reference: ").append(data.get("referenceName"));
        sb.append("\nName: ").append(data.get("name"));
        Map<String,Object> attrs = (Map<String,Object>) data.get("attributes");
        if (attrs != null) {
            sb.append("\nAttrs: direction=").append(attrs.get("direction"))
              .append(", topology=").append(attrs.get("topology"))
              .append(", usage=").append(attrs.get("usage"));
        }
        Object url = data.get("url");
        if (url != null) sb.append("\nURL: ").append(url);
        return sb.toString();
    }
}
