package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class UpdateTeamTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_core_update_team";
    private static final String DESC = "Actualiza un equipo";

    @Autowired
    public UpdateTeamTool(AzureDevOpsClientService service) { super(service); }

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
        Map<String,Object> body = new LinkedHashMap<>();
        Object n = arguments.get("name"); if (n!=null && !n.toString().isBlank()) body.put("name", n.toString());
        Object d = arguments.get("description"); if (d!=null && !d.toString().isBlank()) body.put("description", d.toString());
        Map<String,Object> resp = azureService.patchCoreApi("projects/"+pid+"/teams/"+tid, null, body, "7.2-preview.3");
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return Map.of("isError", false, "raw", resp);
    }
}
