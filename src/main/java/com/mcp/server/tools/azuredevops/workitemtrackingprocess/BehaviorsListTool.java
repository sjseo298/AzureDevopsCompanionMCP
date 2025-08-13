package com.mcp.server.tools.azuredevops.workitemtrackingprocess;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WorkitemtrackingprocessBehaviorsHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_workitemtrackingprocess_behaviors_list
 * Lista comportamientos (behaviors) de un proceso.
 */
@Component
public class BehaviorsListTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_workitemtrackingprocess_behaviors_list";
    private static final String DESC = "Lista comportamientos (behaviors) de un proceso";

    private final WorkitemtrackingprocessBehaviorsHelper helper;

    @Autowired
    public BehaviorsListTool(AzureDevOpsClientService service, WorkitemtrackingprocessBehaviorsHelper helper) {
        super(service);
        this.helper = helper;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    // Org-level: no requiere project
    @Override protected void validateCommon(Map<String,Object> args) { /* no-op */ }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("processId", Map.of("type","string","description","ID del proceso"));
        props.put("raw", Map.of("type","boolean","description","Si true, devuelve JSON crudo"));
        return Map.of(
            "type","object",
            "properties", props,
            "required", List.of("processId")
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado");
        Object procObj = arguments.get("processId");
        try { helper.validateProcessId(procObj); } catch (IllegalArgumentException e) { return error(e.getMessage()); }
        String processId = procObj.toString().trim();
        Map<String,Object> resp = helper.list(processId);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        if (Boolean.TRUE.equals(arguments.get("raw"))) return Map.of("isError", false, "raw", resp);
        return success(helper.formatList(resp));
    }
}
