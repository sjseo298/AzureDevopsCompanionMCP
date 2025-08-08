package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ListProcessesTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_core_list_processes";
    private static final String DESC = "Lista procesos/metodologías disponibles en la organización";

    @Autowired
    public ListProcessesTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        // No requiere 'project'
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of("type","object","properties", Map.of(), "required", List.of());
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        Map<String,String> q = new LinkedHashMap<>(); q.put("api-version","7.2-preview.1");
        Map<String,Object> resp = azureService.getCoreApi("process/processes", q);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        Object count = resp.get("count"), value = resp.get("value");
        if (count instanceof Number && value instanceof List) {
            StringBuilder sb = new StringBuilder();
            @SuppressWarnings("unchecked") List<Map<String,Object>> items = (List<Map<String,Object>>) value;
            int i=1; for (Map<String,Object> it: items) {
                sb.append(i++).append(") ")
                  .append(String.valueOf(it.getOrDefault("name","<sin nombre>")))
                  .append(" [").append(String.valueOf(it.getOrDefault("id","?"))).append("]\n");
            }
            if (sb.length()==0) sb.append("Sin resultados");
            return success(sb.toString());
        }
        return Map.of("isError", false, "raw", resp);
    }
}
