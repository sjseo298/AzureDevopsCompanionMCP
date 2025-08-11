package com.mcp.server.tools.azuredevops.wit;


import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitWorkItemDeleteHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.stereotype.Component;
import java.util.*;

/** Tool: azuredevops_wit_work_item_delete (DELETE single) */
@Component
public class WorkItemDeleteTool extends AbstractAzureDevOpsTool {
    private final WitWorkItemDeleteHelper helper;
    private static final String NAME = "azuredevops_wit_work_item_delete";
    private static final String DESC = "Elimina un work item (Recycle Bin por defecto, --destroy para permanente)";
    private static final String DEF_VER = "7.2-preview";
    public WorkItemDeleteTool(AzureDevOpsClientService svc) { super(svc); this.helper = new WitWorkItemDeleteHelper(svc); }
    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override public Map<String,Object> getInputSchema() {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("project", Map.of("type","string","description","Proyecto"));
        props.put("id", Map.of("type","integer","description","ID del work item"));
        props.put("destroy", Map.of("type","boolean","description","Eliminación permanente"));
        props.put("apiVersion", Map.of("type","string","description","Versión API","default",DEF_VER));
        props.put("api-version", Map.of("type","string","description","Alias script apiVersion"));
        props.put("raw", Map.of("type","boolean","description","Devuelve JSON crudo"));
        return Map.of("type","object","properties",props,"required",List.of("project","id"));
    }

    @Override protected Map<String,Object> executeInternal(Map<String,Object> args) {
        if (!args.containsKey("apiVersion") && args.containsKey("api-version")) args.put("apiVersion", args.get("api-version"));
        try {
            Map<String,Object> resp = helper.deleteOne(args);
            boolean raw = Boolean.TRUE.equals(args.get("raw"));
            if (raw) return Map.of("isError", false, "raw", resp);
            String remoteErr = tryFormatRemoteError(resp);
            if (remoteErr != null) return success(remoteErr);
            Object id = resp.get("id");
            return success("Eliminado work item ID " + (id!=null?id:args.get("id")) + (Boolean.TRUE.equals(args.get("destroy"))?" (permanente)":" (recycle bin)"));
        } catch (Exception e) { return error(e.getMessage()); }
    }
}
