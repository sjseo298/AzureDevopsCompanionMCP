package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_reporting_workitemlinks_get
 * Obtiene enlaces (work item links) para reporting con filtros y paginación.
 * Endpoint: GET /{project}/_apis/wit/reporting/workitemlinks?api-version=7.2-preview.3
 */
public class ReportingWorkItemLinksGetTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_reporting_workitemlinks_get";
    private static final String DESC = "Obtiene vínculos entre work items (reporting) con filtros y continuationToken.";
    private static final String API_VERSION = "7.2-preview.3";

    public ReportingWorkItemLinksGetTool(AzureDevOpsClientService svc) { super(svc); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String,Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String,Object>) base.get("properties");
        props.put("linkTypes", Map.of("type","string","description","Lista separada por comas de tipos de vínculo"));
        props.put("types", Map.of("type","string","description","Lista separada por comas de tipos de work item"));
        props.put("startDateTime", Map.of("type","string","description","ISO8601 startDateTime"));
        props.put("continuationToken", Map.of("type","string","description","Token para siguiente lote"));
        return base; // project requerido según convención interna
    }

    @Override
    protected Map<String,Object> executeInternal(Map<String,Object> args) {
        String project = getProject(args);
        String linkTypes = opt(args, "linkTypes");
        String types = opt(args, "types");
        String start = opt(args, "startDateTime");
        String cont = opt(args, "continuationToken");

        Map<String,String> query = new LinkedHashMap<>();
        if (linkTypes != null) query.put("linkTypes", linkTypes);
        if (types != null) query.put("types", types);
        if (start != null) query.put("startDateTime", start);
        if (cont != null) query.put("continuationToken", cont);

        Map<String,Object> resp = azureService.getWitApiWithQuery(project,null,"reporting/workitemlinks", query.isEmpty()? null : query, API_VERSION);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);

        @SuppressWarnings("unchecked") List<Map<String,Object>> values = (List<Map<String,Object>>) resp.get("values");
        Object isLast = resp.get("isLastBatch");
        Object nextLink = resp.get("nextLink");

        StringBuilder sb = new StringBuilder();
        if (values == null) {
            sb.append("Sin datos.");
        } else {
            sb.append("Links recibidos: ").append(values.size()).append('\n');
            int max = Math.min(values.size(), 10);
            for (int i=0;i<max;i++) {
                Map<String,Object> v = values.get(i);
                sb.append(i+1).append(") ")
                  .append(v.get("rel"))
                  .append(" ")
                  .append(v.get("sourceId"))
                  .append(" -> ")
                  .append(v.get("targetId"));
                Object changed = v.get("changedDate");
                if (changed != null) sb.append(" | ").append(changed);
                sb.append('\n');
            }
            if (values.size() > max) sb.append("... ("+(values.size()-max)+" más)\n");
        }
        if (isLast != null) sb.append("isLastBatch=").append(isLast).append('\n');
        if (nextLink != null) sb.append("Tiene siguiente lote (usar continuationToken extraído del nextLink).\n");
        return success(sb.toString());
    }

    private String opt(Map<String,Object> m, String k) {
        Object v = m.get(k);
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty()? null : s;
    }
}
