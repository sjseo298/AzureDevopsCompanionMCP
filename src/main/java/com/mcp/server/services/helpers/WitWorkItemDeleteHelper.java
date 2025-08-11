package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import java.util.*;

/** Helper para eliminar un work item (respeta destroy flag). */
public class WitWorkItemDeleteHelper {
    private final AzureDevOpsClientService client;
    public WitWorkItemDeleteHelper(AzureDevOpsClientService client) { this.client = client; }

    public Map<String,Object> deleteOne(Map<String,Object> args) {
        String project = Objects.toString(args.get("project"),"").trim();
        if (project.isEmpty()) throw new IllegalArgumentException("project requerido");
        Object apiVerAlias = args.get("api-version"); if (apiVerAlias != null && !args.containsKey("apiVersion")) args.put("apiVersion", apiVerAlias);
        String apiVersion = Objects.toString(args.getOrDefault("apiVersion","7.2-preview"));
        int id = Integer.parseInt(Objects.toString(args.get("id"))); 
        boolean destroy = Boolean.TRUE.equals(args.get("destroy"));
        Map<String,String> query = new LinkedHashMap<>();
        query.put("api-version", apiVersion);
        if (destroy) query.put("destroy", "true");
        return client.deleteWitApiWithQuery(project, null, "workitems/"+id, query, apiVersion);
    }
}
