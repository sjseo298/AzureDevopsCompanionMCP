package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.CoreTeamsHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetAllTeamsTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_core_get_all_teams";
    private static final String DESC = "Lista todos los equipos de la organización";

    private final CoreTeamsHelper teamsHelper;

    @Autowired
    public GetAllTeamsTool(AzureDevOpsClientService service, CoreTeamsHelper teamsHelper) {
        super(service);
        this.teamsHelper = teamsHelper;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> s = new HashMap<>();
        s.put("type","object");
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("mine", Map.of("type","boolean","description","Si true, solo equipos donde el usuario es miembro"));
        props.put("top", Map.of("type","integer","minimum",1,"maximum",1000,"description","Número máximo de resultados"));
        props.put("skip", Map.of("type","integer","minimum",0,"description","Número de resultados a omitir"));
        s.put("properties", props);
        s.put("required", List.of());
        return s;
    }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        teamsHelper.validateListAllTeams(args.get("top"), args.get("skip"));
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        Map<String,String> query = teamsHelper.buildAllTeamsQuery(
            Boolean.TRUE.equals(arguments.get("mine")),
            arguments.get("top"),
            arguments.get("skip")
        );
        Map<String,Object> resp = teamsHelper.fetchAllTeams(query);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        String formatted = teamsHelper.formatTeamsList(resp);
        if (formatted != null) return success(formatted);
        return Map.of("isError", false, "raw", resp);
    }
}
