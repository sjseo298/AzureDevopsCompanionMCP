package com.mcp.server.tools.azuredevops.wit;


import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Component;
import com.mcp.server.services.helpers.WitWiqlHelper;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_wiql_by_query
 * Ejecuta una consulta WIQL ad-hoc (POST _apis/wit/wiql).
 */
@Component
public class WiqlByQueryTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_wiql_by_query";
    private static final String DESC = "Ejecuta una consulta WIQL ad-hoc en el proyecto y obtiene los datos completos de los work items (POST _apis/wit/wiql + GET _apis/wit/workitems).";
    private static final String DEFAULT_API_VERSION = "7.2-preview";

    private final WitWiqlHelper helper;

    @Autowired
    public WiqlByQueryTool(AzureDevOpsClientService service, WitWiqlHelper helper) {
        super(service);
        this.helper = helper;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String, Object>) base.get("properties");
        props.put("wiql", Map.of("type","string","description","Consulta WIQL a ejecutar"));
        props.put("top", Map.of("type","integer","description","Máximo de work items a devolver. Se envía como $top a WIQL y se aplica también como fallback local."));
        props.put("fields", Map.of("type","string","description","Campos referenceName adicionales para traer datos completos. Si no se indica, usa columnas WIQL; si solo hay System.Id, usa campos útiles por defecto."));
        props.put("raw", Map.of("type","boolean","description","Si true devuelve respuesta estructurada completa con items, count, topApplied, fieldsUsed y metadata de orden."));
        props.put("apiVersion", Map.of("type","string","description","Override api-version (opcional, default "+DEFAULT_API_VERSION+")"));
        base.put("required", List.of("project","wiql"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        String project = getProject(arguments);
        String team = getTeam(arguments);
        Object wiql = arguments.get("wiql");
        Object top = arguments.get("top");
        Object fields = arguments.get("fields");
        Object apiVersion = arguments.get("apiVersion");
        boolean raw = Boolean.TRUE.equals(arguments.get("raw"));
        try {
            helper.validateWiql(wiql);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
        
        // Usar el nuevo método que obtiene datos completos de work items
        Map<String,Object> resp = helper.fetchByQueryWithData(project, team, wiql, top, fields, apiVersion);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        if (raw) return rawSuccess(resp);
        return success(helper.formatResponse(resp));
    }
}
