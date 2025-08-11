package com.mcp.server.tools.azuredevops.wit;


import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitQueriesMailHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_send_mail
 * Envía un correo relacionado a work items (POST sendmail).
 * Endpoint: POST /{project}/_apis/wit/sendmail?api-version=7.2-preview.1
 */
@Component
public class SendMailTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_send_mail";
    private static final String DESC = "Envía un correo sobre uno o más work items.";
    private static final String API_VERSION = "7.2-preview.1";

    private final WitQueriesMailHelper helper;

    public SendMailTool(AzureDevOpsClientService svc) {
        super(svc);
        this.helper = new WitQueriesMailHelper(svc);
    }
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
        try {
            helper.validateMailParams(args);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
        Map<String,Object> payload = helper.buildMailPayload(args);
        Map<String,Object> resp = helper.sendMail(project, payload);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        return success(helper.formatMailResponse(resp));
    }
}
