package com.mcp.server.tools.azuredevops.router;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import com.mcp.server.tools.azuredevops.wit.ReportingWorkItemLinksGetTool;
import com.mcp.server.tools.azuredevops.wit.ReportingWorkItemRevisionsGetTool;
import com.mcp.server.tools.azuredevops.wit.ReportingWorkItemRevisionsPostTool;
import com.mcp.server.tools.azuredevops.wit.RevisionsGetTool;
import com.mcp.server.tools.azuredevops.wit.RevisionsListTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class WitReportingTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_reporting";
    private static final String DESC = "Operaciones WIT Reporting/Revisions. operation: revisions_list|revisions_get|reporting_links_get|reporting_revisions_get|reporting_revisions_post.";

    private final RevisionsListTool revisionsListTool;
    private final RevisionsGetTool revisionsGetTool;
    private final ReportingWorkItemLinksGetTool reportingLinksGetTool;
    private final ReportingWorkItemRevisionsGetTool reportingRevisionsGetTool;
    private final ReportingWorkItemRevisionsPostTool reportingRevisionsPostTool;

    @Autowired
    public WitReportingTool(
            AzureDevOpsClientService svc,
            RevisionsListTool revisionsListTool,
            RevisionsGetTool revisionsGetTool,
            ReportingWorkItemLinksGetTool reportingLinksGetTool,
            ReportingWorkItemRevisionsGetTool reportingRevisionsGetTool,
            ReportingWorkItemRevisionsPostTool reportingRevisionsPostTool
    ) {
        super(svc);
        this.revisionsListTool = revisionsListTool;
        this.revisionsGetTool = revisionsGetTool;
        this.reportingLinksGetTool = reportingLinksGetTool;
        this.reportingRevisionsGetTool = reportingRevisionsGetTool;
        this.reportingRevisionsPostTool = reportingRevisionsPostTool;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) base.get("properties");
        props.put("operation", Map.of(
                "type", "string",
                "enum", List.of("revisions_list", "revisions_get", "reporting_links_get", "reporting_revisions_get", "reporting_revisions_post"),
                "description", "Operación a ejecutar"
        ));

        // Unión de parámetros (dejamos que el tool delegado valide)
        props.put("id", Map.of("type", "integer", "description", "ID work item (revisions_get)"));
        props.put("fields", Map.of("type", "string", "description", "Campos"));
        props.put("types", Map.of("type", "string", "description", "Tipos"));
        props.put("includeDeleted", Map.of("type", "boolean"));
        props.put("includeLatestOnly", Map.of("type", "boolean"));
        props.put("includeIdentityRef", Map.of("type", "boolean"));
        props.put("includeTagRef", Map.of("type", "boolean"));
        props.put("continuationToken", Map.of("type", "string", "description", "Paginación"));
        props.put("top", Map.of("type", "integer", "description", "Límite"));
        props.put("raw", Map.of("type", "boolean", "description", "Devuelve JSON crudo"));

        base.put("required", List.of("project", "operation"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        String op = arguments.get("operation") == null ? "" : arguments.get("operation").toString().trim();
        if (op.isEmpty()) return error("'operation' es requerido");
        return switch (op) {
            case "revisions_list" -> delegate(revisionsListTool, arguments);
            case "revisions_get" -> delegate(revisionsGetTool, arguments);
            case "reporting_links_get" -> delegate(reportingLinksGetTool, arguments);
            case "reporting_revisions_get" -> delegate(reportingRevisionsGetTool, arguments);
            case "reporting_revisions_post" -> delegate(reportingRevisionsPostTool, arguments);
            default -> error("Operación no soportada: " + op);
        };
    }

    private Map<String, Object> delegate(AbstractAzureDevOpsTool tool, Map<String, Object> arguments) {
        Map<String, Object> copy = new LinkedHashMap<>(arguments);
        copy.remove("operation");
        return tool.execute(copy);
    }
}
