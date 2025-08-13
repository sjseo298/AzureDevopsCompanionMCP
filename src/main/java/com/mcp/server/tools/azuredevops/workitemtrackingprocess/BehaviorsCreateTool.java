package com.mcp.server.tools.azuredevops.workitemtrackingprocess;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WorkitemtrackingprocessBehaviorsHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;


public class BehaviorsCreateTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_workitemtrackingprocess_behaviors_create";
    private static final String DESC = "Crea un comportamiento (behavior) en un proceso";
    private final WorkitemtrackingprocessBehaviorsHelper helper;

    @Autowired
    public BehaviorsCreateTool(AzureDevOpsClientService service, WorkitemtrackingprocessBehaviorsHelper helper) {
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
        props.put("name", Map.of("type","string","description","Nombre del comportamiento"));
        props.put("description", Map.of("type","string","description","Descripción (opcional)"));
        props.put("color", Map.of("type","string","description","Color (hex) opcional"));
        props.put("raw", Map.of("type","boolean","description","Si true, devuelve JSON crudo"));
        return Map.of(
            "type","object",
            "properties", props,
            "required", List.of("processId","name")
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado");
        Object p = arguments.get("processId");
        try { helper.validateProcessId(p);} catch (IllegalArgumentException e) { return error(e.getMessage()); }
        String name = opt(arguments, "name");
        if (name == null) return error("'name' es requerido");
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("name", name);
        String desc = opt(arguments, "description"); if (desc != null) body.put("description", desc);
        String color = opt(arguments, "color"); if (color != null) body.put("color", color);
        Map<String,Object> resp = helper.create(p.toString().trim(), body);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        if (Boolean.TRUE.equals(arguments.get("raw"))) return Map.of("isError", false, "raw", resp);
        return success(helper.formatSingle(resp));
    }

    private String opt(Map<String,Object> args, String key) {
        Object v = args.get(key);
        if (v == null) return null;
        String s = Objects.toString(v, "").trim();
        return s.isEmpty() ? null : s;
    }
}
