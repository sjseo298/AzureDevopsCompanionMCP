package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Helper para obtener un work item individual por ID (GET workitems/{id}) */
@Component
public class WitWorkItemGetHelper {
    private final AzureDevOpsClientService client;
    public WitWorkItemGetHelper(AzureDevOpsClientService client) { this.client = client; }

    public Map<String,Object> get(Map<String,Object> args) {
        String project = Objects.toString(args.get("project"), "").trim();
        // Si project está vacío, usar null para consulta a nivel organizacional
        if (project.isEmpty()) project = null;
        
        String idStr = Objects.toString(args.get("id"), "").trim();
        if (idStr.isEmpty()) throw new IllegalArgumentException("'id' es requerido");

        String fields = opt(args, "fields");
        String expand = opt(args, "expand");
        String asOf = opt(args, "asOf");
        Object apiVerAlias = args.get("api-version");
        if (apiVerAlias != null && !args.containsKey("apiVersion")) args.put("apiVersion", apiVerAlias);
        String apiVersion = Objects.toString(args.getOrDefault("apiVersion", "7.2-preview"));

        Map<String,String> q = new LinkedHashMap<>();
        if (fields != null && !fields.isBlank()) q.put("fields", fields);
        // Nota: En scripts cURL se usa $expand; en helpers mantenemos 'expand' por consistencia interna
        if (expand != null && !expand.isBlank()) q.put("expand", expand);
        if (asOf != null && !asOf.isBlank()) q.put("asOf", asOf);
        q.put("api-version", apiVersion);

        String path = "workitems/" + idStr;
        return client.getWitApiWithQuery(project, null, path, q, apiVersion);
    }

    /** Compatibilidad: método usado por otros helpers (project puede ser "" para nivel org). */
    public Map<String,Object> getWorkItem(String project, int id, String fields, String apiVersion) {
        String proj = project == null ? "" : project.trim();
        String ver = (apiVersion == null || apiVersion.isBlank()) ? "7.2-preview" : apiVersion;
        java.util.Map<String,String> q = new java.util.LinkedHashMap<>();
        if (fields != null && !fields.isBlank()) q.put("fields", fields);
        q.put("api-version", ver);
        return client.getWitApiWithQuery(proj, null, "workitems/" + id, q, ver);
    }

    public Map<String,Object> getWorkItem(String project, Integer id, String fields, String apiVersion) {
        if (id == null) throw new IllegalArgumentException("id es requerido");
        return getWorkItem(project, id.intValue(), fields, apiVersion);
    }

    private String opt(Map<String,Object> m, String k) { Object v = m.get(k); return v==null? null : v.toString(); }
}
