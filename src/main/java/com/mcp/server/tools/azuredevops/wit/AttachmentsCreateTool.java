package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
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

    @Autowired
    public AttachmentsCreateTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        // Org-level. Validar parámetros específicos
        String fileName = opt(args, "fileName");
        String dataB64 = opt(args, "dataBase64");
        if (fileName == null || dataB64 == null) {
            throw new IllegalArgumentException("'fileName' y 'dataBase64' son requeridos");
        }
        try { Base64.getDecoder().decode(dataB64); } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("'dataBase64' no es base64 válido");
        }
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
        String fileName = arguments.get("fileName").toString().trim();
        String dataB64 = arguments.get("dataBase64").toString().trim();
        String ctStr = Optional.ofNullable(arguments.get("contentType")).map(Object::toString).filter(s -> !s.isBlank()).orElse("application/octet-stream");
        byte[] data = Base64.getDecoder().decode(dataB64);

        Map<String,String> query = new LinkedHashMap<>();
        query.put("fileName", fileName);
        Map<String,Object> resp = azureService.postCoreBinary("wit/attachments", query, data, "7.2-preview", MediaType.parseMediaType(ctStr));

        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);

        return success(format(resp));
    }

    private String format(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Respuesta vacía)";
        if (data.containsKey("error")) return "Error remoto: " + data.get("error");
        String id = Objects.toString(data.get("id"), null);
        String url = Objects.toString(data.get("url"), null);
        if (id != null || url != null) {
            StringBuilder sb = new StringBuilder("=== Attachment Created ===\n\n");
            if (id != null) sb.append("ID: ").append(id).append("\n");
            if (url != null) sb.append("URL: ").append(url).append("\n");
            return sb.toString();
        }
        return data.toString();
    }

    private String opt(Map<String,Object> m, String k) {
        Object v = m.get(k);
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty()? null : s;
    }
}
