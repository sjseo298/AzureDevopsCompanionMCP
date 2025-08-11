package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.CoreTeamsHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DeleteTeamTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_core_delete_team";
    private static final String DESC = "Elimina un equipo";

    private final CoreTeamsHelper teamsHelper;

    @Autowired
    public DeleteTeamTool(AzureDevOpsClientService service, CoreTeamsHelper teamsHelper) {
        super(service);
        this.teamsHelper = teamsHelper;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type","object",
            "properties", Map.of(
                "projectId", Map.of("type","string"),
                "teamId", Map.of("type","string")
            ),
            "required", List.of("projectId","teamId")
        );
    }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        teamsHelper.validateDeleteTeam(
            Optional.ofNullable(args.get("projectId")).map(Object::toString).orElse(null),
            Optional.ofNullable(args.get("teamId")).map(Object::toString).orElse(null)
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        String pid = arguments.get("projectId").toString();
        String tid = arguments.get("teamId").toString();
        Map<String,Object> resp = teamsHelper.deleteTeam(pid, tid);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        String formatted = teamsHelper.formatDeleteTeamResponse(resp);
        if (formatted != null) return success(formatted);
        return Map.of("isError", false, "raw", resp);
    }
}
