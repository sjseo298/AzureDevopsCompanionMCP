package com.mcp.server.tools.azuredevops.workitemtrackingprocess;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_workitemtrackingprocess_picklists_get
 * Derivado de script scripts/curl/workitemtrackingprocess/picklists_get.sh
 * Operación: Get Picklist (org-level) GET /_apis/work/processes/lists/{id}?api-version=7.2-preview.1
 */
@Component
public class PicklistsGetTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_workitemtrackingprocess_picklists_get";
    private static final String DESC = "Obtiene un picklist (lista de selección) por GUID (nivel organización).";
    private static final String API_VERSION = "7.2-preview.1"; // especificada en la doc local

    @Autowired
    public PicklistsGetTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    // Org-level: no requiere project
    @Override
    protected void validateCommon(Map<String, Object> args) { /* no project required */ }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("id", Map.of("type","string","description","GUID del picklist"));
        props.put("raw", Map.of("type","boolean","description","Si true, devuelve respuesta cruda"));
        return Map.of(
            "type","object",
            "properties", props,
            "required", List.of("id")
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado");
        Object idObj = arguments.get("id");
        if (idObj == null || idObj.toString().trim().isEmpty()) return error("'id' es requerido");
        String id = idObj.toString().trim();
        boolean raw = Boolean.TRUE.equals(arguments.get("raw"));

        // Endpoint org-level usando Core API builder: /_apis/work/processes/lists/{id}
        String path = "work/processes/lists/" + id;
        Map<String,String> q = new LinkedHashMap<>();
        q.put("api-version", API_VERSION);
        Map<String,Object> resp = azureService.getCoreApi(path, q);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        if (raw) {
            return Map.of(
                "isError", false,
                "raw", resp
            );
        }
        return success(format(resp));
    }

    private String format(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(sin datos)";
        StringBuilder sb = new StringBuilder();
        sb.append("Picklist: ").append(data.getOrDefault("name","(sin nombre)"));
        Object id = data.get("id"); if (id != null) sb.append(" [").append(id).append("]");
        Object type = data.get("type"); if (type != null) sb.append(" | type=").append(type);
        @SuppressWarnings("unchecked") List<Object> items = (List<Object>) data.get("items");
        if (items != null) {
            sb.append("\nItems (" ).append(items.size()).append("):");
            int i=1; for (Object it : items) {
                sb.append("\n  ").append(i++).append(") ").append(it);
            }
        }
        return sb.toString();
    }
}
