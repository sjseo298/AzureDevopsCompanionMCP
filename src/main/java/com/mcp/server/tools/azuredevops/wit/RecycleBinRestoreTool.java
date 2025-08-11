package com.mcp.server.tools.azuredevops.wit;


import com.mcp.server.services.AzureDevOpsClientService;

import com.mcp.server.services.helpers.WitRecycleBinHelper;
import org.springframework.stereotype.Component;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_recyclebin_restore
 * Restaura un work item desde la Recycle Bin.
 * Endpoint: PATCH /{project}/_apis/wit/recyclebin/{id}?api-version=7.2-preview
 */
@Component
public class RecycleBinRestoreTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_recyclebin_restore";
    private static final String DESC = "Restaura un work item desde la Recycle Bin.";

    private final WitRecycleBinHelper helper;

    public RecycleBinRestoreTool(AzureDevOpsClientService svc) {
        super(svc);
        this.helper = new WitRecycleBinHelper(svc);
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String,Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String,Object>) base.get("properties");
        props.put("id", Map.of("type","integer","description","ID del work item eliminado"));
        @SuppressWarnings("unchecked") List<String> req = (List<String>) base.get("required");
        if (!req.contains("id")) req.add("id");
        return base;
    }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        super.validateCommon(args);
        helper.validateId(args.get("id"));
    }

    @Override
    protected Map<String,Object> executeInternal(Map<String,Object> args) {
        String project = getProject(args);
        Object id = args.get("id");
        Map<String,Object> resp = helper.restore(project, id);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        return success(helper.formatRestoreResponse(resp));
    }
}
