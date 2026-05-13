package com.mcp.server.tools.azuredevops.wit;


import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import com.mcp.server.services.helpers.WitAttachmentsHelper;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

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
        String id = resolveAttachmentId(args);
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("'id' es requerido (también se acepta 'attachmentId')");
        }
        attachmentsHelper.validateGet(id);
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("id", Map.of("type","string","description","GUID del adjunto"));
        props.put("attachmentId", Map.of("type","string","description","Alias de 'id' para compatibilidad con router"));
        props.put("includeBase64", Map.of("type", "boolean", "description", "Incluye el contenido en base64 (default: true)"));
        props.put("maxBase64Chars", Map.of("type", "integer", "description", "Límite máximo de caracteres base64 devueltos (default: 200000)"));
        props.put("includeTextPreview", Map.of("type", "boolean", "description", "Incluye preview UTF-8 para content-types de texto (default: true)"));
        props.put("maxTextChars", Map.of("type", "integer", "description", "Límite de caracteres para textPreview (default: 8000)"));
        props.put("outputPath", Map.of("type", "string", "description", "Ruta local donde guardar el archivo descargado (opcional)"));
        return Map.of("type","object","properties", props, "required", List.of("id"));
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        String id = resolveAttachmentId(arguments);
        if (id == null || id.isBlank()) {
            return error("Parámetros inválidos: 'id' es requerido (también se acepta 'attachmentId')");
        }
        boolean includeBase64 = parseBoolean(arguments.get("includeBase64"), true);
        Integer maxBase64Chars = parsePositiveInt(arguments.get("maxBase64Chars"));
        boolean includeTextPreview = parseBoolean(arguments.get("includeTextPreview"), true);
        Integer maxTextChars = parsePositiveInt(arguments.get("maxTextChars"));
        String outputPath = arguments.get("outputPath") == null ? null : arguments.get("outputPath").toString();

        Map<String,Object> resp = attachmentsHelper.getAttachment(id);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);

        Map<String,Object> payload = attachmentsHelper.buildGetResponse(
                id,
                resp,
                includeBase64,
                maxBase64Chars,
                includeTextPreview,
                maxTextChars,
                outputPath
        );
        return rawSuccess(payload);
    }

    private boolean parseBoolean(Object value, boolean defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(value.toString());
    }

    private Integer parsePositiveInt(Object value) {
        if (value == null) return null;
        try {
            int n = Integer.parseInt(value.toString());
            return n > 0 ? n : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveAttachmentId(Map<String, Object> arguments) {
        Object id = arguments.get("id");
        if (id != null && !id.toString().trim().isEmpty()) {
            return id.toString().trim();
        }
        Object attachmentId = arguments.get("attachmentId");
        if (attachmentId != null && !attachmentId.toString().trim().isEmpty()) {
            return attachmentId.toString().trim();
        }
        return null;
    }
}
