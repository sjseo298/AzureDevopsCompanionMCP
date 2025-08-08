package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Categorized Teams: GET /_apis/projects/{projectId}/teams?mine={mine}&api-version=7.2-preview.1
 */
@Component
public class CategorizedTeamsTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_core_get_categorized_teams";
    private static final String DESC = "Obtiene equipos categorizados (legibles y de pertenencia)";

    @Autowired
    public CategorizedTeamsTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> s = new HashMap<>();
        s.put("type","object");
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("projectId", Map.of("type","string","description","GUID del proyecto"));
        props.put("mine", Map.of("type","boolean","description","Si true, solo equipos donde el usuario es miembro"));
        s.put("properties", props);
        s.put("required", List.of("projectId"));
        return s;
    }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        String pid = Optional.ofNullable(args.get("projectId")).map(Object::toString).map(String::trim).orElse("");
        if (pid.isEmpty()) throw new IllegalArgumentException("'projectId' es requerido");
        if (!pid.matches("[0-9a-fA-F-]{36}")) throw new IllegalArgumentException("'projectId' debe ser GUID de 36 chars");
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        String pid = arguments.get("projectId").toString().trim();
        Map<String,String> q = new LinkedHashMap<>();
        if (Boolean.TRUE.equals(arguments.get("mine"))) q.put("mine","true");
        q.put("api-version","7.2-preview.1");
        Map<String,Object> resp = azureService.getCoreApi("projects/"+pid+"/teams", q);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        Object count = resp.get("count"), value = resp.get("value");
        if (count instanceof Number && value instanceof List) {
            StringBuilder sb = new StringBuilder();
            @SuppressWarnings("unchecked") List<Map<String,Object>> items = (List<Map<String, Object>>) value;
            int i=1; for (Map<String,Object> it: items) {
                sb.append(i++).append(") ")
                  .append(String.valueOf(it.getOrDefault("name","<sin nombre>")))
                  .append(" [").append(String.valueOf(it.getOrDefault("id","?"))).append("]\n");
            }
            if (sb.length()==0) sb.append("Sin resultados");
            return success(sb.toString());
        }
        return Map.of("isError", false, "raw", resp);
    }
}
