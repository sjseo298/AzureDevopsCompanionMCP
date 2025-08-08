package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
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

    @Autowired
    public GetTeamsTool(AzureDevOpsClientService service) { super(service); }

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
        String pid = Optional.ofNullable(args.get("projectId")).map(Object::toString).map(String::trim).orElse("");
        String tid = Optional.ofNullable(args.get("teamId")).map(Object::toString).map(String::trim).orElse("");
        if (pid.isEmpty() || tid.isEmpty()) throw new IllegalArgumentException("'projectId' y 'teamId' son requeridos");
        if (!pid.matches("[0-9a-fA-F-]{36}")) throw new IllegalArgumentException("'projectId' debe ser GUID de 36 chars");
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        String pid = arguments.get("projectId").toString().trim();
        String tid = arguments.get("teamId").toString().trim();
        Map<String,String> q = new LinkedHashMap<>();
        q.put("api-version", "7.2-preview.3");
        Map<String,Object> resp = azureService.getCoreApi("projects/"+pid+"/teams/"+tid, q);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        if (resp.containsKey("id") || resp.containsKey("name")) {
            String text = String.format("%s [%s]", String.valueOf(resp.getOrDefault("name","<sin nombre>")), String.valueOf(resp.getOrDefault("id","?")));
            return success(text);
        }
        return Map.of("isError", false, "raw", resp);
    }
}
