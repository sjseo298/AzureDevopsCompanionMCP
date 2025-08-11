package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitClassificationNodesGetRootHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_classification_nodes_get_root
 * Obtiene los nodos raíz de clasificación del proyecto (areas e iterations).
 */
@Component
public class ClassificationNodesGetRootTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_classification_nodes_get_root";
    private static final String DESC = "Obtiene los nodos raíz de clasificación (areas e iterations) a nivel de proyecto.";
    private static final String API_VERSION_OVERRIDE = "7.2-preview";

    private final WitClassificationNodesGetRootHelper helper;

    @Autowired
    public ClassificationNodesGetRootTool(AzureDevOpsClientService service) {
        super(service);
        this.helper = new WitClassificationNodesGetRootHelper(service);
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        // Solo requiere project, team opcional
        return createBaseSchema();
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        String project = getProject(arguments);
        String team = getTeam(arguments);
        try {
            helper.validate(project);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
        Map<String,Object> resp = helper.fetchRootNodes(project, team, API_VERSION_OVERRIDE);
        return success(helper.formatRootNodesResponse(resp));
    }
}
