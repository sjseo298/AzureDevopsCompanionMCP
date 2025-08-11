package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import com.mcp.server.services.helpers.CoreTeamsHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class UpdateTeamTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_core_update_team";
    private static final String DESC = "Actualiza un equipo";

    private final CoreTeamsHelper coreTeamsHelper;

    @Autowired
    public UpdateTeamTool(AzureDevOpsClientService service, CoreTeamsHelper coreTeamsHelper) {
        super(service);
        this.coreTeamsHelper = coreTeamsHelper;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> s = new HashMap<>();
        s.put("type","object");
        s.put("properties", Map.of(
            "projectId", Map.of("type","string"),
            "teamId", Map.of("type","string"),
            "name", Map.of("type","string"),
            "description", Map.of("type","string")
        ));
        s.put("required", List.of("projectId","teamId"));
        return s;
    }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        coreTeamsHelper.validateUpdateTeam(
            Objects.toString(args.get("projectId"), null),
            Objects.toString(args.get("teamId"), null),
            args.get("name"),
            args.get("description")
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        String projectId = arguments.get("projectId").toString();
        String teamId = arguments.get("teamId").toString();
        Map<String,Object> body = coreTeamsHelper.buildUpdateTeamBody(arguments.get("name"), arguments.get("description"));
        Map<String,Object> resp = coreTeamsHelper.updateTeam(projectId, teamId, body);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        String formatted = coreTeamsHelper.formatUpdateTeamResponse(resp);
        if (formatted != null) return success(formatted);
        return Map.of("isError", false, "raw", resp);
    }
}
