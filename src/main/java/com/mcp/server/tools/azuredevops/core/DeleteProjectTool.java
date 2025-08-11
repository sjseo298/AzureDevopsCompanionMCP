package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.ProjectsHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DeleteProjectTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_core_delete_project";
    private static final String DESC = "Elimina un proyecto por su ID";

    private final ProjectsHelper projectsHelper;

    @Autowired
    public DeleteProjectTool(AzureDevOpsClientService service, ProjectsHelper projectsHelper) {
        super(service);
        this.projectsHelper = projectsHelper;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type","object",
            "properties", Map.of("projectId", Map.of("type","string","description","GUID del proyecto")),
            "required", List.of("projectId")
        );
    }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        projectsHelper.validateProjectId(Optional.ofNullable(args.get("projectId")).map(Object::toString).orElse(null));
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        String pid = arguments.get("projectId").toString();
        Map<String,Object> resp = projectsHelper.deleteProject(pid);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        String formatted = projectsHelper.formatDeleteProjectResponse(resp);
        if (formatted != null) return success(formatted);
        return Map.of("isError", false, "raw", resp);
    }
}
