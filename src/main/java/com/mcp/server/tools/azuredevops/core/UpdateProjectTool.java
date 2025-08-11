package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import com.mcp.server.services.helpers.ProjectsHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class UpdateProjectTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_core_update_project";
    private static final String DESC = "Actualiza nombre y/o descripción de un proyecto";

    private final ProjectsHelper projectsHelper;

    @Autowired
    public UpdateProjectTool(AzureDevOpsClientService service, ProjectsHelper projectsHelper) {
        super(service);
        this.projectsHelper = projectsHelper;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> s = new HashMap<>();
        s.put("type","object");
        s.put("properties", Map.of(
            "projectId", Map.of("type","string","description","GUID del proyecto"),
            "name", Map.of("type","string","description","Nuevo nombre"),
            "description", Map.of("type","string","description","Nueva descripción")
        ));
        s.put("required", List.of("projectId"));
        return s;
    }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        projectsHelper.validateUpdateProject(
            Objects.toString(args.get("projectId"), null),
            args.get("name"),
            args.get("description")
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        String projectId = arguments.get("projectId").toString();
        Map<String,Object> body = projectsHelper.buildUpdateProjectBody(arguments.get("name"), arguments.get("description"));
        Map<String,Object> resp = projectsHelper.updateProject(projectId, body);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        String formatted = projectsHelper.formatUpdateProjectResponse(resp);
        if (formatted != null) return success(formatted);
        return Map.of("isError", false, "raw", resp);
    }
}
