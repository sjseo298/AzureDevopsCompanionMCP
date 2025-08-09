package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_reporting_workitemrevisionsdiscussions_get
 * Lista discusiones de revisiones de work items.
 * Endpoint: GET /{project}/_apis/wit/reporting/workitemrevisionsdiscussions?api-version=7.2-preview
 */
public class WorkItemRevisionsDiscussionsGetTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_reporting_workitemrevisionsdiscussions_get";
    private static final String DESC = "Lista discusiones de revisiones de work items (si la característica está habilitada).";
    private static final String API_VERSION = "7.2-preview";

    public WorkItemRevisionsDiscussionsGetTool(AzureDevOpsClientService svc) { super(svc); }
    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String,Object> getInputSchema() { return new LinkedHashMap<>(createBaseSchema()); }

    @Override
    protected Map<String,Object> executeInternal(Map<String,Object> args) {
        String project = getProject(args);
        Map<String,Object> resp = azureService.getWitApiWithQuery(project,null,"reporting/workitemrevisionsdiscussions", null, API_VERSION);

        // Detectar endpoint no habilitado similar al script (controller not found o sin payload esperado)
        if (resp == null || resp.isEmpty()) {
            return success("Endpoint no habilitado en esta organización (respuesta vacía): reporting/workitemrevisionsdiscussions");
        }
        Object errorText = resp.get("error");
        if (errorText instanceof String s && s.toLowerCase().contains("controller") && s.toLowerCase().contains("not found")) {
            return success("Endpoint no habilitado en esta organización (controller not found): reporting/workitemrevisionsdiscussions");
        }
        if (Boolean.TRUE.equals(resp.get("isHttpError")) && resp.get("value") == null && resp.get("message") == null) {
            return success("Endpoint no habilitado en esta organización: reporting/workitemrevisionsdiscussions (httpStatus=" + resp.get("httpStatus") + ")");
        }

        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        Object count = resp.get("count");
        @SuppressWarnings("unchecked") List<Map<String,Object>> value = (List<Map<String,Object>>) resp.get("value");
        if (value == null) {
            return success("Sin datos devueltos (posible endpoint no habilitado)");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Discusiones count=").append(count).append('\n');
        if (!value.isEmpty()) {
            int max = Math.min(3, value.size());
            for (int i=0;i<max;i++) {
                Map<String,Object> v = value.get(i);
                sb.append(i+1).append(") wi=").append(v.get("workItemId"))
                  .append(" rev=").append(v.get("revision"));
                Object discussionObj = v.get("discussion");
                if (discussionObj instanceof Map<?,?> d) {
                    Object comments = d.get("comments");
                    if (comments instanceof List<?> cl) sb.append(" comments=").append(cl.size());
                }
                sb.append('\n');
            }
            if (value.size()>max) sb.append("... ("+(value.size()-max)+" más)\n");
        }
        return success(sb.toString());
    }
}
