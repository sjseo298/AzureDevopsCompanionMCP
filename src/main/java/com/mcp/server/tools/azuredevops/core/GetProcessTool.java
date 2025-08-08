package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetProcessTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_core_get_process";
    private static final String DESC = "Obtiene un proceso (metodología) por ID";

    @Autowired
    public GetProcessTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        String id = Optional.ofNullable(args.get("processId")).map(Object::toString).map(String::trim).orElse("");
        if (id.isEmpty()) throw new IllegalArgumentException("'processId' es requerido");
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type","object",
            "properties", Map.of("processId", Map.of("type","string","description","ID del proceso")),
            "required", List.of("processId")
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        String pid = arguments.get("processId").toString().trim();
        Map<String,String> q = new LinkedHashMap<>(); q.put("api-version","7.2-preview.1");
        Map<String,Object> resp = azureService.getCoreApi("process/processes/"+pid, q);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        if (resp.containsKey("id") || resp.containsKey("name")) {
            return success(String.format("%s [%s]", String.valueOf(resp.getOrDefault("name","<sin nombre>")), String.valueOf(resp.getOrDefault("id","?"))));
        }
        return Map.of("isError", false, "raw", resp);
    }
}
