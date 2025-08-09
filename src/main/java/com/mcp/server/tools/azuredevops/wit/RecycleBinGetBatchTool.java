package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tool MCP: azuredevops_wit_recyclebin_get_batch
 * Obtiene varios work items eliminados por lista de IDs.
 * Endpoint: GET /{project}/_apis/wit/recyclebin?ids=1,2,3&api-version=7.2-preview
 */
public class RecycleBinGetBatchTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_recyclebin_get_batch";
    private static final String DESC = "Obtiene varios work items eliminados (Recycle Bin) por IDs.";

    public RecycleBinGetBatchTool(AzureDevOpsClientService svc) { super(svc); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String,Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String,Object>) base.get("properties");
        props.put("ids", Map.of("type","string","description","Lista de IDs separados por coma"));
        @SuppressWarnings("unchecked") List<String> req = (List<String>) base.get("required");
        if (!req.contains("ids")) req.add("ids");
        return base;
    }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        super.validateCommon(args);
        if (args.get("ids") == null || args.get("ids").toString().trim().isEmpty()) throw new IllegalArgumentException("'ids' es requerido");
    }

    @Override
    protected Map<String,Object> executeInternal(Map<String,Object> args) {
        String project = getProject(args);
        String idsRaw = args.get("ids").toString().trim();
        List<String> ids = Arrays.stream(idsRaw.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        if (ids.isEmpty()) return success("Lista de IDs vacía");
        Map<String,String> query = new LinkedHashMap<>();
        query.put("ids", String.join(",", ids));
        Map<String,Object> resp = azureService.getWitApiWithQuery(project,null,"recyclebin", query, "7.2-preview");
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        Object count = resp.get("count");
        @SuppressWarnings("unchecked") List<Map<String,Object>> value = (List<Map<String,Object>>) resp.get("value");
        if (value == null) return success("Sin resultados");
        StringBuilder sb = new StringBuilder();
        sb.append("Batch eliminados: ").append(count != null ? count : value.size()).append('\n');
        for (Map<String,Object> v : value) {
            sb.append("ID=").append(v.get("id"));
            Object name = v.get("name"); if (name != null) sb.append(" | name=").append(name);
            sb.append('\n');
        }
        return success(sb.toString());
    }
}
