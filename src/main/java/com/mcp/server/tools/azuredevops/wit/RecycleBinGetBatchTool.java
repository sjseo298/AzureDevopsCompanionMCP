package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitRecycleBinHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tool MCP: azuredevops_wit_recyclebin_get_batch
 * Obtiene varios work items eliminados por lista de IDs.
 * Endpoint: GET /{project}/_apis/wit/recyclebin?ids=1,2,3&api-version=7.2-preview
 */
public class RecycleBinGetBatchTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_recyclebin_get_batch";
    private static final String DESC = "Obtiene varios work items eliminados (Recycle Bin) por IDs.";

    private final WitRecycleBinHelper helper;

    public RecycleBinGetBatchTool(AzureDevOpsClientService svc) {
        super(svc);
        this.helper = new WitRecycleBinHelper(svc);
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String,Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String,Object>) base.get("properties");
        props.put("ids", Map.of("type","string","description","Lista de IDs separados por coma"));
        @SuppressWarnings("unchecked") List<String> req = (List<String>) base.get("required");
        if (!req.contains("ids")) req.add("ids");
        return base;
    }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        super.validateCommon(args);
        helper.validateIds(args.get("ids"));
    }

    @Override
    protected Map<String,Object> executeInternal(Map<String,Object> args) {
        String project = getProject(args);
        Object ids = args.get("ids");
        Map<String,Object> resp = helper.getBatch(project, ids);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        return success(helper.formatGetBatchResponse(resp));
    }
}
