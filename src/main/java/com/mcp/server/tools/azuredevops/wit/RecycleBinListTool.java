package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_recyclebin_list
 * Lista referencias (shallow) de work items eliminados.
 * Endpoint: GET /{project}/_apis/wit/recyclebin?api-version=7.2-preview
 */
public class RecycleBinListTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_recyclebin_list";
    private static final String DESC = "Lista work items eliminados (shallow). Si la organización excede límite, muestra mensaje con recomendación de filtrar por id.";

    public RecycleBinListTool(AzureDevOpsClientService svc) { super(svc); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    protected Map<String,Object> executeInternal(Map<String,Object> args) {
        String project = getProject(args);
        Map<String,Object> resp = azureService.getWitApi(project,null,"recyclebin");
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        Object count = resp.get("count");
        @SuppressWarnings("unchecked") List<Map<String,Object>> value = (List<Map<String,Object>>) resp.get("value");
        if (value == null) {
            return success("Sin datos o límite excedido. Use azuredevops_wit_recyclebin_get para IDs específicos.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Items eliminados: ").append(count != null ? count : value.size()).append('\n');
        int i = 1;
        for (Map<String,Object> v : value) {
            sb.append(i++).append(") ID=").append(v.get("id"));
            Object url = v.get("url"); if (url != null) sb.append(" | url=").append(url);
            sb.append('\n');
        }
        return success(sb.toString());
    }
}
