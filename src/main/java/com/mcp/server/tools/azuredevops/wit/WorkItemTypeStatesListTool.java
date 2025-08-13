package com.mcp.server.tools.azuredevops.wit;


import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitWorkItemTypeStatesListHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Tool MCP: azuredevops_wit_work_item_type_states_list
 * Derivado de scripts/curl/wit/work_item_type_states_list.sh
 * Endpoint: GET /{project}/_apis/wit/workitemtypes/{type}/states?api-version=7.2-preview
 */
@Component
public class WorkItemTypeStatesListTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_work_item_type_states_list";
    private static final String DESC = "Lista los estados definidos para un tipo de work item (name, color, order) en un proyecto.";
    private static final String DEFAULT_API_VERSION = "7.2-preview";

    private final WitWorkItemTypeStatesListHelper helper;

    public WorkItemTypeStatesListTool(AzureDevOpsClientService service) {
        super(service);
        this.helper = new WitWorkItemTypeStatesListHelper(service);
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String,Object> getInputSchema() {
    Map<String,Object> props = new LinkedHashMap<>();
    props.put("project", Map.of("type","string","description","Nombre o ID del proyecto"));
    props.put("type", Map.of("type","string","description","Nombre del tipo de work item"));
    props.put("apiVersion", Map.of("type","string","description","Versión de la API", "default", DEFAULT_API_VERSION));
    props.put("raw", Map.of("type","boolean","description","Devuelve JSON crudo (sin formatear)"));
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
        boolean raw = Boolean.TRUE.equals(args.get("raw"));
        if (project.isEmpty() || type.isEmpty()) {
            return error("Faltan parámetros obligatorios: project, type");
        }
        try {
            Map<String,Object> resp = helper.listStates(project, type, apiVersion);
            String err = tryFormatRemoteError(resp);
            if (err != null) return success(err);
            if (raw) {
                return Map.of(
                    "isError", false,
                    "result", resp,
                    "content", java.util.List.of(java.util.Map.of("type","text","text","JSON crudo devuelto (ver campo 'result')"))
                );
            }
            Object val = resp.get("value");
            if (val instanceof java.util.List<?> list) {
                StringBuilder sb = new StringBuilder();
                sb.append("Estados para tipo '").append(type).append("' en proyecto '").append(project).append("' ("+list.size()+"):\n");
                int i=1;
                for (Object o : list) {
                    if (o instanceof java.util.Map<?,?> m) {
                        Object name = m.get("name");
                        Object color = m.get("color");
                        Object order = m.get("order");
                        Object category = m.get("category");
                        sb.append(String.format("%2d) %s", i++, name));
                        if (color != null) sb.append(" | color: ").append(color);
                        if (order != null) sb.append(" | order: ").append(order);
                        if (category != null) sb.append(" | category: ").append(category);
                        sb.append('\n');
                    }
                }
                return success(sb.toString().trim());
            }
            return success("Sin lista de estados en la respuesta");
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
