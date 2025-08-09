package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_wiql_by_id
 * Ejecuta una query guardada por ID (GET _apis/wit/wiql/{id}).
 */
@Component
public class WiqlByIdTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_wiql_by_id";
    private static final String DESC = "Ejecuta una query guardada por ID (_apis/wit/wiql/{id}).";
    private static final String DEFAULT_API_VERSION = "7.2-preview";

    @Autowired
    public WiqlByIdTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String, Object>) base.get("properties");
        props.put("id", Map.of("type","string","description","ID (GUID) de la query guardada"));
        props.put("apiVersion", Map.of("type","string","description","Override api-version (opcional, default "+DEFAULT_API_VERSION+")"));
        base.put("required", List.of("project","id"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        String project = getProject(arguments);
        String team = getTeam(arguments); // no se usa en wiql by id, pero se permite para consistencia
        Object id = arguments.get("id");
        if (id == null || id.toString().trim().isEmpty()) return error("'id' es requerido");
        String apiVersion = Optional.ofNullable(arguments.get("apiVersion")).map(Object::toString).filter(s->!s.isBlank()).orElse(DEFAULT_API_VERSION);
        String endpoint = "wiql/" + id;
        Map<String,Object> resp = azureService.getWitApiWithQuery(project, team, endpoint, null, apiVersion);

        // Mejorar detección de error típico cuando se pasa un folderId
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) {
            if (isFolderQueryError(resp)) {
                formattedErr += "\nSugerencia: El ID corresponde a una carpeta de queries (isFolder=true). Use 'search_queries' o 'list_queries_root_folders' para localizar una query (isFolder=false) y use ese ID.";
            }
            return success(formattedErr);
        }
        return success(format(resp));
    }

    private boolean isFolderQueryError(Map<String,Object> resp) {
        if (resp == null) return false;
        Object typeKey = resp.get("typeKey");
        Object message = resp.get("message");
        return typeKey != null && "QueryException".equals(typeKey.toString()) && message != null && message.toString().contains("Querying folders is not supported");
    }

    private String format(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Sin datos)";
        StringBuilder sb = new StringBuilder();
        Object queryType = data.get("queryType");
        if (queryType != null) sb.append("Tipo: ").append(queryType).append('\n');
        @SuppressWarnings("unchecked") List<Map<String,Object>> cols = (List<Map<String,Object>>) data.get("columns");
        if (cols != null && !cols.isEmpty()) {
            sb.append("Columnas ("+cols.size()+"): ");
            cols.stream().limit(8).forEach(c -> sb.append(c.getOrDefault("referenceName","?"))); 
            sb.append('\n');
        }
        Object workItems = data.get("workItems");
        if (workItems instanceof List) {
            List<?> list = (List<?>) workItems;
            sb.append("WorkItems: ").append(list.size()).append('\n');
            int i=1;
            for (Object o : list) {
                if (i>25) { sb.append("... ("+(list.size()-25)+" más)\n"); break; }
                if (o instanceof Map) {
                    Object wid = ((Map<?,?>)o).get("id");
                    sb.append(i++).append(") ID=").append(wid).append('\n');
                }
            }
        }
        return sb.toString().isBlank() ? data.toString() : sb.toString();
    }
}
