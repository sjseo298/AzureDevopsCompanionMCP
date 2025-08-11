package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.CoreTeamsHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CreateTeamTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_core_create_team";
    private static final String DESC = "Crea un equipo dentro de un proyecto";

    private final CoreTeamsHelper teamsHelper;

    @Autowired
    public CreateTeamTool(AzureDevOpsClientService service, CoreTeamsHelper teamsHelper) {
        super(service);
        this.teamsHelper = teamsHelper;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> s = new HashMap<>();
        s.put("type","object");
        s.put("properties", Map.of(
            "projectId", Map.of("type","string","description","GUID del proyecto"),
            "name", Map.of("type","string","description","Nombre del equipo"),
            "description", Map.of("type","string","description","Descripción del equipo (opcional)")
        ));
        s.put("required", List.of("projectId","name"));
        return s;
    }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        teamsHelper.validateCreateTeam(
            Optional.ofNullable(args.get("projectId")).map(Object::toString).orElse(null),
            Optional.ofNullable(args.get("name")).map(Object::toString).orElse(null)
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        String pid = arguments.get("projectId").toString();
        Map<String,Object> body = teamsHelper.buildCreateTeamBody(
            arguments.get("name").toString(),
            Optional.ofNullable(arguments.get("description")).map(Object::toString).orElse(null)
        );
        Map<String,Object> resp = teamsHelper.createTeam(pid, body);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        String formatted = teamsHelper.formatCreateTeamResponse(resp);
        if (formatted != null) return success(formatted);
        return Map.of("isError", false, "raw", resp);
    }
}
