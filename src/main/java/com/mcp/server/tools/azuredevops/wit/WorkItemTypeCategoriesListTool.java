package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitWorkItemTypeCategoriesListHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Tool MCP: azuredevops_wit_work_item_type_categories_list
 * Derivado de scripts/curl/wit/work_item_type_categories_list.sh
 * Endpoint: GET /{project}/_apis/wit/workitemtypecategories?api-version=7.2-preview
 */
@Component
public class WorkItemTypeCategoriesListTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_work_item_type_categories_list";
    private static final String DESC = "Lista las categorías de tipos de work item en el proyecto.";
    private static final String DEFAULT_API_VERSION = "7.2-preview";

    private final WitWorkItemTypeCategoriesListHelper helper;

    public WorkItemTypeCategoriesListTool(AzureDevOpsClientService service) {
        super(service);
        this.helper = new WitWorkItemTypeCategoriesListHelper(service);
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("project", Map.of("type","string","description","Nombre o ID del proyecto"));
        props.put("apiVersion", Map.of("type","string","description","Versión de la API","default", DEFAULT_API_VERSION));
        props.put("raw", Map.of("type","boolean","description","Devuelve JSON crudo (sin formatear)"));
        return Map.of(
            "type","object",
            "properties", props,
            "required", List.of("project")
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> args) {
        String project = Objects.toString(args.get("project"), "");
        String apiVersion = Objects.toString(args.getOrDefault("apiVersion", DEFAULT_API_VERSION));
        boolean raw = Boolean.TRUE.equals(args.get("raw"));
        if (project.isEmpty()) {
            return error("Falta parámetro obligatorio: project");
        }
        try {
            Map<String,Object> resp = helper.listCategories(project, apiVersion);
            String err = tryFormatRemoteError(resp);
            if (err != null) return success(err);
            if (raw) {
                return Map.of(
                    "isError", false,
                    "result", resp,
                    "content", List.of(Map.of("type","text","text","JSON crudo devuelto (ver campo 'result')"))
                );
            }
            Object val = resp.get("value");
            if (val instanceof List<?> list) {
                StringBuilder sb = new StringBuilder();
                sb.append("Categorías de tipos de work item en proyecto '").append(project).append("' (").append(list.size()).append("):\n");
                int i=1;
                for (Object o : list) {
                    if (o instanceof Map<?,?> m) {
                        Object name = m.get("name");
                        Object referenceName = m.get("referenceName");
                        Object defaultType = m.get("defaultWorkItemType");
                        Object witsObj = m.get("workItemTypes");
                        sb.append(String.format("%2d) %s", i++, name));
                        if (referenceName != null) sb.append(" (").append(referenceName).append(")");
                        if (defaultType != null) sb.append(" | default: ").append(defaultType);
                        if (witsObj instanceof List<?> wits) {
                            sb.append(" | tipos: ").append(wits.size());
                        }
                        sb.append('\n');
                    }
                }
                return success(sb.toString().trim());
            }
            return success("Sin lista de categorías en la respuesta");
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
