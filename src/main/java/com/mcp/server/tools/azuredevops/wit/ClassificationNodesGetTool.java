package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitClassificationNodesGetHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_classification_nodes_get
 * Obtiene un nodo de clasificación (áreas o iteraciones) o el root si no se especifica path.
 */
@Component
public class ClassificationNodesGetTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_classification_nodes_get";
    private static final String DESC = "Obtiene nodos de clasificación (areas/iterations) a nivel de proyecto.";

    // Según doc local: usar 7.2-preview (sin .1) para evitar respuestas no-JSON
    private static final String API_VERSION_OVERRIDE = "7.2-preview";

    private final WitClassificationNodesGetHelper helper;

    @Autowired
    public ClassificationNodesGetTool(AzureDevOpsClientService service) {
        super(service);
        this.helper = new WitClassificationNodesGetHelper(service);
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>();
        base.putAll(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String, Object>) base.get("properties");
        Map<String,Object> groupProp = new LinkedHashMap<>();
        groupProp.put("type", "string");
        groupProp.put("description", "'areas' o 'iterations'");
        groupProp.put("enum", List.of("areas", "iterations"));
        props.put("group", groupProp);
        props.put("path", Map.of("type","string","description","Ruta del nodo (opcional)"));
        base.put("required", List.of("project","group"));
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
            helper.validate(project, group);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
        String endpoint = helper.buildEndpoint(group, path);
        Map<String,Object> resp = helper.fetchNode(project, team, endpoint, API_VERSION_OVERRIDE);
        return success(helper.formatNodeResponse(resp));
    }
}
