package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitWorkItemTypesFieldGetHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_work_item_types_field_get
 * Derivado de scripts/curl/wit/work_item_types_field_get.sh
 * Endpoint: GET /{project}/_apis/wit/workitemtypes/{type}/fields/{field}?api-version=7.2-preview
 */
public class WorkItemTypesFieldGetTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_work_item_types_field_get";
    private static final String DESC = "Obtiene el detalle de un campo de un tipo de work item, con enriquecimiento opcional.";
    private static final String DEFAULT_API_VERSION = "7.2-preview";

    private final WitWorkItemTypesFieldGetHelper helper;

    public WorkItemTypesFieldGetTool(AzureDevOpsClientService service) {
        super(service);
        this.helper = new WitWorkItemTypesFieldGetHelper(service);
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String,Object> getInputSchema() {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("project", Map.of("type","string","description","Nombre o ID del proyecto"));
        props.put("type", Map.of("type","string","description","Nombre del tipo de work item"));
        props.put("field", Map.of("type","string","description","referenceName del campo"));
        props.put("apiVersion", Map.of("type","string","description","Versión de la API", "default", DEFAULT_API_VERSION));
        props.put("summary", Map.of("type","boolean","description","Vista resumida"));
        props.put("showPicklistItems", Map.of("type","boolean","description","Incluye items de picklist en modo resumen"));
        props.put("noEnrich", Map.of("type","boolean","description","Desactiva enriquecimiento"));
        props.put("raw", Map.of("type","boolean","description","Devuelve JSON crudo del endpoint scoped"));
        return Map.of(
            "type","object",
            "properties", props,
            "required", List.of("project","type","field")
        );
    }

    @Override
    protected Map<String,Object> executeInternal(Map<String,Object> args) {
        String project = Objects.toString(args.get("project"),"");
        String type = Objects.toString(args.get("type"),"");
        String field = Objects.toString(args.get("field"),"");
        String apiVersion = Objects.toString(args.getOrDefault("apiVersion", DEFAULT_API_VERSION));
        boolean summary = Boolean.TRUE.equals(args.get("summary"));
        boolean showPicklistItems = Boolean.TRUE.equals(args.get("showPicklistItems"));
        boolean noEnrich = Boolean.TRUE.equals(args.get("noEnrich"));
        boolean raw = Boolean.TRUE.equals(args.get("raw"));
        boolean enrich = !noEnrich;
        if (project.isEmpty() || type.isEmpty() || field.isEmpty()) {
            return error("Faltan parámetros obligatorios: project, type, field");
        }
        try {
            Map<String,Object> resp = helper.getField(project, type, field, apiVersion, enrich, summary, showPicklistItems, raw);
            return Map.of("isError", false, "result", resp);
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
