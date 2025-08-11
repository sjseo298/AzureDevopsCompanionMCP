package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitWorkItemsDeleteListHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import java.util.*;

/** Tool: azuredevops_wit_work_items_delete_list (DELETE multiple) */
public class WorkItemsDeleteListTool extends AbstractAzureDevOpsTool {
    private final WitWorkItemsDeleteListHelper helper;
    private static final String NAME = "azuredevops_wit_work_items_delete_list";
    private static final String DESC = "Elimina múltiples work items (Recycle Bin por defecto, --destroy para permanente)";
    private static final String DEF_VER = "7.2-preview";
    public WorkItemsDeleteListTool(AzureDevOpsClientService svc) { super(svc); this.helper = new WitWorkItemsDeleteListHelper(svc); }
    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override public Map<String,Object> getInputSchema() {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("project", Map.of("type","string","description","Proyecto"));
        props.put("ids", Map.of("type","string","description","Lista IDs separado por coma"));
        props.put("destroy", Map.of("type","boolean","description","Eliminación permanente"));
        props.put("apiVersion", Map.of("type","string","description","Versión API","default",DEF_VER));
        props.put("api-version", Map.of("type","string","description","Alias script apiVersion"));
        props.put("raw", Map.of("type","boolean","description","Devuelve JSON crudo"));
        return Map.of("type","object","properties",props,"required",List.of("project","ids"));
    }

    @Override protected Map<String,Object> executeInternal(Map<String,Object> args) {
        if (!args.containsKey("apiVersion") && args.containsKey("api-version")) args.put("apiVersion", args.get("api-version"));
        try {
            Map<String,Object> resp = helper.deleteList(args);
            boolean raw = Boolean.TRUE.equals(args.get("raw"));
            if (raw) return Map.of("isError", false, "raw", resp);
            String remoteErr = tryFormatRemoteError(resp);
            if (remoteErr != null) return success(remoteErr);
            Object count = resp.get("count");
            return success("Eliminados work items: " + (count!=null?count:"(ver raw)") + (Boolean.TRUE.equals(args.get("destroy"))?" (permanente)":" (recycle bin)"));
        } catch (Exception e) { return error(e.getMessage()); }
    }
}
