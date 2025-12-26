package com.mcp.server.tools.azuredevops.router;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import com.mcp.server.tools.azuredevops.core.CategorizedTeamsTool;
import com.mcp.server.tools.azuredevops.core.CreateTeamTool;
import com.mcp.server.tools.azuredevops.core.DeleteTeamTool;
import com.mcp.server.tools.azuredevops.core.GetAllTeamsTool;
import com.mcp.server.tools.azuredevops.core.GetTeamMembersTool;
import com.mcp.server.tools.azuredevops.core.GetTeamsTool;
import com.mcp.server.tools.azuredevops.core.TeamsTool;
import com.mcp.server.tools.azuredevops.core.UpdateTeamTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CoreTeamsTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_core_teams";
    private static final String DESC = "Operaciones Core Teams. operation: list|get|list_all|members|categorized|create|update|delete.";

    private final TeamsTool listTeamsTool;
    private final GetTeamsTool getTeamTool;
    private final GetAllTeamsTool listAllTeamsTool;
    private final GetTeamMembersTool getTeamMembersTool;
    private final CategorizedTeamsTool categorizedTeamsTool;
    private final CreateTeamTool createTeamTool;
    private final UpdateTeamTool updateTeamTool;
    private final DeleteTeamTool deleteTeamTool;

    @Autowired
    public CoreTeamsTool(
            AzureDevOpsClientService svc,
            TeamsTool listTeamsTool,
            GetTeamsTool getTeamTool,
            GetAllTeamsTool listAllTeamsTool,
            GetTeamMembersTool getTeamMembersTool,
            CategorizedTeamsTool categorizedTeamsTool,
            CreateTeamTool createTeamTool,
            UpdateTeamTool updateTeamTool,
            DeleteTeamTool deleteTeamTool
    ) {
        super(svc);
        this.listTeamsTool = listTeamsTool;
        this.getTeamTool = getTeamTool;
        this.listAllTeamsTool = listAllTeamsTool;
        this.getTeamMembersTool = getTeamMembersTool;
        this.categorizedTeamsTool = categorizedTeamsTool;
        this.createTeamTool = createTeamTool;
        this.updateTeamTool = updateTeamTool;
        this.deleteTeamTool = deleteTeamTool;
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
                "enum", List.of("list", "get", "list_all", "members", "categorized", "create", "update", "delete"),
                "description", "Operación a ejecutar"
        ));

        // Parámetros comunes usados por los tools existentes
        props.put("projectId", Map.of("type", "string", "description", "GUID proyecto (list)"));
        props.put("teamId", Map.of("type", "string", "description", "GUID team (get/update/delete/members)"));
        props.put("teamName", Map.of("type", "string", "description", "Nombre team (get/members según tool)"));
        props.put("top", Map.of("type", "integer", "description", "Paginación"));
        props.put("skip", Map.of("type", "integer", "description", "Paginación"));

        // create/update
        props.put("name", Map.of("type", "string", "description", "Nombre del team"));
        props.put("description", Map.of("type", "string", "description", "Descripción del team"));
        props.put("projectName", Map.of("type", "string", "description", "Nombre del proyecto (create)"));

        return Map.of("type", "object", "properties", props, "required", List.of("operation"));
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        String op = arguments.get("operation") == null ? "" : arguments.get("operation").toString().trim();
        if (op.isEmpty()) return error("'operation' es requerido");

        return switch (op) {
            case "list" -> delegate(listTeamsTool, arguments);
            case "get" -> delegate(getTeamTool, arguments);
            case "list_all" -> delegate(listAllTeamsTool, arguments);
            case "members" -> delegate(getTeamMembersTool, arguments);
            case "categorized" -> delegate(categorizedTeamsTool, arguments);
            case "create" -> delegate(createTeamTool, arguments);
            case "update" -> delegate(updateTeamTool, arguments);
            case "delete" -> delegate(deleteTeamTool, arguments);
            default -> error("Operación no soportada: " + op);
        };
    }

    private Map<String, Object> delegate(AbstractAzureDevOpsTool tool, Map<String, Object> arguments) {
        Map<String, Object> copy = new LinkedHashMap<>(arguments);
        copy.remove("operation");
        return tool.execute(copy);
    }
}
