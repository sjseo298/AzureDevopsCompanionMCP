package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;
import java.util.*;

/** Helper para eliminar múltiples work items (ids coma) */
@Component
public class WitWorkItemsDeleteListHelper {
    private final AzureDevOpsClientService client;
    public WitWorkItemsDeleteListHelper(AzureDevOpsClientService client) { this.client = client; }

    public Map<String,Object> deleteList(Map<String,Object> args) {
        String project = Objects.toString(args.get("project"),"").trim();
        if (project.isEmpty()) throw new IllegalArgumentException("project requerido");
        Object apiVerAlias = args.get("api-version"); if (apiVerAlias != null && !args.containsKey("apiVersion")) args.put("apiVersion", apiVerAlias);
        String apiVersion = Objects.toString(args.getOrDefault("apiVersion","7.2-preview"));
        String ids = Objects.toString(args.get("ids"),"").trim();
        if (ids.isEmpty()) throw new IllegalArgumentException("ids requerido");
        boolean destroy = Boolean.TRUE.equals(args.get("destroy"));
        Map<String,String> query = new LinkedHashMap<>();
        query.put("ids", ids);
        query.put("api-version", apiVersion);
        if (destroy) query.put("destroy", "true");
        return client.deleteWitApiWithQuery(project, null, "workitems", query, apiVersion);
    }
}
