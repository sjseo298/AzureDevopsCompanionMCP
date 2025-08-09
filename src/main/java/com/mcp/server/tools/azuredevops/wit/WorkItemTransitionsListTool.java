package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool MCP: azuredevops_wit_work_item_transitions_list (organization-level)
 * Endpoint: GET /_apis/wit/workitemtransitions?ids={ids}&api-version=7.1
 * Lista el siguiente estado (stateOnTransition) para cada work item.
 */
@Component
public class WorkItemTransitionsListTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_work_item_transitions_list";
    private static final String DESC = "Lista el siguiente estado posible (stateOnTransition) por work item ID (organization-level).";
    private static final String API_VERSION = "7.1";

    @Autowired
    public WorkItemTransitionsListTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        // Este endpoint es organization-level: no requiere project, pero mantenemos compat con base schema (project opcional)
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String,Object>) base.get("properties");
        // project lo marcamos como no requerido aquí: remover de required si está
        @SuppressWarnings("unchecked") List<String> required = new ArrayList<>((List<String>) base.get("required"));
        required.remove("project");
        base.put("required", required);
        props.put("ids", Map.of(
            "type","string",
            "description","Lista de IDs separados por coma (obligatorio)"
        ));
        props.put("action", Map.of(
            "type","string",
            "description","Acción opcional (solo 'checkin' soportado oficialmente)"
        ));
        // nueva required list solo con ids
        List<String> newReq = new ArrayList<>(); newReq.add("ids");
        base.put("required", newReq);
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado");
        Object idsObj = arguments.get("ids");
        if (idsObj == null || idsObj.toString().trim().isEmpty()) return error("'ids' es requerido");
        String ids = idsObj.toString().trim();
        String action = arguments.getOrDefault("action", "").toString().trim();

        // Construir query
        Map<String,String> query = new LinkedHashMap<>();
        query.put("ids", ids);
        if (!action.isEmpty()) query.put("action", action);
        query.put("api-version", API_VERSION);

        // Llamar organization-level => usamos getWitApiWithQuery SIN project (pasa null) y path base 'workitemtransitions'
        Map<String,Object> resp = azureService.getWitApiWithQuery(null, null, "workitemtransitions", query, API_VERSION);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        return success(format(resp));
    }

    @SuppressWarnings("unchecked")
    private String format(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Sin datos)";
        Object val = data.get("value");
        if (!(val instanceof List)) return data.toString();
        List<?> list = (List<?>) val;
        if (list.isEmpty()) return "(Sin resultados)";
        StringBuilder sb = new StringBuilder();
        sb.append("Transiciones siguientes ("+list.size()+"):\n");
        int i=1;
        for (Object o : list) {
            if (o instanceof Map) {
                Map<String,Object> m = (Map<String,Object>) o;
                sb.append(i++).append(") ID=").append(m.get("id"))
                  .append(" -> ")
                  .append(m.getOrDefault("stateOnTransition", m.getOrDefault("nextState", "?")));
                if (m.containsKey("errorCode")) {
                    sb.append(" [error=").append(m.get("errorCode"));
                    if (m.get("message") != null) sb.append(": ").append(m.get("message"));
                    sb.append("]");
                }
                sb.append('\n');
                if (i>50) { sb.append("... ("+(list.size()-50)+" más)\n"); break; }
            }
        }
        return sb.toString();
    }
}
