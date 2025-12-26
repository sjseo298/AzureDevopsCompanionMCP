package com.mcp.server.tools.azuredevops.router;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import com.mcp.server.tools.azuredevops.core.CreateProjectTool;
import com.mcp.server.tools.azuredevops.core.DeleteProjectTool;
import com.mcp.server.tools.azuredevops.core.GetProjectPropertiesTool;
import com.mcp.server.tools.azuredevops.core.GetProjectTool;
import com.mcp.server.tools.azuredevops.core.ProjectsTool;
import com.mcp.server.tools.azuredevops.core.SetProjectPropertiesTool;
import com.mcp.server.tools.azuredevops.core.UpdateProjectTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CoreProjectsTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_core_projects";
    private static final String DESC = "Operaciones Core Projects. operation: list|get|get_properties|create|update|delete|set_properties.";

    private final ProjectsTool listProjectsTool;
    private final GetProjectTool getProjectTool;
    private final GetProjectPropertiesTool getProjectPropertiesTool;
    private final CreateProjectTool createProjectTool;
    private final UpdateProjectTool updateProjectTool;
    private final DeleteProjectTool deleteProjectTool;
    private final SetProjectPropertiesTool setProjectPropertiesTool;

    @Autowired
    public CoreProjectsTool(
            AzureDevOpsClientService svc,
            ProjectsTool listProjectsTool,
            GetProjectTool getProjectTool,
            GetProjectPropertiesTool getProjectPropertiesTool,
            CreateProjectTool createProjectTool,
            UpdateProjectTool updateProjectTool,
            DeleteProjectTool deleteProjectTool,
            SetProjectPropertiesTool setProjectPropertiesTool
    ) {
        super(svc);
        this.listProjectsTool = listProjectsTool;
        this.getProjectTool = getProjectTool;
        this.getProjectPropertiesTool = getProjectPropertiesTool;
        this.createProjectTool = createProjectTool;
        this.updateProjectTool = updateProjectTool;
        this.deleteProjectTool = deleteProjectTool;
        this.setProjectPropertiesTool = setProjectPropertiesTool;
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
                "enum", List.of("list", "get", "get_properties", "create", "update", "delete", "set_properties"),
                "description", "Operación a ejecutar"
        ));

        // Unión de parámetros soportados por los tools existentes
        props.put("state", Map.of("type", "string", "description", "Filtro (list)"));
        props.put("top", Map.of("type", "integer", "description", "Máximo resultados (list)"));
        props.put("continuationToken", Map.of("type", "string", "description", "Paginación (list)"));

        props.put("projectId", Map.of("type", "string", "description", "GUID proyecto (get/get_properties/update/delete/set_properties)"));

        props.put("name", Map.of("type", "string", "description", "Nombre del proyecto (create)"));
        props.put("description", Map.of("type", "string", "description", "Descripción (create/update)"));
        props.put("visibility", Map.of("type", "string", "description", "private|public (create)"));
        props.put("sourceControlType", Map.of("type", "string", "description", "Git|TFVC (create)"));
        props.put("processTypeId", Map.of("type", "string", "description", "GUID template proceso (create)"));

        // Set properties tool usa campos específicos (se deja libre para el tool delegado)
        props.put("properties", Map.of("type", "object", "description", "Propiedades del proyecto (set_properties)"));

        return Map.of("type", "object", "properties", props, "required", List.of("operation"));
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        String op = arguments.get("operation") == null ? "" : arguments.get("operation").toString().trim();
        if (op.isEmpty()) return error("'operation' es requerido");

        return switch (op) {
            case "list" -> delegate(listProjectsTool, arguments);
            case "get" -> delegate(getProjectTool, arguments);
            case "get_properties" -> delegate(getProjectPropertiesTool, arguments);
            case "create" -> delegate(createProjectTool, arguments);
            case "update" -> delegate(updateProjectTool, arguments);
            case "delete" -> delegate(deleteProjectTool, arguments);
            case "set_properties" -> delegate(setProjectPropertiesTool, arguments);
            default -> error("Operación no soportada: " + op);
        };
    }

    private Map<String, Object> delegate(AbstractAzureDevOpsTool tool, Map<String, Object> arguments) {
        Map<String, Object> copy = new LinkedHashMap<>(arguments);
        copy.remove("operation");
        return tool.execute(copy);
    }
}
