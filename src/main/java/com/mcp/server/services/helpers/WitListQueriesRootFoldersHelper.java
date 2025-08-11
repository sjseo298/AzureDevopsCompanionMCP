package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper para listar carpetas raíz y subcarpetas de queries en un proyecto.
 */
@Component
public class WitListQueriesRootFoldersHelper {
    private final AzureDevOpsClientService azureService;
    private static final String API_VERSION_OVERRIDE = "7.2-preview";

    public WitListQueriesRootFoldersHelper(AzureDevOpsClientService azureService) {
        this.azureService = azureService;
    }

    public Map<String,String> buildQuery(Object expand, Object depth, Object includeDeleted, Object queryType) {
        Map<String,String> query = new LinkedHashMap<>();
        if (expand != null) query.put("$expand", expand.toString());
        if (depth != null) query.put("depth", depth.toString());
        if (includeDeleted != null) query.put("includeDeleted", includeDeleted.toString());
        if (queryType != null) query.put("queryType", queryType.toString());
        return query.isEmpty() ? null : query;
    }

    public Map<String,Object> fetchRootFolders(String project, String team, Map<String,String> query) {
        return azureService.getWitApiWithQuery(project, team, "queries", query, API_VERSION_OVERRIDE);
    }

    public String formatRootFoldersResponse(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Sin resultados)";
        Object val = data.get("value");
        if (val instanceof List) {
            List<?> list = (List<?>) val;
            if (list.isEmpty()) return "(Sin resultados)";
            StringBuilder sb = new StringBuilder("=== Queries Root Folders ===\n\n");
            int i=1;
            for (Object o : list) {
                if (o instanceof Map) {
                    Map<?,?> m = (Map<?,?>) o;
                    Object id = m.get("id");
                    Object name = m.get("name");
                    sb.append(i++).append(") ").append(name != null ? name : "(sin nombre)")
                      .append(" [").append(id != null ? id : "?").append("]\n");
                }
            }
            return sb.toString();
        }
        return data.toString();
    }
}
