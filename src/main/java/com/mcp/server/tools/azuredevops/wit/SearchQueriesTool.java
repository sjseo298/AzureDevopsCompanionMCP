package com.mcp.server.tools.azuredevops.wit;


import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitQueriesMailHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_search_queries
 * Busca queries por texto en un proyecto (POST queries/$search con fallback GET ?searchText).
 */
@Component
public class SearchQueriesTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_search_queries";
    private static final String DESC = "Busca queries por texto en un proyecto (POST queries/$search con fallback GET ?searchText).";
    private static final String API_VERSION_OVERRIDE = "7.2-preview";

    private final WitQueriesMailHelper helper;

    @Autowired
    public SearchQueriesTool(AzureDevOpsClientService service) {
        super(service);
        this.helper = new WitQueriesMailHelper(service);
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String, Object>) base.get("properties");
        props.put("searchText", Map.of("type","string","description","Texto a buscar"));
        props.put("expand", Map.of("type","string","enum", List.of("none","clauses","all","wiql"), "description","Nivel de expansión"));
        props.put("top", Map.of("type","integer","description","Límite de resultados"));
        base.put("required", List.of("project","searchText"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        String project = getProject(arguments);
        String team = getTeam(arguments);
        Object searchText = arguments.get("searchText");
        try {
            helper.validateSearchText(searchText);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
        Object expand = arguments.get("expand");
        Object top = arguments.get("top");
        Map<String,Object> body = helper.buildSearchBody(searchText, expand, top);
        Map<String,Object> resp = helper.postSearch(project, team, body);
        if (helper.looksLikeUnsupportedSearchEndpoint(resp)) {
            Map<String,String> query = helper.buildSearchQuery(searchText, expand, top);
            Map<String,Object> getResp = helper.getSearch(project, team, query);
            String formattedErrGet = tryFormatRemoteError(getResp);
            if (formattedErrGet != null) return success(formattedErrGet);
            return success(helper.formatSearchResponse(getResp));
        }
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return success(helper.formatSearchResponse(resp));
    }
}
