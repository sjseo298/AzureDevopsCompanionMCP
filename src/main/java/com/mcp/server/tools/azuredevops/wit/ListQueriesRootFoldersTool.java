package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitListQueriesRootFoldersHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_list_queries_root_folders
 * Lista carpetas raíz y subcarpetas de queries en un proyecto.
 */
@Component
public class ListQueriesRootFoldersTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_list_queries_root_folders";
    private static final String DESC = "Lista carpetas raíz y subcarpetas de queries en un proyecto.";
    private static final String API_VERSION_OVERRIDE = "7.2-preview";

    private final WitListQueriesRootFoldersHelper helper;

    @Autowired
    public ListQueriesRootFoldersTool(AzureDevOpsClientService service) {
        super(service);
        this.helper = new WitListQueriesRootFoldersHelper(service);
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String, Object>) base.get("properties");
        props.put("expand", Map.of("type","string","enum", List.of("none","clauses","all","wiql"), "description","Nivel de expansión"));
        props.put("depth", Map.of("type","integer","description","Profundidad de carpetas"));
        props.put("includeDeleted", Map.of("type","boolean","description","Incluir eliminados"));
        props.put("queryType", Map.of("type","string","enum", List.of("flat","tree","oneHop"), "description","Tipo de query"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        String project = getProject(arguments);
        String team = getTeam(arguments);
        Object expand = arguments.get("expand");
        Object depth = arguments.get("depth");
        Object includeDeleted = arguments.get("includeDeleted");
        Object queryType = arguments.get("queryType");
        Map<String,String> query = helper.buildQuery(expand, depth, includeDeleted, queryType);
        Map<String,Object> resp = helper.fetchRootFolders(project, team, query);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return success(helper.formatRootFoldersResponse(resp));
    }
}
