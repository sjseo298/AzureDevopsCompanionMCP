package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import java.util.*;

/**
 * Helper para búsqueda de queries y envío de correo en Azure DevOps.
 */
public class WitQueriesMailHelper {
    private final AzureDevOpsClientService azureService;

    public WitQueriesMailHelper(AzureDevOpsClientService svc) {
        this.azureService = svc;
    }

    // --- Search Queries ---
    public void validateSearchText(Object searchText) {
        if (searchText == null || searchText.toString().trim().isEmpty()) throw new IllegalArgumentException("'searchText' es requerido");
    }

    public Map<String,Object> buildSearchBody(Object searchText, Object expand, Object top) {
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("searchText", searchText.toString());
        if (expand != null) body.put("$expand", expand.toString());
        if (top != null) body.put("top", Integer.valueOf(top.toString()));
        return body;
    }
    public Map<String,String> buildSearchQuery(Object searchText, Object expand, Object top) {
        Map<String,String> query = new LinkedHashMap<>();
        query.put("searchText", searchText.toString());
        if (expand != null) query.put("$expand", expand.toString());
        if (top != null) query.put("top", top.toString());
        return query;
    }
    public Map<String,Object> postSearch(String project, String team, Map<String,Object> body) {
        return azureService.postWitApi(project, team, "queries/$search", body, "7.2-preview");
    }
    public Map<String,Object> getSearch(String project, String team, Map<String,String> query) {
        return azureService.getWitApiWithQuery(project, team, "queries", query, "7.2-preview");
    }
    public boolean looksLikeUnsupportedSearchEndpoint(Map<String,Object> resp) {
        if (resp == null || resp.isEmpty()) return false;
        Object typeKey = resp.get("typeKey");
        Object message = resp.get("message");
        if (typeKey != null && "VssPropertyValidationException".equals(typeKey.toString()) && message != null) {
            String msg = message.toString();
            return msg.contains("Parameter name: Name") || msg.matches("(?s).*Parameter name: (Wiql|isFolder).*");
        }
        Object isHttpErr = resp.get("isHttpError");
        if (Boolean.TRUE.equals(isHttpErr)) {
            Object innerType = resp.get("typeKey");
            Object innerMsg = resp.get("message");
            if (innerType != null && "VssPropertyValidationException".equals(innerType.toString()) && innerMsg != null) {
                String m = innerMsg.toString();
                return m.contains("Parameter name: Name") || m.matches("(?s).*Parameter name: (Wiql|isFolder).*");
            }
        }
        return false;
    }
    public String formatSearchResponse(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Sin resultados)";
        Object val = data.get("value");
        if (val instanceof List) {
            List<?> list = (List<?>) val;
            if (list.isEmpty()) return "(Sin resultados)";
            StringBuilder sb = new StringBuilder("=== Queries encontradas ===\n\n");
            int i=1;
            for (Object o : list) {
                if (o instanceof Map) {
                    Map<?,?> m = (Map<?,?>) o;
                    Object id = m.get("id");
                    Object name = m.get("name");
                    sb.append(i++).append(") ")
                      .append(name != null ? name : "(sin nombre)")
                      .append(" [").append(id != null ? id : "?").append("]\n");
                }
            }
            return sb.toString();
        }
        return data.toString();
    }

    // --- Send Mail ---
    public void validateMailParams(Map<String,Object> args) {
        for (String r : List.of("to","subject","body","workItemIds")) {
            Object v = args.get(r);
            if (v == null || v.toString().trim().isEmpty()) throw new IllegalArgumentException("Parámetro obligatorio faltante: " + r);
        }
    }
    public Map<String,Object> buildMailPayload(Map<String,Object> args) {
        Map<String,Object> payload = new LinkedHashMap<>();
        Map<String,Object> message = new LinkedHashMap<>();
        message.put("to", splitCsv(args.get("to")));
        if (has(args.get("cc"))) message.put("cc", splitCsv(args.get("cc")));
        if (has(args.get("replyTo"))) message.put("replyTo", splitCsv(args.get("replyTo")));
        message.put("subject", args.get("subject"));
        message.put("body", args.get("body"));
        payload.put("message", message);
        payload.put("workItemIds", toIntList(args.get("workItemIds")));
        if (has(args.get("reason"))) payload.put("reason", args.get("reason"));
        return payload;
    }
    public Map<String,Object> sendMail(String project, Map<String,Object> payload) {
        return azureService.postWitApi(project, null, "sendmail", payload, "7.2-preview.1");
    }
    public String formatMailResponse(Map<String,Object> resp) {
        Object status = resp.get("status");
        return status != null ? ("Estado: "+status) : "Correo enviado (revisar status).";
    }

    // --- Utilidades ---
    private boolean has(Object o){ return o!=null && !o.toString().trim().isEmpty(); }
    private List<String> splitCsv(Object csvObj){
        if (csvObj == null) return List.of();
        String csv = csvObj.toString();
        List<String> l=new ArrayList<>();
        for(String p: csv.split(",")){
            String t=p.trim(); if(!t.isEmpty()) l.add(t);
        }
        return l;
    }
    private List<Integer> toIntList(Object csvObj){
        if (csvObj == null) return List.of();
        String csv = csvObj.toString();
        List<Integer> l=new ArrayList<>();
        for(String p: csv.split(",")){
            String t=p.trim(); if(t.matches("\\d+")) l.add(Integer.parseInt(t));
        }
        return l;
    }
}
