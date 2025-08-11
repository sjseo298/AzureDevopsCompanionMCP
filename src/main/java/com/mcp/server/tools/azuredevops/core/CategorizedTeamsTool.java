package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.CoreTeamsHelper;
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

    private final CoreTeamsHelper helper;

    @Autowired
    public CategorizedTeamsTool(AzureDevOpsClientService service, CoreTeamsHelper helper) {
        super(service);
        this.helper = helper;
    }

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
        helper.validateProjectId(Optional.ofNullable(args.get("projectId")).map(Object::toString).orElse(null));
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests"); // fallback tests
        String pid = arguments.get("projectId").toString();
        boolean mine = Boolean.TRUE.equals(arguments.get("mine"));
        Map<String,Object> resp = helper.fetchCategorizedTeams(pid, mine);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        String list = helper.formatTeamsList(resp);
        if (list != null) return success(list);
        return Map.of("isError", false, "raw", resp); // fallback raw
    }
}
