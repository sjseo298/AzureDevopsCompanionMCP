package com.mcp.server.tools.azuredevops.wit;


import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitWorkItemsListHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.stereotype.Component;

import java.util.*;

/** Tool: azuredevops_wit_work_items_list (GET multiple by ids) */
@Component
public class WorkItemsListTool extends AbstractAzureDevOpsTool {
    private final WitWorkItemsListHelper helper;
    private static final String NAME = "azuredevops_wit_work_items_list";
    private static final String DESC = "Lista múltiples work items por IDs (fields, expand, asOf)";
    private static final String DEF_VER = "7.2-preview";
    public WorkItemsListTool(AzureDevOpsClientService svc) { super(svc); this.helper = new WitWorkItemsListHelper(svc); }
    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String,Object> getInputSchema() {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("project", Map.of("type","string","description","Proyecto"));
        props.put("ids", Map.of("type","string","description","Lista de IDs separados por coma (máx 200)"));
        props.put("fields", Map.of("type","string","description","Campos referenceName separados por coma"));
        props.put("expand", Map.of("type","string","description","None|Relations|Links|All"));
        props.put("asOf", Map.of("type","string","description","Fecha/hora ISO"));
        props.put("apiVersion", Map.of("type","string","description","Versión API","default", DEF_VER));
        props.put("api-version", Map.of("type","string","description","Alias (script) de apiVersion"));
        props.put("raw", Map.of("type","boolean","description","Devuelve JSON crudo"));
        return Map.of("type","object","properties",props,"required",List.of("project","ids"));
    }

    @Override
    protected Map<String,Object> executeInternal(Map<String,Object> args) {
        String project = Objects.toString(args.get("project"),"");
        if (project.isEmpty()) return error("project requerido");
        if (!args.containsKey("apiVersion") && args.containsKey("api-version")) args.put("apiVersion", args.get("api-version"));
        try {
            Map<String,Object> resp = helper.list(args);
            String remoteErr = tryFormatRemoteError(resp);
            boolean raw = Boolean.TRUE.equals(args.get("raw"));
            if (raw) return Map.of("isError", false, "raw", resp);
            if (remoteErr != null) return success(remoteErr);
            Object value = resp.get("value");
            if (!(value instanceof List<?> list)) return success("Sin lista de work items en respuesta");
            StringBuilder sb = new StringBuilder();
            sb.append("Work items obtenidos: ").append(list.size());
            int idx=1;
            for (Object o : list) {
                if (!(o instanceof Map<?,?> m)) continue;
                Object id = m.get("id");
                Object rev = m.get("rev");
                String title = null; String state = null;
                Object fields = m.get("fields");
                if (fields instanceof Map<?,?> fm) {
                    Object t = fm.get("System.Title"); if (t!=null) title = t.toString();
                    Object st = fm.get("System.State"); if (st!=null) state = st.toString();
                }
                sb.append("\n").append(idx++).append(") ID ").append(id!=null?id:"?");
                if (rev != null) sb.append(" Rev ").append(rev);
                if (title != null) sb.append(" | ").append(title);
                if (state != null) sb.append(" [").append(state).append("]");
            }
            return success(sb.toString());
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
