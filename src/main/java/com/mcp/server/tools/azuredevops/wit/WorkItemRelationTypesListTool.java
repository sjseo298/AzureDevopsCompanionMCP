package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Tool MCP: azuredevops_wit_work_item_relation_types_list
 * Lista tipos de relación entre work items (GET _apis/wit/workitemrelationtypes).
 */
@Component
public class WorkItemRelationTypesListTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_work_item_relation_types_list";
    private static final String DESC = "Lista los tipos de relación entre work items (referenceName, name, direction, topology).";
    private static final String API_VERSION = "7.2-preview";

    @Autowired
    public WorkItemRelationTypesListTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado");
        Map<String,Object> resp = azureService.getWitApiWithQuery(null, null, "workitemrelationtypes", null, API_VERSION);
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
        sb.append("Relation Types ("+list.size()+"):\n");
        int i = 1;
        for (Object o : list) {
            if (o instanceof Map) {
                Map<String,Object> m = (Map<String,Object>) o;
                Map<String,Object> attrs = (Map<String,Object>) m.get("attributes");
                Object direction = attrs != null ? attrs.get("direction") : null;
                Object topology = attrs != null ? attrs.get("topology") : null;
                sb.append(i++).append(") ")
                  .append(m.getOrDefault("referenceName","?"))
                  .append(" | name=").append(m.getOrDefault("name","?"))
                  .append(" | dir=").append(direction != null ? direction : "-")
                  .append(" | topo=").append(topology != null ? topology : "-")
                  .append('\n');
                if (i>30) { sb.append("... ("+(list.size()-30)+" más)\n"); break; }
            }
        }
        return sb.toString();
    }
}
