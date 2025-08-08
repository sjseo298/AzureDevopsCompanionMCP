package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
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

    @Autowired
    public AttachmentsGetTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        String id = opt(args, "id");
        if (id == null) throw new IllegalArgumentException("'id' es requerido");
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
        String id = arguments.get("id").toString().trim();
        Map<String,String> query = new LinkedHashMap<>();
        query.put("download","true");
        Map<String,Object> resp = azureService.getCoreBinary("wit/attachments/"+id, query, "7.2-preview");
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        // Respuesta binaria en base64
        String dataB64 = Objects.toString(resp.get("data"), null);
        String ct = Objects.toString(resp.get("contentType"), null);
        if (dataB64 != null) {
            int size = Base64.getDecoder().decode(dataB64).length;
            StringBuilder sb = new StringBuilder("=== Attachment Downloaded ===\n\n");
            if (ct != null) sb.append("Content-Type: ").append(ct).append("\n");
            sb.append("Bytes: ").append(size).append("\n");
            return success(sb.toString());
        }
        return success(resp.toString());
    }

    private String opt(Map<String,Object> m, String k) {
        Object v = m.get(k);
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty()? null : s;
    }
}
