package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tool MCP: azuredevops_wit_work_item_type_categories_get
 * Obtiene una categoría de tipos de work item por referenceName.
 */
@Component
public class WorkItemTypeCategoriesGetTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_work_item_type_categories_get";
    private static final String DESC = "Obtiene una categoría de tipos de work item (referenceName, nombre y tipos asociados).";
    private static final String API_VERSION = "7.2-preview";

    @Autowired
    public WorkItemTypeCategoriesGetTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new java.util.LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String,Object>) base.get("properties");
        props.put("category", Map.of("type","string","description","referenceName de la categoría (ej: Microsoft.RequirementCategory)"));
        @SuppressWarnings("unchecked") List<String> existing = (List<String>) base.get("required");
        List<String> req = new ArrayList<>(existing);
        if (!req.contains("category")) req.add("category");
        base.put("required", req);
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado");
        String project = getProject(arguments);
        Object catObj = arguments.get("category");
        if (catObj == null || catObj.toString().trim().isEmpty()) return error("'category' es requerido");
        String category = catObj.toString().trim();
        Map<String,Object> resp = azureService.getWitApiWithQuery(project, null, "workitemtypecategories/"+category, null, API_VERSION);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        return success(format(resp));
    }

    @SuppressWarnings("unchecked")
    private String format(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Sin datos)";
        StringBuilder sb = new StringBuilder();
        sb.append("Reference: ").append(data.get("referenceName"));
        sb.append("\nNombre: ").append(data.get("name"));
        Map<String,Object> dft = (Map<String,Object>) data.get("defaultWorkItemType");
        if (dft != null) sb.append("\nDefault: ").append(dft.get("name"));
        List<Map<String,Object>> wits = (List<Map<String,Object>>) data.get("workItemTypes");
        if (wits != null && !wits.isEmpty()) {
            sb.append("\nTipos ("+wits.size()+"): ");
            int i=0; for (Map<String,Object> w : wits) { if (i++>0) sb.append(", "); sb.append(w.get("name")); }
        }
        return sb.toString();
    }
}
