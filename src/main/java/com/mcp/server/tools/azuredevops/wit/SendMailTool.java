package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_send_mail
 * Envía un correo relacionado a work items (POST sendmail).
 * Endpoint: POST /{project}/_apis/wit/sendmail?api-version=7.2-preview.1
 */
public class SendMailTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_send_mail";
    private static final String DESC = "Envía un correo sobre uno o más work items.";
    private static final String API_VERSION = "7.2-preview.1";

    public SendMailTool(AzureDevOpsClientService svc) { super(svc); }
    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String,Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String,Object>) base.get("properties");
        props.put("to", Map.of("type","string","description","Destinatarios principales separados por comas"));
        props.put("cc", Map.of("type","string","description","CC separados por comas"));
        props.put("replyTo", Map.of("type","string","description","ReplyTo separados por comas"));
        props.put("subject", Map.of("type","string","description","Asunto"));
        props.put("body", Map.of("type","string","description","Cuerpo HTML o texto"));
        props.put("workItemIds", Map.of("type","string","description","IDs de work items separados por comas"));
        props.put("reason", Map.of("type","string","description","Motivo opcional"));
        @SuppressWarnings("unchecked") List<String> req = (List<String>) base.get("required");
        for (String r : List.of("to","subject","body","workItemIds")) if (!req.contains(r)) req.add(r);
        return base;
    }

    @Override
    protected Map<String,Object> executeInternal(Map<String,Object> args) {
        String project = getProject(args);
        String to = str(args.get("to"));
        String subject = str(args.get("subject"));
        String body = str(args.get("body"));
        String workItemIds = str(args.get("workItemIds"));
        if (to==null || subject==null || body==null || workItemIds==null) return error("Parámetros obligatorios faltantes");
        Map<String,Object> payload = new LinkedHashMap<>();
        Map<String,Object> message = new LinkedHashMap<>();
        message.put("to", splitCsv(to));
        if (has(args.get("cc"))) message.put("cc", splitCsv(str(args.get("cc"))));
        if (has(args.get("replyTo"))) message.put("replyTo", splitCsv(str(args.get("replyTo"))));
        message.put("subject", subject);
        message.put("body", body);
        payload.put("message", message);
        payload.put("workItemIds", toIntList(workItemIds));
        if (has(args.get("reason"))) payload.put("reason", str(args.get("reason")));

        Map<String,Object> resp = azureService.postWitApi(project,null,"sendmail", payload, API_VERSION);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        Object status = resp.get("status");
        return success(status != null ? ("Estado: "+status) : "Correo enviado (revisar status)." );
    }

    private String str(Object o) { if (o==null) return null; String s=o.toString().trim(); return s.isEmpty()? null : s; }
    private boolean has(Object o){ return str(o)!=null; }
    private List<String> splitCsv(String csv){ List<String> l=new ArrayList<>(); for(String p: csv.split(",")){ String t=p.trim(); if(!t.isEmpty()) l.add(t);} return l; }
    private List<Integer> toIntList(String csv){ List<Integer> l=new ArrayList<>(); for(String p: csv.split(",")){ String t=p.trim(); if(t.matches("\\d+")) l.add(Integer.parseInt(t)); } return l; }
}
