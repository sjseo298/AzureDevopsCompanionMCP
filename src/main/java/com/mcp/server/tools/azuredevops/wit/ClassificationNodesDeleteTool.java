package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitClassificationNodesDeleteHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_classification_nodes_delete
 * Elimina un nodo de clasificación (áreas/iteraciones) vía DELETE.
 */
@Component
public class ClassificationNodesDeleteTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_classification_nodes_delete";
    private static final String DESC = "Elimina un nodo de clasificación (areas/iterations) a nivel de proyecto.";
    private static final String API_VERSION_OVERRIDE = "7.2-preview"; // doc local

    private final WitClassificationNodesDeleteHelper helper;

    @Autowired
    public ClassificationNodesDeleteTool(AzureDevOpsClientService service) {
        super(service);
        this.helper = new WitClassificationNodesDeleteHelper(service);
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String, Object>) base.get("properties");
        Map<String,Object> groupProp = new LinkedHashMap<>();
        groupProp.put("type", "string");
        groupProp.put("description", "'areas' o 'iterations'");
        groupProp.put("enum", List.of("areas", "iterations"));
        props.put("group", groupProp);
        props.put("path", Map.of("type","string","description","Ruta del nodo (obligatoria)"));
        base.put("required", List.of("project","group","path"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        String project = getProject(arguments);
        String team = getTeam(arguments);
        String group = arguments.get("group") != null ? arguments.get("group").toString().trim().toLowerCase() : null;
        String path = arguments.get("path") != null ? arguments.get("path").toString().trim() : null;
        try {
            helper.validateDelete(project, group, path);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
        Map<String,Object> resp = helper.deleteNode(project, team, group, path, API_VERSION_OVERRIDE);
        String result = helper.formatDeleteResponse(resp);
        return success(result);
    }
}
