package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitWorkItemGetHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** Tool MCP: azuredevops_wit_work_item_get (GET workitems/{id}) */
@Component
public class WorkItemGetTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_work_item_get";
    private static final String DESC = "Obtiene un work item por ID y devuelve todos los datos";
    private static final String DEFAULT_API_VERSION = "7.2-preview";

    private final WitWorkItemGetHelper helper;

    public WorkItemGetTool(AzureDevOpsClientService service) {
        super(service);
        this.helper = new WitWorkItemGetHelper(service);
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    // Work items pueden consultarse por ID sin especificar proyecto
    @Override
    protected boolean isProjectRequired() { return false; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("project", Map.of("type","string","description","Nombre o ID del proyecto Azure DevOps (opcional para consulta por ID)"));
        props.put("team", Map.of("type","string","description","Nombre o ID del equipo (opcional)"));
        props.put("id", Map.of("type","integer","description","ID del work item"));
        props.put("fields", Map.of("type","string","description","Campos referenceName separados por coma"));
        props.put("expand", Map.of("type","string","description","None|Relations|Links|All"));
        props.put("asOf", Map.of("type","string","description","Fecha/hora ISO"));
        props.put("apiVersion", Map.of("type","string","description","Versión API","default", DEFAULT_API_VERSION));
        props.put("api-version", Map.of("type","string","description","Alias (script) de apiVersion"));
        props.put("raw", Map.of("type","boolean","description","Devuelve JSON crudo (recomendado)"));
    return Map.of("type","object","properties",props,"required", java.util.List.of("id"));
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> args) {
        if (!args.containsKey("apiVersion") && args.containsKey("api-version")) args.put("apiVersion", args.get("api-version"));
        try {
            Map<String,Object> resp = helper.get(args);
            String err = tryFormatRemoteError(resp);
            boolean raw = Boolean.TRUE.equals(args.get("raw"));
            if (raw) return rawSuccess(resp);
            if (err != null) return success(err);
            return Map.of(
                "isError", false,
                "result", resp,
                "content", java.util.List.of(java.util.Map.of("type","text","text", toJson(resp)))
            );
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
