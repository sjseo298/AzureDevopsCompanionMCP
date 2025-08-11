package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.AvatarsHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetAvatarTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_core_get_avatar";
    private static final String DESC = "Obtiene avatar (binario) de un subjectDescriptor de Graph";

    private final AvatarsHelper avatarsHelper;

    @Autowired
    public GetAvatarTool(AzureDevOpsClientService service, AvatarsHelper avatarsHelper) {
        super(service);
        this.avatarsHelper = avatarsHelper;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> s = new HashMap<>();
        s.put("type","object");
        s.put("properties", Map.of("subjectDescriptor", Map.of("type","string","description","Descriptor de Graph")));
        s.put("required", List.of("subjectDescriptor"));
        return s;
    }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        avatarsHelper.validateSubjectDescriptor(
            Optional.ofNullable(args.get("subjectDescriptor")).map(Object::toString).orElse(null)
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        Map<String,Object> resp = avatarsHelper.fetchAvatar(arguments.get("subjectDescriptor").toString());
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        String formatted = avatarsHelper.formatAvatarResponse(resp);
        if (formatted != null) return success(formatted);
        return Map.of("isError", false, "raw", resp);
    }
}
