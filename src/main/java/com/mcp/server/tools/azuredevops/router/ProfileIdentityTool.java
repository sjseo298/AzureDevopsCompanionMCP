package com.mcp.server.tools.azuredevops.router;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.GetMyMemberIdTool;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProfileIdentityTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_profile_identity";
    private static final String DESC = "Operaciones de identidad/perfil. operation: get_my_memberid.";

    private final GetMyMemberIdTool getMyMemberIdTool;

    @Autowired
    public ProfileIdentityTool(AzureDevOpsClientService svc, GetMyMemberIdTool getMyMemberIdTool) {
        super(svc);
        this.getMyMemberIdTool = getMyMemberIdTool;
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
                "enum", List.of("get_my_memberid"),
                "description", "Operación a ejecutar"
        ));
        return Map.of(
                "type", "object",
                "properties", props,
                "required", List.of("operation")
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        String op = arguments.get("operation") == null ? "" : arguments.get("operation").toString().trim();
        if (op.isEmpty()) return error("'operation' es requerido");

        return switch (op) {
            case "get_my_memberid" -> delegate(getMyMemberIdTool, arguments);
            default -> error("Operación no soportada: " + op);
        };
    }

    private Map<String, Object> delegate(AbstractAzureDevOpsTool tool, Map<String, Object> arguments) {
        Map<String, Object> copy = new LinkedHashMap<>(arguments);
        copy.remove("operation");
        return tool.execute(copy);
    }
}
