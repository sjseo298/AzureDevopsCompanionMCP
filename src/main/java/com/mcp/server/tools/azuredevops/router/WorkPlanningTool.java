package com.mcp.server.tools.azuredevops.router;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.BacklogsTool;
import com.mcp.server.tools.azuredevops.BoardsTool;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class WorkPlanningTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_work_planning";
    private static final String DESC = "Operaciones Work/Planning. operation: get_boards|get_backlogs.";

    private final BoardsTool boardsTool;
    private final BacklogsTool backlogsTool;

    @Autowired
    public WorkPlanningTool(AzureDevOpsClientService svc, BoardsTool boardsTool, BacklogsTool backlogsTool) {
        super(svc);
        this.boardsTool = boardsTool;
        this.backlogsTool = backlogsTool;
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
    public Map<String, Object> getInputSchema() {
        Map<String, Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) base.get("properties");
        props.put("operation", Map.of(
                "type", "string",
                "enum", List.of("get_boards", "get_backlogs"),
                "description", "Operación a ejecutar"
        ));
        props.put("boardId", Map.of("type", "string", "description", "Solo para get_boards (detalle por id/nombre)"));
        base.put("required", List.of("project", "operation"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        String op = arguments.get("operation") == null ? "" : arguments.get("operation").toString().trim();
        if (op.isEmpty()) return error("'operation' es requerido");

        return switch (op) {
            case "get_boards" -> delegate(boardsTool, arguments);
            case "get_backlogs" -> delegate(backlogsTool, arguments);
            default -> error("Operación no soportada: " + op);
        };
    }

    private Map<String, Object> delegate(AbstractAzureDevOpsTool tool, Map<String, Object> arguments) {
        Map<String, Object> copy = new LinkedHashMap<>(arguments);
        copy.remove("operation");
        return tool.execute(copy);
    }
}
