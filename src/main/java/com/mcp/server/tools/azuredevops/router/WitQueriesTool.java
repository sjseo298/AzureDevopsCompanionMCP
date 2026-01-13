package com.mcp.server.tools.azuredevops.router;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import com.mcp.server.tools.azuredevops.wit.ListQueriesRootFoldersTool;
import com.mcp.server.tools.azuredevops.wit.SearchQueriesTool;
import com.mcp.server.tools.azuredevops.wit.WiqlByIdTool;
import com.mcp.server.tools.azuredevops.wit.WiqlByQueryTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class WitQueriesTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_queries";
    private static final String DESC = "Operaciones WIT Queries. operation: wiql_query|wiql_by_id|search_queries|list_root_folders.";

    private final WiqlByQueryTool wiqlByQueryTool;
    private final WiqlByIdTool wiqlByIdTool;
    private final SearchQueriesTool searchQueriesTool;
    private final ListQueriesRootFoldersTool listRootFoldersTool;

    @Autowired
    public WitQueriesTool(
            AzureDevOpsClientService svc,
            WiqlByQueryTool wiqlByQueryTool,
            WiqlByIdTool wiqlByIdTool,
            SearchQueriesTool searchQueriesTool,
            ListQueriesRootFoldersTool listRootFoldersTool
    ) {
        super(svc);
        this.wiqlByQueryTool = wiqlByQueryTool;
        this.wiqlByIdTool = wiqlByIdTool;
        this.searchQueriesTool = searchQueriesTool;
        this.listRootFoldersTool = listRootFoldersTool;
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
                "enum", List.of("wiql_query", "wiql_by_id", "search_queries", "list_root_folders"),
                "description", "Operación a ejecutar"
        ));
        props.put("query", Map.of("type", "string", "description", "WIQL o término de búsqueda según operación"));
        props.put("id", Map.of("type", "string", "description", "ID de query (wiql_by_id)"));
        props.put("top", Map.of("type", "integer", "description", "Límite"));
        props.put("folderPath", Map.of("type", "string", "description", "Folder path (list_root_folders)"));
        base.put("required", List.of("project", "operation"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        String op = arguments.get("operation") == null ? "" : arguments.get("operation").toString().trim();
        if (op.isEmpty()) return error("'operation' es requerido");
        
        // Mapeo de parámetro 'query' genérico al específico de cada herramienta
        Object queryVal = arguments.get("query");
        if (queryVal != null) {
            if ("wiql_query".equals(op)) {
                arguments.put("wiql", queryVal);
            } else if ("search_queries".equals(op)) {
                arguments.put("searchText", queryVal);
            }
        }

        return switch (op) {
            case "wiql_query" -> delegate(wiqlByQueryTool, arguments);
            case "wiql_by_id" -> delegate(wiqlByIdTool, arguments);
            case "search_queries" -> delegate(searchQueriesTool, arguments);
            case "list_root_folders" -> delegate(listRootFoldersTool, arguments);
            default -> error("Operación no soportada: " + op);
        };
    }

    private Map<String, Object> delegate(AbstractAzureDevOpsTool tool, Map<String, Object> arguments) {
        Map<String, Object> copy = new LinkedHashMap<>(arguments);
        copy.remove("operation");
        return tool.execute(copy);
    }
}
