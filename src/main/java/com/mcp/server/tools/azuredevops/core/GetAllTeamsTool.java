package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetAllTeamsTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_core_get_all_teams";
    private static final String DESC = "Lista todos los equipos de la organización";

    @Autowired
    public GetAllTeamsTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of("type","object","properties", Map.of(), "required", List.of());
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        Map<String,String> q = new LinkedHashMap<>(); q.put("api-version","7.2-preview.3");
        Map<String,Object> resp = azureService.getCoreApi("teams", q);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return Map.of("isError", false, "raw", resp);
    }
}
