package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.CoreTeamsHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Obtiene un equipo específico por projectId y teamId
 */
@Component
public class GetTeamsTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_core_get_team";
    private static final String DESC = "Obtiene un equipo por projectId y teamId";

    private final CoreTeamsHelper teamsHelper;

    @Autowired
    public GetTeamsTool(AzureDevOpsClientService service, CoreTeamsHelper teamsHelper) {
        super(service);
        this.teamsHelper = teamsHelper;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> s = new HashMap<>();
        s.put("type", "object");
        s.put("properties", Map.of(
            "projectId", Map.of("type","string","description","GUID del proyecto"),
            "teamId", Map.of("type","string","description","ID/Nombre del equipo")
        ));
        s.put("required", List.of("projectId","teamId"));
        return s;
    }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        teamsHelper.validateGetTeam(
            Optional.ofNullable(args.get("projectId")).map(Object::toString).orElse(null),
            Optional.ofNullable(args.get("teamId")).map(Object::toString).orElse(null)
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        Map<String,Object> resp = teamsHelper.fetchTeam(
            arguments.get("projectId").toString(),
            arguments.get("teamId").toString()
        );
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        String formatted = teamsHelper.formatTeamResponse(resp);
        if (formatted != null) return success(formatted);
        return Map.of("isError", false, "raw", resp);
    }
}
