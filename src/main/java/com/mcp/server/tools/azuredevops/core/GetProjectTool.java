package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetProjectTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_core_get_project";
    private static final String DESC = "Obtiene la información de un proyecto";

    @Autowired
    public GetProjectTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        String id = Optional.ofNullable(args.get("projectId")).map(Object::toString).map(String::trim).orElse("");
        if (id.isEmpty()) throw new IllegalArgumentException("'projectId' es requerido");
        if (!id.matches("[0-9a-fA-F-]{36}")) throw new IllegalArgumentException("'projectId' debe ser GUID de 36 chars");
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type","object",
            "properties", Map.of("projectId", Map.of("type","string","description","GUID del proyecto")),
            "required", List.of("projectId")
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        String pid = arguments.get("projectId").toString().trim();
        Map<String,String> q = new LinkedHashMap<>(); q.put("api-version","7.2-preview.4");
        Map<String,Object> resp = azureService.getCoreApi("projects/"+pid, q);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return Map.of("isError", false, "raw", resp);
    }
}
