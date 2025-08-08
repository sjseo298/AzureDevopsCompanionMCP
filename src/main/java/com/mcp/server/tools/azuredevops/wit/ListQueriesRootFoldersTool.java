package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_list_queries_root_folders
 * Lista carpetas raíz y subcarpetas de queries en un proyecto.
 */
@Component
public class ListQueriesRootFoldersTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_list_queries_root_folders";
    private static final String DESC = "Lista carpetas raíz y subcarpetas de queries en un proyecto.";
    private static final String API_VERSION_OVERRIDE = "7.2-preview";

    @Autowired
    public ListQueriesRootFoldersTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String, Object>) base.get("properties");
        props.put("expand", Map.of("type","string","enum", List.of("none","clauses","all","wiql"), "description","Nivel de expansión"));
        props.put("depth", Map.of("type","integer","description","Profundidad de carpetas"));
        props.put("includeDeleted", Map.of("type","boolean","description","Incluir eliminados"));
        props.put("queryType", Map.of("type","string","enum", List.of("flat","tree","oneHop"), "description","Tipo de query"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        String project = getProject(arguments);
        String team = getTeam(arguments);
        String endpoint = "queries";
        Map<String,String> query = new LinkedHashMap<>();
        Object expand = arguments.get("expand");
        if (expand != null) query.put("$expand", expand.toString());
        Object depth = arguments.get("depth");
        if (depth != null) query.put("depth", depth.toString());
        Object includeDeleted = arguments.get("includeDeleted");
        if (includeDeleted != null) query.put("includeDeleted", includeDeleted.toString());
        Object queryType = arguments.get("queryType");
        if (queryType != null) query.put("queryType", queryType.toString());

        Map<String,Object> resp = azureService.getWitApiWithQuery(project, team, endpoint, query.isEmpty()?null:query, API_VERSION_OVERRIDE);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return success(format(resp));
    }

    private String format(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Sin resultados)";
        Object val = data.get("value");
        if (val instanceof List) {
            List<?> list = (List<?>) val;
            if (list.isEmpty()) return "(Sin resultados)";
            StringBuilder sb = new StringBuilder("=== Queries Root Folders ===\n\n");
            int i=1;
            for (Object o : list) {
                if (o instanceof Map) {
                    Map<?,?> m = (Map<?,?>) o;
                    Object id = m.get("id");
                    Object name = m.get("name");
                    sb.append(i++).append(") ").append(name != null ? name : "(sin nombre)")
                      .append(" [").append(id != null ? id : "?").append("]\n");
                }
            }
            return sb.toString();
        }
        return data.toString();
    }
}
