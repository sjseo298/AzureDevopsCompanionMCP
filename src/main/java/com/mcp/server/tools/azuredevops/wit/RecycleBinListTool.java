package com.mcp.server.tools.azuredevops.wit;


import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitRecycleBinHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_recyclebin_list
 * Lista referencias (shallow) de work items eliminados.
 * Endpoint: GET /{project}/_apis/wit/recyclebin?api-version=7.2-preview
 */
@Component
public class RecycleBinListTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_recyclebin_list";
    private static final String DESC = "Lista work items eliminados (shallow). Si la organización excede límite, muestra mensaje con recomendación de filtrar por id.";

    private final WitRecycleBinHelper helper;

    public RecycleBinListTool(AzureDevOpsClientService svc) {
        super(svc);
        this.helper = new WitRecycleBinHelper(svc);
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    protected Map<String,Object> executeInternal(Map<String,Object> args) {
        String project = getProject(args);
        Map<String,Object> resp = helper.list(project);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        return success(helper.formatListResponse(resp));
    }
}
