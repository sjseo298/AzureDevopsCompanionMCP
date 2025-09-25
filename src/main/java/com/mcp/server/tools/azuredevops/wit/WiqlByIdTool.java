package com.mcp.server.tools.azuredevops.wit;


import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitWiqlHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_wiql_by_id
 * Ejecuta una query guardada por ID (GET _apis/wit/wiql/{id}).
 */
@Component
public class WiqlByIdTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_wiql_by_id";
    private static final String DESC = "Ejecuta una query guardada por ID (_apis/wit/wiql/{id}).";
    private static final String DEFAULT_API_VERSION = "7.2-preview";

    private final WitWiqlHelper helper;

    @Autowired
    public WiqlByIdTool(AzureDevOpsClientService service, WitWiqlHelper helper) {
        super(service);
        this.helper = helper;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String, Object>) base.get("properties");
        props.put("id", Map.of("type","string","description","ID (GUID) de la query guardada"));
        props.put("apiVersion", Map.of("type","string","description","Override api-version (opcional, default "+DEFAULT_API_VERSION+")"));
        base.put("required", List.of("project","id"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        String project = getProject(arguments);
        String team = getTeam(arguments);
        Object id = arguments.get("id");
        Object apiVersion = arguments.get("apiVersion");
        try {
            helper.validateId(id);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
        Map<String,Object> resp = helper.fetchById(project, team, id, apiVersion);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) {
            if (helper.isFolderQueryError(resp)) {
                formattedErr += "\nSugerencia: El ID corresponde a una carpeta de queries (isFolder=true). Use 'search_queries' o 'list_queries_root_folders' para localizar una query (isFolder=false) y use ese ID.";
            }
            return success(formattedErr);
        }
        return success(helper.formatResponse(resp));
    }
}
