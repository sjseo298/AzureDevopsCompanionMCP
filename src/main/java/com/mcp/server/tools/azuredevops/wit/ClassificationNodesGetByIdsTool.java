package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitClassificationNodesGetByIdsHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_classification_nodes_get_by_ids
 * Obtiene nodos de clasificación por IDs (query param ids=...).
 */
@Component
public class ClassificationNodesGetByIdsTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_classification_nodes_get_by_ids";
    private static final String DESC = "Obtiene nodos de clasificación por IDs a nivel de proyecto.";
    private static final String API_VERSION_OVERRIDE = "7.2-preview";

    private final WitClassificationNodesGetByIdsHelper helper;

    @Autowired
    public ClassificationNodesGetByIdsTool(AzureDevOpsClientService service) {
        super(service);
        this.helper = new WitClassificationNodesGetByIdsHelper(service);
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String, Object>) base.get("properties");
        props.put("ids", Map.of("type","string","description","Lista de IDs separados por coma"));
        base.put("required", List.of("project","ids"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        String project = getProject(arguments);
        String team = getTeam(arguments);
        String ids = arguments.get("ids") != null ? arguments.get("ids").toString().trim() : "";
        try {
            helper.validate(project, ids);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
        Map<String,Object> resp = helper.fetchNodes(project, team, ids, API_VERSION_OVERRIDE);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return success(helper.formatNodesResponse(resp));
    }
}
