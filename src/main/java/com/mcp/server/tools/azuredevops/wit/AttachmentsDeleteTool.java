package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_attachments_delete
 * Elimina un adjunto por ID. Org-level.
 * Endpoint: DELETE /_apis/wit/attachments/{id}
 */
@Component
public class AttachmentsDeleteTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_attachments_delete";
    private static final String DESC = "Elimina permanentemente un adjunto por ID.";

    @Autowired
    public AttachmentsDeleteTool(AzureDevOpsClientService service) { super(service); }

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
        Map<String,Object> resp = azureService.deleteCoreApi("wit/attachments/"+id, null, null);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return success("Adjunto eliminado (si existía)");
    }

    private String opt(Map<String,Object> m, String k) {
        Object v = m.get(k);
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty()? null : s;
    }
}
