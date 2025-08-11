package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import com.mcp.server.services.helpers.WitAttachmentsHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.Base64;

/**
 * Tool MCP: azuredevops_wit_attachments_get
 * Descarga un adjunto (binario en base64) por ID. Org-level.
 * Endpoint: GET /_apis/wit/attachments/{id}?download=true (api-version 7.2-preview)
 */
@Component
public class AttachmentsGetTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_attachments_get";
    private static final String DESC = "Obtiene un adjunto por ID (devuelve base64 y contentType).";

    private final WitAttachmentsHelper attachmentsHelper;

    @Autowired
    public AttachmentsGetTool(AzureDevOpsClientService service, WitAttachmentsHelper attachmentsHelper) {
        super(service);
        this.attachmentsHelper = attachmentsHelper;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        attachmentsHelper.validateGet(Objects.toString(args.get("id"), null));
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("id", Map.of("type","string","description","GUID del adjunto"));
        return Map.of("type","object","properties", props, "required", List.of("id"));
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        String id = arguments.get("id").toString();
        Map<String,Object> resp = attachmentsHelper.getAttachment(id);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        String formatted = attachmentsHelper.formatGetResponse(resp);
        if (formatted != null) return success(formatted);
        return Map.of("isError", false, "raw", resp);
    }
}
