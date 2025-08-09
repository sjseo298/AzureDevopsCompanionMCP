package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_recyclebin_get
 * Obtiene un work item eliminado (papelera) por ID.
 * Endpoint: GET /{project}/_apis/wit/recyclebin/{id}?api-version=7.2-preview
 */
public class RecycleBinGetTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_recyclebin_get";
    private static final String DESC = "Obtiene un work item eliminado (Recycle Bin) por ID.";

    public RecycleBinGetTool(AzureDevOpsClientService svc) { super(svc); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
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
        if (args.get("id") == null) throw new IllegalArgumentException("'id' es requerido");
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        String project = getProject(arguments);
        String id = arguments.get("id").toString().trim();
        Map<String,Object> resp = azureService.getWitApi(project,null,"recyclebin/"+id);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        StringBuilder sb = new StringBuilder();
        sb.append("RecycleBin Item ID=").append(resp.get("id"));
        Object name = resp.get("name");
        if (name != null) sb.append(" | name=").append(name);
        Object deletedDate = resp.get("deletedDate");
        if (deletedDate != null) sb.append(" | deletedDate=").append(deletedDate);
        return success(sb.toString());
    }
}
