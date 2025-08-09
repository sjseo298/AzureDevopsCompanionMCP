package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Tool MCP: azuredevops_wit_work_item_types_list
 * Lista tipos de work item de un proyecto.
 */
@Component
public class WorkItemTypesListTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_work_item_types_list";
    private static final String DESC = "Lista los tipos de work item (name, referenceName, color, disabled).";
    private static final String API_VERSION = "7.2-preview";

    @Autowired
    public WorkItemTypesListTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado");
        String project = getProject(arguments);
        Map<String,Object> resp = azureService.getWitApiWithQuery(project, null, "workitemtypes", null, API_VERSION);
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
        sb.append("Tipos ("+list.size()+"):\n");
        int i=1;
        for (Object o : list) {
            if (o instanceof Map) {
                Map<String,Object> m = (Map<String,Object>) o;
                sb.append(i++).append(") ")
                  .append(m.getOrDefault("name","?"))
                  .append(" | ref=").append(m.getOrDefault("referenceName","?"))
                  .append(" | color=").append(m.getOrDefault("color","-"))
                  .append(" | disabled=").append(m.getOrDefault("isDisabled", false))
                  .append('\n');
                if (i>30) { sb.append("... ("+(list.size()-30)+" más)\n"); break; }
            }
        }
        return sb.toString();
    }
}
