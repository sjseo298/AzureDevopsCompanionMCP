package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import com.mcp.server.services.helpers.CoreTeamsHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Herramienta MCP para listar equipos (teams) de un proyecto.
 * Usa endpoint Core: GET /_apis/projects/{projectId}/teams
 */
@Component
public class TeamsTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_core_get_teams";
    private static final String DESC = "Obtiene equipos de un proyecto (requiere projectId)";

    private final CoreTeamsHelper coreTeamsHelper;

    @Autowired
    public TeamsTool(AzureDevOpsClientService service, CoreTeamsHelper coreTeamsHelper) {
        super(service);
        this.coreTeamsHelper = coreTeamsHelper;
    }

    @Override
    public String getName() { return NAME; }

    @Override
    public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new HashMap<>();
        base.put("type", "object");
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("projectId", Map.of("type", "string", "description", "ID del proyecto (GUID)"));
        props.put("top", Map.of("type", "integer", "minimum", 1, "maximum", 1000));
        props.put("skip", Map.of("type", "integer", "minimum", 0));
        base.put("properties", props);
        base.put("required", List.of("projectId"));
        return base;
    }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        coreTeamsHelper.validateListProjectTeams(
            Objects.toString(args.get("projectId"), null),
            args.get("top"),
            args.get("skip")
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        String projectId = arguments.get("projectId").toString();
        Map<String,String> query = coreTeamsHelper.buildListProjectTeamsQuery(arguments.get("top"), arguments.get("skip"));
        Map<String,Object> resp = coreTeamsHelper.fetchProjectTeams(projectId, query);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        String formatted = coreTeamsHelper.formatTeamsList(resp);
        if (formatted != null) return success(formatted);
        return Map.of("isError", false, "raw", resp);
    }
}
