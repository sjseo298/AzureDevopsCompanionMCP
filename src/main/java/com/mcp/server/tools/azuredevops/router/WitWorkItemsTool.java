package com.mcp.server.tools.azuredevops.router;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import com.mcp.server.tools.azuredevops.wit.WorkItemCreateTool;
import com.mcp.server.tools.azuredevops.wit.WorkItemDeleteTool;
import com.mcp.server.tools.azuredevops.wit.WorkItemGetTool;
import com.mcp.server.tools.azuredevops.wit.WorkItemUpdateTool;
import com.mcp.server.tools.azuredevops.wit.WorkItemsBatchTool;
import com.mcp.server.tools.azuredevops.wit.WorkItemsDeleteListTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class WitWorkItemsTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_work_items";
    private static final String DESC = "Operaciones WIT Work Items. operation: get|create|update|delete|batch_get|bulk_delete.";

    private final WorkItemGetTool getTool;
    private final WorkItemCreateTool createTool;
    private final WorkItemUpdateTool updateTool;
    private final WorkItemDeleteTool deleteTool;
    private final WorkItemsBatchTool batchTool;
    private final WorkItemsDeleteListTool bulkDeleteTool;

    @Autowired
    public WitWorkItemsTool(
            AzureDevOpsClientService svc,
            WorkItemGetTool getTool,
            WorkItemCreateTool createTool,
            WorkItemUpdateTool updateTool,
            WorkItemDeleteTool deleteTool,
            WorkItemsBatchTool batchTool,
            WorkItemsDeleteListTool bulkDeleteTool
    ) {
        super(svc);
        this.getTool = getTool;
        this.createTool = createTool;
        this.updateTool = updateTool;
        this.deleteTool = deleteTool;
        this.batchTool = batchTool;
        this.bulkDeleteTool = bulkDeleteTool;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESC;
    }

    @Override
    protected boolean isProjectRequired() {
        // Algunas operaciones (get por id) aceptan project opcional
        return false;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) base.get("properties");

        props.put("operation", Map.of(
                "type", "string",
                "enum", List.of("get", "create", "update", "delete", "batch_get", "bulk_delete"),
                "description", "Operación a ejecutar"
        ));

        // Unión (subset) de parámetros de los tools delegados
        props.put("id", Map.of("type", "integer", "description", "Work item ID (get/update/delete)"));
        props.put("ids", Map.of("type", "string", "description", "IDs separados por coma (batch_get/bulk_delete)"));
        props.put("fields", Map.of("type", "string", "description", "Campos referenceName separados por coma"));
        props.put("expand", Map.of("type", "string", "description", "None|Relations|Links|All (get)"));
        props.put("asOf", Map.of("type", "string", "description", "Fecha/hora ISO (get/batch_get)"));

        props.put("type", Map.of("type", "string", "description", "Tipo de work item (create)"));
        props.put("title", Map.of("type", "string", "description", "Título (create/update shortcut)"));
        props.put("description", Map.of("type", "string", "description", "Descripción (create/update shortcut)"));
        props.put("state", Map.of("type", "string", "description", "Estado (create/update shortcut)"));
        props.put("area", Map.of("type", "string", "description", "AreaPath (create/update shortcut)"));
        props.put("iteration", Map.of("type", "string", "description", "IterationPath (create/update shortcut)"));
        props.put("parentId", Map.of("type", "integer", "description", "Work item padre (create/update)"));
        props.put("parent", Map.of("type", "integer", "description", "Alias de parentId"));
        props.put("relations", Map.of("type", "string", "description", "Relaciones extra (create/update)"));
        props.put("add", Map.of("type", "string", "description", "Patch add k=v (update)"));
        props.put("replace", Map.of("type", "string", "description", "Patch replace k=v (update)"));
        props.put("remove", Map.of("type", "string", "description", "Patch remove fields (update)"));

        props.put("apiVersion", Map.of("type", "string", "description", "Override apiVersion"));
        props.put("api-version", Map.of("type", "string", "description", "Alias script apiVersion"));
        props.put("raw", Map.of("type", "boolean", "description", "Devuelve JSON crudo"));
        props.put("validateOnly", Map.of("type", "boolean", "description", "validateOnly"));
        props.put("validate-only", Map.of("type", "boolean", "description", "Alias validateOnly"));
        props.put("bypassRules", Map.of("type", "boolean", "description", "bypassRules"));
        props.put("bypass-rules", Map.of("type", "boolean", "description", "Alias bypassRules"));
        props.put("suppressNotifications", Map.of("type", "boolean", "description", "suppressNotifications"));
        props.put("suppress-notifications", Map.of("type", "boolean", "description", "Alias suppressNotifications"));
        props.put("errorPolicy", Map.of("type", "string", "description", "Omit|Fail (batch_get)"));

        base.put("required", List.of("operation"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        String op = arguments.get("operation") == null ? "" : arguments.get("operation").toString().trim();
        if (op.isEmpty()) return error("'operation' es requerido");

        return switch (op) {
            case "get" -> delegate(getTool, arguments);
            case "create" -> delegate(createTool, arguments);
            case "update" -> delegate(updateTool, arguments);
            case "delete" -> delegate(deleteTool, arguments);
            case "batch_get" -> delegate(batchTool, arguments);
            case "bulk_delete" -> delegate(bulkDeleteTool, arguments);
            default -> error("Operación no soportada: " + op);
        };
    }

    private Map<String, Object> delegate(AbstractAzureDevOpsTool tool, Map<String, Object> arguments) {
        Map<String, Object> copy = new LinkedHashMap<>(arguments);
        copy.remove("operation");
        return tool.execute(copy);
    }
}
