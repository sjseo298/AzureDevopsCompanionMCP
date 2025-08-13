package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Helper para operaciones de Behaviors (Work Item Tracking Process) a nivel de organización.
 * Base: /_apis/work/processes/{processId}/behaviors[/{behaviorId}]
 */
@Component
public class WorkitemtrackingprocessBehaviorsHelper {
    private final AzureDevOpsClientService azureService;
    private static final String API_VERSION = "7.2-preview.1";

    public WorkitemtrackingprocessBehaviorsHelper(AzureDevOpsClientService svc) {
        this.azureService = svc;
    }

    public void validateProcessId(Object processId) {
        if (processId == null || processId.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("'processId' es requerido");
        }
    }

    public void validateBehaviorId(Object behaviorId) {
        if (behaviorId == null || behaviorId.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("'behaviorId' es requerido");
        }
    }

    public Map<String,Object> list(String processId) {
        String path = "work/processes/" + processId + "/behaviors";
        Map<String,String> q = new LinkedHashMap<>();
        q.put("api-version", API_VERSION);
        return azureService.getCoreApi(path, q);
    }

    public Map<String,Object> get(String processId, String behaviorId) {
        String path = "work/processes/" + processId + "/behaviors/" + behaviorId;
        Map<String,String> q = new LinkedHashMap<>();
        q.put("api-version", API_VERSION);
        return azureService.getCoreApi(path, q);
    }

    public Map<String,Object> create(String processId, Map<String,Object> body) {
        String path = "work/processes/" + processId + "/behaviors";
        return azureService.postCoreApi(path, null, body != null ? body : Map.of(), API_VERSION);
    }

    public Map<String,Object> update(String processId, String behaviorId, Map<String,Object> body) {
        String path = "work/processes/" + processId + "/behaviors/" + behaviorId;
        return azureService.patchCoreApi(path, null, body != null ? body : Map.of(), API_VERSION);
    }

    public Map<String,Object> delete(String processId, String behaviorId) {
        String path = "work/processes/" + processId + "/behaviors/" + behaviorId;
        return azureService.deleteCoreApi(path, null, API_VERSION);
    }

    public String formatList(Map<String,Object> resp) {
        if (resp == null || resp.isEmpty()) return "(sin datos)";
        StringBuilder sb = new StringBuilder();
        @SuppressWarnings("unchecked") List<Map<String,Object>> value = (List<Map<String,Object>>) resp.get("value");
        int total = value != null ? value.size() : (resp.get("count") instanceof Number ? ((Number)resp.get("count")).intValue() : 0);
        sb.append("Behaviors (").append(total).append("):");
        if (value != null) {
            int i = 1;
            for (Map<String,Object> b : value) {
                String id = Objects.toString(b.get("id"), "");
                String name = Objects.toString(b.get("name"), "(sin nombre)");
                String color = Objects.toString(b.get("color"), "");
                sb.append("\n  ").append(i++).append(") ").append(name);
                if (!id.isEmpty()) sb.append(" [").append(id).append("]");
                if (!color.isEmpty()) sb.append(" color=").append(color);
            }
        }
        return sb.toString();
    }

    public String formatSingle(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(sin datos)";
        StringBuilder sb = new StringBuilder();
        sb.append("Behavior: ").append(Objects.toString(data.get("name"), "(sin nombre)"));
        Object id = data.get("id"); if (id != null) sb.append(" [").append(id).append("]");
        Object color = data.get("color"); if (color != null) sb.append(" | color=").append(color);
        Object description = data.get("description"); if (description != null) sb.append("\nDescripción: ").append(description);
        return sb.toString();
    }
}
