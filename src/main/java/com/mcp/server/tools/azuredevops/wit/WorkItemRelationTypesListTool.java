package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitWorkItemRelationTypesListHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import java.util.*;

/**
 * Tool MCP: azuredevops_wit_work_item_relation_types_list
 * Derivado de scripts/curl/wit/work_item_relation_types_list.sh
 * Endpoint: GET /_apis/wit/workitemrelationtypes?api-version=7.2-preview
 */
public class WorkItemRelationTypesListTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_work_item_relation_types_list";
    private static final String DESC = "Lista los tipos de relación entre work items definidos en la organización.";
    private static final String DEFAULT_API_VERSION = "7.2-preview";

    private final WitWorkItemRelationTypesListHelper helper;

    public WorkItemRelationTypesListTool(AzureDevOpsClientService service) {
        super(service);
        this.helper = new WitWorkItemRelationTypesListHelper(service);
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String,Object> getInputSchema() {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("apiVersion", Map.of("type","string","description","Versión de la API", "default", DEFAULT_API_VERSION));
        return Map.of(
            "type","object",
            "properties", props,
            "required", List.of()
        );
    }

    @Override
    protected Map<String,Object> executeInternal(Map<String,Object> args) {
        String apiVersion = Objects.toString(args.getOrDefault("apiVersion", DEFAULT_API_VERSION));
        try {
            Map<String,Object> resp = helper.listRelationTypes(apiVersion);
            String err = tryFormatRemoteError(resp);
            if (err != null) return success(err);
            return Map.of("isError", false, "result", resp);
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
