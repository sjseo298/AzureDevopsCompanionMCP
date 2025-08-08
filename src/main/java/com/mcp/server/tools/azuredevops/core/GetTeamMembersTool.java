package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetTeamMembersTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_core_get_team_members";
    private static final String DESC = "Obtiene los miembros de un equipo con propiedades extendidas";

    @Autowired
    public GetTeamMembersTool(AzureDevOpsClientService service) { super(service); }

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
        q.put("api-version","7.2-preview.3");
        Map<String,Object> resp = azureService.getCoreApi("projects/"+pid+"/teams/"+tid+"/members", q);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        Object count = resp.get("count"); Object value = resp.get("value");
        if (count instanceof Number && value instanceof List) {
            StringBuilder sb = new StringBuilder();
            @SuppressWarnings("unchecked") List<Map<String,Object>> items = (List<Map<String, Object>>) value;
            int idx=1; for (Map<String,Object> it: items) {
                sb.append(idx++).append(") ")
                  .append(String.valueOf(it.getOrDefault("displayName","<sin nombre>")))
                  .append(" [").append(String.valueOf(it.getOrDefault("id","?"))).append("]\n");
            }
            if (sb.length()==0) sb.append("Sin resultados");
            return success(sb.toString());
        }
        return Map.of("isError", false, "raw", resp);
    }
}
