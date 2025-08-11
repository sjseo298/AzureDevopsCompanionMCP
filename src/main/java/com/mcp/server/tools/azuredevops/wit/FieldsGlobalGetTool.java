package com.mcp.server.tools.azuredevops.wit;


import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitFieldsGlobalGetHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_fields_global_get
 * Derivado de script scripts/curl/wit/fields_global_get.sh
 * Endpoint org-level: GET /_apis/wit/fields/{referenceName}?api-version=7.2-preview
 */
@Component
public class FieldsGlobalGetTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_fields_global_get";
    private static final String DESC = "Obtiene definición global de un campo (tipo, usage, readOnly, picklistId, valores).";
    private static final String API_VERSION = "7.2-preview"; // según script

    private final WitFieldsGlobalGetHelper helper;

    @Autowired
    public FieldsGlobalGetTool(AzureDevOpsClientService service) {
        super(service);
        this.helper = new WitFieldsGlobalGetHelper(service);
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    // Org-level, no requiere project
    @Override protected void validateCommon(Map<String,Object> args) { /* no project */ }

    @Override
    public Map<String,Object> getInputSchema() {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("field", Map.of("type","string","description","referenceName del campo"));
        props.put("raw", Map.of("type","boolean","description","Si true, devuelve JSON crudo"));
        return Map.of(
            "type","object",
            "properties", props,
            "required", List.of("field")
        );
    }

    @Override
    protected Map<String,Object> executeInternal(Map<String,Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado");
        Object fObj = arguments.get("field");
        boolean raw = Boolean.TRUE.equals(arguments.get("raw"));
        try {
            helper.validate(fObj);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
        String fieldRef = fObj.toString().trim();
        Map<String,Object> resp = helper.fetchField(fieldRef);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        if (raw) return Map.of("isError", false, "raw", resp);
        return success(helper.formatFieldResponse(resp));
    }
}
