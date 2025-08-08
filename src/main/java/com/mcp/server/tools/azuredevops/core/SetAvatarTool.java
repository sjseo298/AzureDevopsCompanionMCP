package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.Base64;

@Component
public class SetAvatarTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_core_set_avatar";
    private static final String DESC = "Actualiza el avatar de un subjectDescriptor (data base64 y contentType)";

    @Autowired
    public SetAvatarTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type","object",
            "properties", Map.of(
                "subjectDescriptor", Map.of("type","string","description","Descriptor del sujeto (Graph)"),
                "dataBase64", Map.of("type","string","description","Imagen codificada en base64"),
                "contentType", Map.of("type","string","description","MIME type (image/png, image/jpeg)")
            ),
            "required", List.of("subjectDescriptor","dataBase64")
        );
    }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        String sub = Optional.ofNullable(args.get("subjectDescriptor")).map(Object::toString).map(String::trim).orElse("");
        String data = Optional.ofNullable(args.get("dataBase64")).map(Object::toString).map(String::trim).orElse("");
        if (sub.isEmpty() || data.isEmpty()) throw new IllegalArgumentException("'subjectDescriptor' y 'dataBase64' son requeridos");
        // Validación básica base64
        try { Base64.getDecoder().decode(data); } catch (IllegalArgumentException e) { throw new IllegalArgumentException("'dataBase64' no es base64 válido"); }
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        String sub = arguments.get("subjectDescriptor").toString().trim();
        String dataB64 = arguments.get("dataBase64").toString().trim();
        byte[] bytes = Base64.getDecoder().decode(dataB64);
        String ctStr = Optional.ofNullable(arguments.get("contentType")).map(Object::toString).filter(s -> !s.isBlank()).orElse("image/png");
        MediaType ct = MediaType.parseMediaType(ctStr);
        Map<String,Object> resp = azureService.putVsspsBinary("graph/avatars/"+sub+"?api-version=7.2-preview.1", bytes, ct);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return Map.of("isError", false, "raw", resp);
    }
}
