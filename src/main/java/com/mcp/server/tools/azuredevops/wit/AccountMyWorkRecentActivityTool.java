package com.mcp.server.tools.azuredevops.wit;


import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import com.mcp.server.services.helpers.WitAccountActivityHelper;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool: azuredevops_wit_get_account_my_work_recent_activity
 * Endpoint org-level: GET /_apis/wit/accountmyworkrecentactivity
 * No requiere project/team.
 */
@Component
public class AccountMyWorkRecentActivityTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_get_account_my_work_recent_activity";
    private static final String DESC = "Lista actividades recientes de work items del usuario autenticado (nivel organización).";

    private final WitAccountActivityHelper activityHelper;

    @Autowired
    public AccountMyWorkRecentActivityTool(AzureDevOpsClientService service, WitAccountActivityHelper activityHelper) {
        super(service);
        this.activityHelper = activityHelper;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        // No requiere 'project'
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("top", Map.of(
            "type", "integer",
            "description", "Cantidad máxima de actividades a mostrar (opcional, default 20)",
            "minimum", 1
        ));
        props.put("fromDate", Map.of(
            "type", "string",
            "description", "Filtrar actividades desde esta fecha (YYYY-MM-DD, opcional)",
            "pattern", "^\\d{4}-\\d{2}-\\d{2}$"
        ));
        return Map.of(
            "type", "object",
            "properties", props,
            "required", List.of()
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        activityHelper.validate();
        Integer top = arguments.get("top") instanceof Number ? ((Number) arguments.get("top")).intValue() : null;
        String fromDate = arguments.get("fromDate") instanceof String ? (String) arguments.get("fromDate") : null;
        Map<String,Object> resp = activityHelper.fetchRecentActivity();
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        String formatted = activityHelper.formatRecentActivity(resp, top, fromDate);
        if (formatted != null) return success(formatted);
        return Map.of("isError", false, "raw", resp);
    }
}
