package com.mcp.server.tools.azuredevops.router;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import com.mcp.server.tools.azuredevops.core.GetProcessTool;
import com.mcp.server.tools.azuredevops.core.ListProcessesTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CoreProcessesTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_core_processes";
    private static final String DESC = "Operaciones Core Processes. operation: list|get.";

    private final ListProcessesTool listProcessesTool;
    private final GetProcessTool getProcessTool;

    @Autowired
    public CoreProcessesTool(AzureDevOpsClientService svc, ListProcessesTool listProcessesTool, GetProcessTool getProcessTool) {
        super(svc);
        this.listProcessesTool = listProcessesTool;
        this.getProcessTool = getProcessTool;
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
        return false;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("operation", Map.of(
                "type", "string",
                "enum", List.of("list", "get"),
                "description", "Operación a ejecutar"
        ));
        props.put("processId", Map.of("type", "string", "description", "GUID del proceso (get)"));
        return Map.of("type", "object", "properties", props, "required", List.of("operation"));
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        String op = arguments.get("operation") == null ? "" : arguments.get("operation").toString().trim();
        if (op.isEmpty()) return error("'operation' es requerido");
        return switch (op) {
            case "list" -> delegate(listProcessesTool, arguments);
            case "get" -> delegate(getProcessTool, arguments);
            default -> error("Operación no soportada: " + op);
        };
    }

    private Map<String, Object> delegate(AbstractAzureDevOpsTool tool, Map<String, Object> arguments) {
        Map<String, Object> copy = new LinkedHashMap<>(arguments);
        copy.remove("operation");
        return tool.execute(copy);
    }
}
