package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.http.MediaType;

import java.util.*;

/** Helper para invocar POST workitemsbatch replicando script work_items_batch.sh */
public class WitWorkItemsBatchHelper {
    private final AzureDevOpsClientService client;
    public WitWorkItemsBatchHelper(AzureDevOpsClientService client) { this.client = client; }

    /**
     * Parámetros soportados (igual script): project, ids (req), fields, asOf, errorPolicy (Omit|Fail), apiVersion/api-version
     */
    public Map<String,Object> batch(Map<String,Object> args) {
        String project = Objects.toString(args.get("project"), "").trim();
        if (project.isEmpty()) throw new IllegalArgumentException("project requerido");
        Object apiVerAlias = args.get("api-version");
        if (apiVerAlias != null && !args.containsKey("apiVersion")) args.put("apiVersion", apiVerAlias);
        String apiVersion = Objects.toString(args.getOrDefault("apiVersion", "7.2-preview"));
        String idsStr = Objects.toString(args.get("ids"), "").trim();
        if (idsStr.isEmpty()) throw new IllegalArgumentException("ids requerido");
        String fields = opt(args, "fields");
        String asOf = opt(args, "asOf");
        String errorPolicy = opt(args, "errorPolicy");
        if (errorPolicy == null || errorPolicy.isBlank()) errorPolicy = "Omit"; // default script

        List<Integer> ids = new ArrayList<>();
        for (String tok : idsStr.split(",")) {
            String t = tok.trim(); if (t.isEmpty()) continue; if (!t.matches("^\\d+$")) continue; ids.add(Integer.valueOf(t));
            if (ids.size() >= 200) break; // límite script
        }
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("ids", ids);
        if (fields != null && !fields.isBlank()) {
            List<String> fl = new ArrayList<>();
            for (String f : fields.split(",")) { String ff = f.trim(); if (!ff.isEmpty()) fl.add(ff); }
            if (!fl.isEmpty()) body.put("fields", fl);
        }
        if (asOf != null && !asOf.isBlank()) body.put("asOf", asOf);
        body.put("errorPolicy", errorPolicy);

        Map<String,String> query = new LinkedHashMap<>();
        query.put("api-version", apiVersion);
        return client.postWitApiWithQuery(project, null, "workitemsbatch", query, body, apiVersion, MediaType.APPLICATION_JSON);
    }

    private String opt(Map<String,Object> m, String k) { Object v = m.get(k); return v==null? null : v.toString(); }
}
