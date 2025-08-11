package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.ProjectsHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Herramienta MCP para listar proyectos de Azure DevOps (Core API /_apis/projects).
 */
@Component
public class ProjectsTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_core_get_projects";
    private static final String DESC = "Obtiene la lista de proyectos de la organización";

    private final ProjectsHelper projectsHelper;

    @Autowired
    public ProjectsTool(AzureDevOpsClientService service, ProjectsHelper projectsHelper) {
        super(service);
        this.projectsHelper = projectsHelper;
    }

    @Override
    public String getName() { return NAME; }

    @Override
    public String getDescription() { return DESC; }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        projectsHelper.validateListProjects(args.get("state"), args.get("top"));
    }

    @Override
    public Map<String, Object> getInputSchema() {
        // Sin parámetros obligatorios; permitimos filtros opcionales: state, top, continuationToken
        Map<String,Object> base = new HashMap<>();
        base.put("type", "object");
        Map<String,Object> props = new HashMap<>();
        props.put("state", Map.of("type", "string", "description", "Filtro por estado del proyecto (WellFormed, CreatePending, Deleted)"));
        props.put("top", Map.of("type", "integer", "minimum", 1, "maximum", 1000, "description", "Número máximo de resultados"));
        props.put("continuationToken", Map.of("type", "string", "description", "Token de continuación para paginación"));
        base.put("properties", props);
        base.put("required", List.of());
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        Map<String,String> query = projectsHelper.buildListProjectsQuery(
            arguments.get("state"),
            arguments.get("top"),
            arguments.get("continuationToken")
        );
        Map<String,Object> resp = projectsHelper.fetchProjects(query);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        String formatted = projectsHelper.formatProjectsList(resp);
        if (formatted != null) return success(formatted);
        return Map.of("isError", false, "raw", resp);
    }
}
