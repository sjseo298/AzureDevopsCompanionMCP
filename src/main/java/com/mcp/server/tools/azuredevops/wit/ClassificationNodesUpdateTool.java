package com.mcp.server.tools.azuredevops.wit;


import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitClassificationNodesUpdateHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_classification_nodes_update
 * Actualiza un nodo de clasificación (áreas/iteraciones) vía PATCH.
 */
@Component
public class ClassificationNodesUpdateTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_classification_nodes_update";
    private static final String DESC = "Actualiza un nodo de clasificación (areas/iterations) a nivel de proyecto.";
    private static final String API_VERSION_OVERRIDE = "7.2-preview"; // doc local

    private final WitClassificationNodesUpdateHelper helper;

    @Autowired
    public ClassificationNodesUpdateTool(AzureDevOpsClientService service) {
        super(service);
        this.helper = new WitClassificationNodesUpdateHelper(service);
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
        props.put("name", Map.of("type","string","description","Nuevo nombre del nodo (opcional)"));
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
        Object nameArg = arguments.get("name");
        try {
            helper.validate(project, group, path);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
        String endpoint = helper.buildEndpoint(group, path);
        Map<String,Object> body = helper.buildBody(nameArg);
        Map<String,Object> resp = helper.patchNode(project, team, endpoint, body, API_VERSION_OVERRIDE);
        return success(helper.formatNodeResponse(resp));
    }
}
