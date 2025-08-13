package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;
import java.util.*;

/** Helper para listar múltiples work items por IDs replicando script work_items_list.sh */
@Component
public class WitWorkItemsListHelper {
    private final AzureDevOpsClientService client;
    public WitWorkItemsListHelper(AzureDevOpsClientService client) { this.client = client; }

    public Map<String,Object> list(Map<String,Object> args) {
    String project = Objects.toString(args.get("project"),""); // puede estar vacío (nivel organización)
        String ids = Objects.toString(args.get("ids"),"").trim();
        String fields = opt(args,"fields");
        String expand = opt(args,"expand");
        String asOf = opt(args,"asOf");
        Object apiVerAlias = args.get("api-version");
        if (apiVerAlias != null && !args.containsKey("apiVersion")) args.put("apiVersion", apiVerAlias);
        String apiVersion = Objects.toString(args.getOrDefault("apiVersion","7.2-preview"));
        if (ids.isEmpty()) throw new IllegalArgumentException("'ids' es requerido");
        Map<String,String> q = new LinkedHashMap<>();
        q.put("ids", ids);
        if (fields != null && !fields.isBlank()) q.put("fields", fields);
        if (expand != null && !expand.isBlank()) q.put("expand", expand);
        if (asOf != null && !asOf.isBlank()) q.put("asOf", asOf);
        q.put("api-version", apiVersion);
        return client.getWitApiWithQuery(project, null, "workitems", q, apiVersion);
    }

    private String opt(Map<String,Object> m, String k) { Object v = m.get(k); return v==null? null : v.toString(); }
}
