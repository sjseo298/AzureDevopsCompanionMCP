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
        Object top = args.get("top");
        if (top != null) {
            try {
                int t = Integer.parseInt(top.toString());
                if (t < 1 || t > 1000) throw new IllegalArgumentException("'top' debe estar entre 1 y 1000");
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("'top' debe ser numérico");
            }
        }
        Object skip = args.get("skip");
        if (skip != null) {
            try {
                int s = Integer.parseInt(skip.toString());
                if (s < 0) throw new IllegalArgumentException("'skip' debe ser >= 0");
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("'skip' debe ser numérico");
            }
        }
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        Map<String,String> q = new LinkedHashMap<>(); q.put("api-version","7.2-preview.3");
        if (Boolean.TRUE.equals(arguments.get("mine"))) q.put("mine","true");
        Object top = arguments.get("top"); if (top != null) q.put("top", String.valueOf(top));
        Object skip = arguments.get("skip"); if (skip != null) q.put("skip", String.valueOf(skip));
        Map<String,Object> resp = azureService.getCoreApi("teams", q);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return Map.of("isError", false, "raw", resp);
    }
}
