package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import java.util.*;

/**
 * Helper para ejecución de queries WIQL en Azure DevOps.
 */
public class WitWiqlHelper {
    private final AzureDevOpsClientService azureService;

    public WitWiqlHelper(AzureDevOpsClientService svc) {
        this.azureService = svc;
    }

    public void validateId(Object id) {
        if (id == null || id.toString().trim().isEmpty()) throw new IllegalArgumentException("'id' es requerido");
    }
    public void validateWiql(Object wiql) {
        if (wiql == null || wiql.toString().trim().isEmpty()) throw new IllegalArgumentException("'wiql' es requerido");
    }
    public String resolveApiVersion(Object apiVersion, String defaultVersion) {
        return Optional.ofNullable(apiVersion).map(Object::toString).filter(s->!s.isBlank()).orElse(defaultVersion);
    }

    public Map<String,Object> fetchById(String project, String team, Object id, Object apiVersion) {
        String endpoint = "wiql/" + id;
        String version = resolveApiVersion(apiVersion, "7.2-preview");
        return azureService.getWitApiWithQuery(project, team, endpoint, null, version);
    }
    public Map<String,Object> fetchByQuery(String project, String team, Object wiql, Object apiVersion) {
        String version = resolveApiVersion(apiVersion, "7.2-preview");
        Map<String,Object> body = Map.of("query", wiql.toString());
        return azureService.postWitApi(project, team, "wiql", body, version);
    }

    public boolean isFolderQueryError(Map<String,Object> resp) {
        if (resp == null) return false;
        Object typeKey = resp.get("typeKey");
        Object message = resp.get("message");
        return typeKey != null && "QueryException".equals(typeKey.toString()) && message != null && message.toString().contains("Querying folders is not supported");
    }

    public String formatResponse(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Sin datos)";
        StringBuilder sb = new StringBuilder();
        Object queryType = data.get("queryType");
        if (queryType != null) sb.append("Tipo: ").append(queryType).append('\n');
        @SuppressWarnings("unchecked") List<Map<String,Object>> cols = (List<Map<String,Object>>) data.get("columns");
        if (cols != null && !cols.isEmpty()) {
            sb.append("Columnas (").append(cols.size()).append("): ");
            cols.stream().limit(8).forEach(c -> sb.append(c.getOrDefault("referenceName","?")));
            sb.append('\n');
        }
        Object workItems = data.get("workItems");
        if (workItems instanceof List) {
            List<?> list = (List<?>) workItems;
            sb.append("WorkItems: ").append(list.size()).append('\n');
            int i=1;
            for (Object o : list) {
                if (i>25) { sb.append("... (").append(list.size()-25).append(" más)\n"); break; }
                if (o instanceof Map) {
                    Object wid = ((Map<?,?>)o).get("id");
                    sb.append(i++).append(") ID=").append(wid).append('\n');
                }
            }
        }
        return sb.toString().isBlank() ? data.toString() : sb.toString();
    }
}
