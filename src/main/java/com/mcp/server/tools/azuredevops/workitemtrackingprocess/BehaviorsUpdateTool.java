package com.mcp.server.tools.azuredevops.workitemtrackingprocess;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WorkitemtrackingprocessBehaviorsHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;


public class BehaviorsUpdateTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_workitemtrackingprocess_behaviors_update";
    private static final String DESC = "Actualiza un comportamiento (behavior) de un proceso";
    private final WorkitemtrackingprocessBehaviorsHelper helper;

    @Autowired
    public BehaviorsUpdateTool(AzureDevOpsClientService service, WorkitemtrackingprocessBehaviorsHelper helper) {
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
        props.put("name", Map.of("type","string","description","Nuevo nombre (opcional)"));
        props.put("description", Map.of("type","string","description","Nueva descripción (opcional)"));
        props.put("color", Map.of("type","string","description","Nuevo color (hex) opcional"));
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
        Map<String,Object> body = new LinkedHashMap<>();
        putIfNotBlank(body, "name", arguments.get("name"));
        putIfNotBlank(body, "description", arguments.get("description"));
        putIfNotBlank(body, "color", arguments.get("color"));
        Map<String,Object> resp = helper.update(p.toString().trim(), b.toString().trim(), body);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        if (Boolean.TRUE.equals(arguments.get("raw"))) return Map.of("isError", false, "raw", resp);
        return success(helper.formatSingle(resp));
    }

    private void putIfNotBlank(Map<String,Object> m, String k, Object v) {
        if (v == null) return;
        String s = Objects.toString(v, "").trim();
        if (!s.isEmpty()) m.put(k, s);
    }
}
