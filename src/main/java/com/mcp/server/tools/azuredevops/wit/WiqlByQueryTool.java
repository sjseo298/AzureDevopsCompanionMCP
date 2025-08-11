package com.mcp.server.tools.azuredevops.wit;


import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Component;
import com.mcp.server.services.helpers.WitWiqlHelper;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_wiql_by_query
 * Ejecuta una consulta WIQL ad-hoc (POST _apis/wit/wiql).
 */
@Component
public class WiqlByQueryTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_wiql_by_query";
    private static final String DESC = "Ejecuta una consulta WIQL ad-hoc en el proyecto (POST _apis/wit/wiql).";
    private static final String DEFAULT_API_VERSION = "7.2-preview";

    private final WitWiqlHelper helper;

    @Autowired
    public WiqlByQueryTool(AzureDevOpsClientService service, WitWiqlHelper helper) {
        super(service);
        this.helper = helper;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String, Object>) base.get("properties");
        props.put("wiql", Map.of("type","string","description","Consulta WIQL a ejecutar"));
        props.put("apiVersion", Map.of("type","string","description","Override api-version (opcional, default "+DEFAULT_API_VERSION+")"));
        base.put("required", List.of("project","wiql"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        String project = getProject(arguments);
        String team = getTeam(arguments);
        Object wiql = arguments.get("wiql");
        Object apiVersion = arguments.get("apiVersion");
        try {
            helper.validateWiql(wiql);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
        Map<String,Object> resp = helper.fetchByQuery(project, team, wiql, apiVersion);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return success(helper.formatResponse(resp));
    }
}
