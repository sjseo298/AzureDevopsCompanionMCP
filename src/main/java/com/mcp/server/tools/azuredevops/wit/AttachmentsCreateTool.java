package com.mcp.server.tools.azuredevops.wit;


import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import com.mcp.server.services.helpers.WitAttachmentsHelper;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.Base64;

/**
 * Tool MCP: azuredevops_wit_attachments_create
 * Crea un adjunto (org-level) enviando binario (base64) con fileName.
 * Endpoint: POST /_apis/wit/attachments?fileName=... (api-version 7.2-preview requerido en esta org)
 */
@Component
public class AttachmentsCreateTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_attachments_create";
    private static final String DESC = "Crea un adjunto enviando dataBase64 y fileName (nivel organización).";

    private final WitAttachmentsHelper attachmentsHelper;

    @Autowired
    public AttachmentsCreateTool(AzureDevOpsClientService service, WitAttachmentsHelper attachmentsHelper) {
        super(service);
        this.attachmentsHelper = attachmentsHelper;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        attachmentsHelper.validateCreate(
            Objects.toString(args.get("fileName"), null),
            Objects.toString(args.get("dataBase64"), null)
        );
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("fileName", Map.of("type","string","description","Nombre de archivo del adjunto"));
        props.put("dataBase64", Map.of("type","string","description","Contenido del adjunto en base64"));
        props.put("contentType", Map.of("type","string","description","MIME type. Por defecto application/octet-stream"));
        return Map.of(
            "type","object",
            "properties", props,
            "required", List.of("fileName","dataBase64")
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        String fileName = arguments.get("fileName").toString();
        String dataB64 = arguments.get("dataBase64").toString();
        String ctStr = attachmentsHelper.sanitizeContentType(arguments.get("contentType") == null ? null : arguments.get("contentType").toString());
        byte[] data = attachmentsHelper.decodeBase64(dataB64);
        Map<String,Object> resp = attachmentsHelper.createAttachment(fileName, data, ctStr);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        String formatted = attachmentsHelper.formatCreateResponse(resp);
        if (formatted != null) return success(formatted);
        return Map.of("isError", false, "raw", resp);
    }
}
