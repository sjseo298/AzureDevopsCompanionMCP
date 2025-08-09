package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_search_queries
 * Busca queries por texto en un proyecto (POST queries/$search con fallback GET ?searchText).
 */
@Component
public class SearchQueriesTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_search_queries";
    private static final String DESC = "Busca queries por texto en un proyecto (POST queries/$search con fallback GET ?searchText).";
    private static final String API_VERSION_OVERRIDE = "7.2-preview";

    @Autowired
    public SearchQueriesTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String, Object>) base.get("properties");
        props.put("searchText", Map.of("type","string","description","Texto a buscar"));
        props.put("expand", Map.of("type","string","enum", List.of("none","clauses","all","wiql"), "description","Nivel de expansión"));
        props.put("top", Map.of("type","integer","description","Límite de resultados"));
        base.put("required", List.of("project","searchText"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        String project = getProject(arguments);
        String team = getTeam(arguments);
        Object searchText = arguments.get("searchText");
        if (searchText == null || searchText.toString().trim().isEmpty()) return error("'searchText' es requerido");
        String sText = searchText.toString();
        String endpoint = "queries/$search";
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("searchText", sText);
        Object expand = arguments.get("expand");
        if (expand != null) body.put("$expand", expand.toString());
        Object top = arguments.get("top");
        if (top != null) body.put("top", Integer.valueOf(top.toString()));

        // 1) Intento POST $search (preview)
        Map<String,Object> resp = azureService.postWitApi(project, team, endpoint, body, API_VERSION_OVERRIDE);
        if (looksLikeUnsupportedSearchEndpoint(resp)) {
            // 2) Fallback GET /queries?searchText=...&top=...&$expand=...
            Map<String,String> query = new LinkedHashMap<>();
            query.put("searchText", sText);
            if (expand != null) query.put("$expand", expand.toString());
            if (top != null) query.put("top", top.toString());
            Map<String,Object> getResp = azureService.getWitApiWithQuery(project, team, "queries", query, API_VERSION_OVERRIDE);
            String formattedErrGet = tryFormatRemoteError(getResp);
            if (formattedErrGet != null) return success(formattedErrGet);
            return success(format(getResp));
        }

        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return success(format(resp));
    }

    @SuppressWarnings("unchecked")
    private boolean looksLikeUnsupportedSearchEndpoint(Map<String,Object> resp) {
        if (resp == null || resp.isEmpty()) return false;
        Object typeKey = resp.get("typeKey");
        Object message = resp.get("message");
        if (typeKey != null && "VssPropertyValidationException".equals(typeKey.toString()) && message != null) {
            String msg = message.toString();
            return msg.contains("Parameter name: Name") || msg.matches("(?s).*Parameter name: (Wiql|isFolder).*");
        }
        // También puede venir envuelto en isHttpError con campos en el cuerpo
        Object isHttpErr = resp.get("isHttpError");
        if (Boolean.TRUE.equals(isHttpErr)) {
            Object innerType = resp.get("typeKey");
            Object innerMsg = resp.get("message");
            if (innerType != null && "VssPropertyValidationException".equals(innerType.toString()) && innerMsg != null) {
                String m = innerMsg.toString();
                return m.contains("Parameter name: Name") || m.matches("(?s).*Parameter name: (Wiql|isFolder).*");
            }
        }
        return false;
    }

    private String format(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Sin resultados)";
        Object val = data.get("value");
        if (val instanceof List) {
            List<?> list = (List<?>) val;
            if (list.isEmpty()) return "(Sin resultados)";
            StringBuilder sb = new StringBuilder("=== Queries encontradas ===\n\n");
            int i=1;
            for (Object o : list) {
                if (o instanceof Map) {
                    Map<?,?> m = (Map<?,?>) o;
                    Object id = m.get("id");
                    Object name = m.get("name");
                    sb.append(i++).append(") ")
                      .append(name != null ? name : "(sin nombre)")
                      .append(" [").append(id != null ? id : "?").append("]\n");
                }
            }
            return sb.toString();
        }
        return data.toString();
    }
}
