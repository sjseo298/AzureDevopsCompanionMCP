package com.mcp.server.tools.azuredevops.wit;


import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitWorkItemTypesGetHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Tool MCP: azuredevops_wit_work_item_types_get
 * Derivado de scripts/curl/wit/work_item_types_get.sh
 * Endpoint: GET /{project}/_apis/wit/workitemtypes/{type}?api-version=7.2-preview
 */
@Component
public class WorkItemTypesGetTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_work_item_types_get";
    private static final String DESC = "Obtiene el detalle de un tipo de work item en un proyecto.";
    private static final String DEFAULT_API_VERSION = "7.2-preview";

    private final WitWorkItemTypesGetHelper helper;

    public WorkItemTypesGetTool(AzureDevOpsClientService service) {
        super(service);
        this.helper = new WitWorkItemTypesGetHelper(service);
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String,Object> getInputSchema() {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("project", Map.of("type","string","description","Nombre o ID del proyecto"));
        props.put("type", Map.of("type","string","description","Nombre del tipo de work item"));
        props.put("apiVersion", Map.of("type","string","description","Versión de la API", "default", DEFAULT_API_VERSION));
        return Map.of(
            "type","object",
            "properties", props,
            "required", List.of("project","type")
        );
    }

    @Override
    protected Map<String,Object> executeInternal(Map<String,Object> args) {
        String project = Objects.toString(args.get("project"),"");
        String type = Objects.toString(args.get("type"),"");
        String apiVersion = Objects.toString(args.getOrDefault("apiVersion", DEFAULT_API_VERSION));
        if (project.isEmpty() || type.isEmpty()) {
            return error("Faltan parámetros obligatorios: project, type");
        }
        try {
            Map<String,Object> resp = helper.getType(project, type, apiVersion);
            String err = tryFormatRemoteError(resp);
            if (err != null) return success(err);
            return Map.of("isError", false, "result", resp);
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
