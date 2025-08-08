package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetAvatarTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_core_get_avatar";
    private static final String DESC = "Obtiene avatar (binario) de un subjectDescriptor de Graph";

    @Autowired
    public GetAvatarTool(AzureDevOpsClientService service) { super(service); }

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
        String sd = Optional.ofNullable(args.get("subjectDescriptor")).map(Object::toString).map(String::trim).orElse("");
        if (sd.isEmpty()) throw new IllegalArgumentException("'subjectDescriptor' es requerido");
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        String sd = arguments.get("subjectDescriptor").toString().trim();
        // Usar VSSPS binario para Avatars, versión preview
        Map<String,Object> resp = azureService.getVsspsBinary("graph/avatars/"+sd+"?api-version=7.2-preview.1");
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return Map.of("isError", false, "raw", resp);
    }
}
