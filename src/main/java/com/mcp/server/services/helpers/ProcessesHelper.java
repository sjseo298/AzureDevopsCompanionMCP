package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helper para operaciones de procesos (metodologías) de Azure DevOps Core.
 */
@Service
public class ProcessesHelper {

    private final AzureDevOpsClientService azureService;

    public ProcessesHelper(AzureDevOpsClientService azureService) {
        this.azureService = azureService;
    }

    public void validateProcessId(String processId) {
        if (processId == null || processId.trim().isEmpty()) {
            throw new IllegalArgumentException("'processId' es requerido");
        }
    }

    public Map<String,String> buildGetProcessQuery() {
        Map<String,String> q = new LinkedHashMap<>();
        q.put("api-version","7.2-preview.1");
        return q;
    }

    public Map<String,Object> fetchProcess(String processId) {
        return azureService.getCoreApi("process/processes/"+processId.trim(), buildGetProcessQuery());
    }

    public String formatProcessResponse(Map<String,Object> resp) {
        if (resp.containsKey("id") || resp.containsKey("name")) {
            return String.format("%s [%s]", String.valueOf(resp.getOrDefault("name","<sin nombre>")), String.valueOf(resp.getOrDefault("id","?")));
        }
        return null; // fallback raw
    }

    // ---- List Processes ----

    public Map<String,String> buildListProcessesQuery() { return buildGetProcessQuery(); }

    public Map<String,Object> fetchProcesses() {
        return azureService.getCoreApi("process/processes", buildListProcessesQuery());
    }

    @SuppressWarnings("unchecked")
    public String formatProcessesList(Map<String,Object> resp) {
        Object count = resp.get("count"); Object value = resp.get("value");
        if (!(count instanceof Number) || !(value instanceof java.util.List)) return null;
        java.util.List<java.util.Map<String,Object>> items = (java.util.List<java.util.Map<String,Object>>) value;
        if (items.isEmpty()) return "Sin resultados";
        StringBuilder sb = new StringBuilder(); int i=1; for (java.util.Map<String,Object> it: items) {
            sb.append(i++).append(") ")
              .append(String.valueOf(it.getOrDefault("name","<sin nombre>")))
              .append(" [").append(String.valueOf(it.getOrDefault("id","?"))).append("]\n");
        }
        return sb.toString();
    }
}
