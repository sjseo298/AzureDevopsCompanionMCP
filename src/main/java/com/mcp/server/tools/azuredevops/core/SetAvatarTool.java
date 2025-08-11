package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import com.mcp.server.services.helpers.AvatarsHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.Base64;

@Component
public class SetAvatarTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_core_set_avatar";
    private static final String DESC = "Actualiza el avatar de un subjectDescriptor (data base64 y contentType)";

    private final AvatarsHelper avatarsHelper;

    @Autowired
    public SetAvatarTool(AzureDevOpsClientService service, AvatarsHelper avatarsHelper) {
        super(service);
        this.avatarsHelper = avatarsHelper;
    }

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
        avatarsHelper.validateSetAvatar(
            Optional.ofNullable(args.get("subjectDescriptor")).map(Object::toString).orElse(null),
            Optional.ofNullable(args.get("dataBase64")).map(Object::toString).orElse(null)
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        String sub = arguments.get("subjectDescriptor").toString();
        String dataB64 = arguments.get("dataBase64").toString();
        String ct = avatarsHelper.sanitizeContentType(arguments.get("contentType") == null ? null : arguments.get("contentType").toString());
        byte[] bytes = avatarsHelper.decodeBase64(dataB64);
        Map<String,Object> resp = avatarsHelper.updateAvatar(sub, bytes, ct);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        String formatted = avatarsHelper.formatAvatarResponse(resp);
        if (formatted != null) return success(formatted);
        return Map.of("isError", false, "raw", resp);
    }
}
