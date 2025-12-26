package com.mcp.server.tools.azuredevops.router;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import com.mcp.server.tools.azuredevops.wit.ClassificationNodesCreateOrUpdateTool;
import com.mcp.server.tools.azuredevops.wit.ClassificationNodesDeleteTool;
import com.mcp.server.tools.azuredevops.wit.ClassificationNodesGetByIdsTool;
import com.mcp.server.tools.azuredevops.wit.ClassificationNodesGetRootTool;
import com.mcp.server.tools.azuredevops.wit.ClassificationNodesGetTool;
import com.mcp.server.tools.azuredevops.wit.ClassificationNodesUpdateTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class WitClassificationNodesTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_classification_nodes";
    private static final String DESC = "Operaciones WIT Classification Nodes. operation: get_root|get|get_by_ids|upsert|update|delete.";

    private final ClassificationNodesGetRootTool getRootTool;
    private final ClassificationNodesGetTool getTool;
    private final ClassificationNodesGetByIdsTool getByIdsTool;
    private final ClassificationNodesCreateOrUpdateTool upsertTool;
    private final ClassificationNodesUpdateTool updateTool;
    private final ClassificationNodesDeleteTool deleteTool;

    @Autowired
    public WitClassificationNodesTool(
            AzureDevOpsClientService svc,
            ClassificationNodesGetRootTool getRootTool,
            ClassificationNodesGetTool getTool,
            ClassificationNodesGetByIdsTool getByIdsTool,
            ClassificationNodesCreateOrUpdateTool upsertTool,
            ClassificationNodesUpdateTool updateTool,
            ClassificationNodesDeleteTool deleteTool
    ) {
        super(svc);
        this.getRootTool = getRootTool;
        this.getTool = getTool;
        this.getByIdsTool = getByIdsTool;
        this.upsertTool = upsertTool;
        this.updateTool = updateTool;
        this.deleteTool = deleteTool;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) base.get("properties");
        props.put("operation", Map.of(
                "type", "string",
                "enum", List.of("get_root", "get", "get_by_ids", "upsert", "update", "delete"),
                "description", "Operación a ejecutar"
        ));
        props.put("structureGroup", Map.of("type", "string", "description", "Areas|Iterations"));
        props.put("path", Map.of("type", "string", "description", "Ruta del nodo"));
        props.put("ids", Map.of("type", "string", "description", "IDs separados por coma (get_by_ids)"));
        props.put("raw", Map.of("type", "boolean", "description", "Devuelve JSON crudo"));
        base.put("required", List.of("project", "operation"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        String op = arguments.get("operation") == null ? "" : arguments.get("operation").toString().trim();
        if (op.isEmpty()) return error("'operation' es requerido");
        return switch (op) {
            case "get_root" -> delegate(getRootTool, arguments);
            case "get" -> delegate(getTool, arguments);
            case "get_by_ids" -> delegate(getByIdsTool, arguments);
            case "upsert" -> delegate(upsertTool, arguments);
            case "update" -> delegate(updateTool, arguments);
            case "delete" -> delegate(deleteTool, arguments);
            default -> error("Operación no soportada: " + op);
        };
    }

    private Map<String, Object> delegate(AbstractAzureDevOpsTool tool, Map<String, Object> arguments) {
        Map<String, Object> copy = new LinkedHashMap<>(arguments);
        copy.remove("operation");
        return tool.execute(copy);
    }
}
