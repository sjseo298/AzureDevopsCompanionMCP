package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_work_item_icons_list
 * Lista íconos de work item (GET _apis/wit/workitemicons).
 */
@Component
public class WorkItemIconsListTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_work_item_icons_list";
    private static final String DESC = "Lista los íconos de work item disponibles en la organización.";
    private static final String API_VERSION = "7.2-preview.1";

    @Autowired
    public WorkItemIconsListTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        // Reutilizamos el schema base (project requerido por consistencia general)
        return createBaseSchema();
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado");
        Map<String,Object> resp = azureService.getWitApiWithQuery(null,null, "workitemicons", null, API_VERSION);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        return success(format(resp));
    }

    private String format(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Sin resultados)";
        Object val = data.get("value");
        if (val instanceof List) {
            List<?> list = (List<?>) val;
            if (list.isEmpty()) return "(Sin resultados)";
            StringBuilder sb = new StringBuilder();
            sb.append("Íconos ("+list.size()+"):\n");
            int i=1;
            for (Object o : list) {
                if (o instanceof Map) {
                    Map<?,?> m = (Map<?,?>) o;
                    Object id = m.get("id");
                    Object upd = m.get("updatedTime");
                    sb.append(i++).append(") ")
                      .append(id != null ? id : "?")
                      .append(" | updated=")
                      .append(upd != null ? upd : "-")
                      .append('\n');
                    if (i>25) { sb.append("... ("+(list.size()-25)+" más)\n"); break; }
                }
            }
            return sb.toString();
        }
        return data.toString();
    }
}
