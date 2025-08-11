package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.ProcessesHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetProcessTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_core_get_process";
    private static final String DESC = "Obtiene un proceso (metodología) por ID";

    private final ProcessesHelper processesHelper;

    @Autowired
    public GetProcessTool(AzureDevOpsClientService service, ProcessesHelper processesHelper) {
        super(service);
        this.processesHelper = processesHelper;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        processesHelper.validateProcessId(
            Optional.ofNullable(args.get("processId")).map(Object::toString).orElse(null)
        );
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
        Map<String,Object> resp = processesHelper.fetchProcess(arguments.get("processId").toString());
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        String formatted = processesHelper.formatProcessResponse(resp);
        if (formatted != null) return success(formatted);
        return Map.of("isError", false, "raw", resp);
    }
}
