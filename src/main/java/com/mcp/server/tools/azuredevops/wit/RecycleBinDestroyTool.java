package com.mcp.server.tools.azuredevops.wit;


import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitRecycleBinHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_recyclebin_destroy
 * Destruye permanentemente (Destroy) un work item que está en la Recycle Bin.
 * Endpoint: DELETE /{project}/_apis/wit/recyclebin/{id}?api-version=7.2-preview
 */
@Component
public class RecycleBinDestroyTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_recyclebin_destroy";
    private static final String DESC = "Elimina permanentemente un work item eliminado. Si retorna 404, se informa que no hay permisos y solo se puede soft delete.";

    private final WitRecycleBinHelper helper;

    public RecycleBinDestroyTool(AzureDevOpsClientService svc) {
        super(svc);
        this.helper = new WitRecycleBinHelper(svc);
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String,Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String,Object>) base.get("properties");
        props.put("id", Map.of("type","integer","description","ID del work item en Recycle Bin"));
    @SuppressWarnings("unchecked") List<String> originalReq = (List<String>) base.get("required");
    List<String> req = new ArrayList<>(originalReq); // hacer mutable (List.of es inmutable)
    if (!req.contains("id")) req.add("id");
    base.put("required", req);
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
        Map<String,Object> resp = helper.destroy(project, id);
        return success(helper.formatDestroyResponse(resp));
    }
}
