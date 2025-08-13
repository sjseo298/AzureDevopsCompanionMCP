package com.mcp.server.tools.azuredevops.workitemtrackingprocess;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WorkitemtrackingprocessBehaviorsHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class BehaviorsGetTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_workitemtrackingprocess_behaviors_get";
    private static final String DESC = "Obtiene un comportamiento (behavior) por ID dentro de un proceso";
    private final WorkitemtrackingprocessBehaviorsHelper helper;

    @Autowired
    public BehaviorsGetTool(AzureDevOpsClientService service, WorkitemtrackingprocessBehaviorsHelper helper) {
        super(service);
        this.helper = helper;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override protected void validateCommon(Map<String, Object> args) { /* org-level */ }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("processId", Map.of("type","string","description","ID del proceso"));
        props.put("behaviorId", Map.of("type","string","description","ID del comportamiento"));
        props.put("raw", Map.of("type","boolean","description","Si true, devuelve JSON crudo"));
        return Map.of(
            "type","object",
            "properties", props,
            "required", List.of("processId","behaviorId")
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado");
        Object p = arguments.get("processId");
        Object b = arguments.get("behaviorId");
        try { helper.validateProcessId(p); helper.validateBehaviorId(b);} catch (IllegalArgumentException e) { return error(e.getMessage()); }
        Map<String,Object> resp = helper.get(p.toString().trim(), b.toString().trim());
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        if (Boolean.TRUE.equals(arguments.get("raw"))) return Map.of("isError", false, "raw", resp);
        return success(helper.formatSingle(resp));
    }
}
