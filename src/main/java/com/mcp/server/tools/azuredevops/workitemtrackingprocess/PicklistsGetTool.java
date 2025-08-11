package com.mcp.server.tools.azuredevops.workitemtrackingprocess;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.mcp.server.services.helpers.WorkitemtrackingprocessPicklistsHelper;

import java.util.*;

/**
 * Tool MCP: azuredevops_workitemtrackingprocess_picklists_get
 * Derivado de script scripts/curl/workitemtrackingprocess/picklists_get.sh
 * Operación: Get Picklist (org-level) GET /_apis/work/processes/lists/{id}?api-version=7.2-preview.1
 */
@Component
public class PicklistsGetTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_workitemtrackingprocess_picklists_get";
    private static final String DESC = "Obtiene un picklist (lista de selección) por GUID (nivel organización).";
    private static final String API_VERSION = "7.2-preview.1"; // especificada en la doc local

    private final WorkitemtrackingprocessPicklistsHelper helper;

    @Autowired
    public PicklistsGetTool(AzureDevOpsClientService service, WorkitemtrackingprocessPicklistsHelper helper) {
        super(service);
        this.helper = helper;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    // Org-level: no requiere project
    @Override
    protected void validateCommon(Map<String, Object> args) { /* no project required */ }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("id", Map.of("type","string","description","GUID del picklist"));
        props.put("raw", Map.of("type","boolean","description","Si true, devuelve respuesta cruda"));
        return Map.of(
            "type","object",
            "properties", props,
            "required", List.of("id")
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado");
        Object idObj = arguments.get("id");
        Object rawObj = arguments.get("raw");
        try {
            helper.validateId(idObj);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
        boolean raw = Boolean.TRUE.equals(rawObj);
        Map<String,Object> resp = helper.fetchPicklist(idObj);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        if (raw) {
            return Map.of(
                "isError", false,
                "raw", resp
            );
        }
        return success(helper.formatPicklistResponse(resp));
    }
}
