package com.mcp.server.tools.azuredevops.router;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import com.mcp.server.tools.azuredevops.core.GetAvatarTool;
import com.mcp.server.tools.azuredevops.core.SetAvatarTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CoreAvatarsTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_core_avatars";
    private static final String DESC = "Operaciones de avatar (Graph). operation: get|set.";

    private final GetAvatarTool getAvatarTool;
    private final SetAvatarTool setAvatarTool;

    @Autowired
    public CoreAvatarsTool(AzureDevOpsClientService svc, GetAvatarTool getAvatarTool, SetAvatarTool setAvatarTool) {
        super(svc);
        this.getAvatarTool = getAvatarTool;
        this.setAvatarTool = setAvatarTool;
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
                "enum", List.of("get", "set"),
                "description", "Operación a ejecutar"
        ));
        props.put("subjectDescriptor", Map.of("type", "string", "description", "Descriptor Graph (get/set)"));
        props.put("dataUrl", Map.of("type", "string", "description", "Data URI para set"));
        props.put("filePath", Map.of("type", "string", "description", "Ruta local o file:// para set"));
        props.put("contentType", Map.of("type", "string", "description", "MIME type para set"));
        return Map.of("type", "object", "properties", props, "required", List.of("operation"));
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        String op = arguments.get("operation") == null ? "" : arguments.get("operation").toString().trim();
        if (op.isEmpty()) return error("'operation' es requerido");
        return switch (op) {
            case "get" -> delegate(getAvatarTool, arguments);
            case "set" -> delegate(setAvatarTool, arguments);
            default -> error("Operación no soportada: " + op);
        };
    }

    private Map<String, Object> delegate(AbstractAzureDevOpsTool tool, Map<String, Object> arguments) {
        Map<String, Object> copy = new LinkedHashMap<>(arguments);
        copy.remove("operation");
        return tool.execute(copy);
    }
}
