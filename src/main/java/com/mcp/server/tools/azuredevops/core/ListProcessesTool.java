package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.ProcessesHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ListProcessesTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_core_list_processes";
    private static final String DESC = "Lista procesos/metodologías disponibles en la organización";

    private final ProcessesHelper processesHelper;

    @Autowired
    public ListProcessesTool(AzureDevOpsClientService service, ProcessesHelper processesHelper) {
        super(service);
        this.processesHelper = processesHelper;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        // No requiere 'project' ni otros parámetros
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of("type","object","properties", Map.of(), "required", List.of());
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        Map<String,Object> resp = processesHelper.fetchProcesses();
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        String formatted = processesHelper.formatProcessesList(resp);
        if (formatted != null) return success(formatted);
        return Map.of("isError", false, "raw", resp);
    }
}
