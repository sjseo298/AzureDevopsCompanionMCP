package com.mcp.server.tools.azuredevops.wit;


import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitRecycleBinHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_recyclebin_get
 * Obtiene un work item eliminado (papelera) por ID.
 * Endpoint: GET /{project}/_apis/wit/recyclebin/{id}?api-version=7.2-preview
 */
@Component
public class RecycleBinGetTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_recyclebin_get";
    private static final String DESC = "Obtiene un work item eliminado (Recycle Bin) por ID.";

    private final WitRecycleBinHelper helper;

    public RecycleBinGetTool(AzureDevOpsClientService svc) {
        super(svc);
        this.helper = new WitRecycleBinHelper(svc);
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String,Object>) base.get("properties");
        props.put("id", Map.of("type","integer","description","ID del work item eliminado"));
        @SuppressWarnings("unchecked") List<String> req = (List<String>) base.get("required");
        // Crear copia mutable si es inmutable
        if (!(req instanceof ArrayList)) {
            req = new ArrayList<>(req);
            base.put("required", req);
        }
        if (!req.contains("id")) req.add("id");
        return base;
    }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        super.validateCommon(args);
        helper.validateId(args.get("id"));
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        String project = getProject(arguments);
        Object id = arguments.get("id");
        Map<String,Object> resp = helper.get(project, id);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        return success(helper.formatGetResponse(resp));
    }
}
