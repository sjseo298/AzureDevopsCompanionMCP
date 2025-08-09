package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Tool MCP: azuredevops_wit_work_item_type_categories_list
 * Lista categorías de tipos de work item para un proyecto.
 */
@Component
public class WorkItemTypeCategoriesListTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_work_item_type_categories_list";
    private static final String DESC = "Lista categorías de tipos de work item (nombre, defaultWorkItemType, número de tipos).";
    private static final String API_VERSION = "7.2-preview";

    @Autowired
    public WorkItemTypeCategoriesListTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado");
        String project = getProject(arguments);
        Map<String,Object> resp = azureService.getWitApiWithQuery(project, null, "workitemtypecategories", null, API_VERSION);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        return success(format(resp));
    }

    @SuppressWarnings("unchecked")
    private String format(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Sin resultados)";
        Object val = data.get("value");
        if (!(val instanceof List)) return data.toString();
        List<?> list = (List<?>) val;
        if (list.isEmpty()) return "(Sin resultados)";
        StringBuilder sb = new StringBuilder();
        sb.append("Categorías ("+list.size()+"):\n");
        int i=1;
        for (Object o : list) {
            if (o instanceof Map) {
                Map<String,Object> m = (Map<String,Object>) o;
                Map<String,Object> dft = (Map<String,Object>) m.get("defaultWorkItemType");
                String dftName = dft != null ? String.valueOf(dft.get("name")) : "-";
                List<?> witTypes = (List<?>) m.get("workItemTypes");
                int total = witTypes != null ? witTypes.size() : 0;
                sb.append(i++).append(") ")
                  .append(m.getOrDefault("referenceName","?"))
                  .append(" | display=").append(m.getOrDefault("name","?"))
                  .append(" | default=").append(dftName)
                  .append(" | count=").append(total)
                  .append('\n');
                if (i>30) { sb.append("... ("+(list.size()-30)+" más)\n"); break; }
            }
        }
        return sb.toString();
    }
}
