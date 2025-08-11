package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import com.mcp.server.services.helpers.WitClassificationNodesHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_classification_nodes_create_or_update
 * Crea o actualiza un nodo de clasificación (áreas/iteraciones) vía PUT.
 */
@Component
public class ClassificationNodesCreateOrUpdateTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_classification_nodes_create_or_update";
    private static final String DESC = "Crea o actualiza un nodo de clasificación (areas/iterations) a nivel de proyecto.";
    private static final String API_VERSION_OVERRIDE = "7.2-preview"; // doc local

    private final WitClassificationNodesHelper nodesHelper;

    @Autowired
    public ClassificationNodesCreateOrUpdateTool(AzureDevOpsClientService service, WitClassificationNodesHelper nodesHelper) {
        super(service);
        this.nodesHelper = nodesHelper;
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
        props.put("name", Map.of("type","string","description","Nombre del nodo (opcional, por defecto último segmento del path)"));
        base.put("required", List.of("project","group","path"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        String project = getProject(arguments);
        String team = getTeam(arguments);
        String group = Objects.toString(arguments.get("group"), null);
        String path = Objects.toString(arguments.get("path"), null);
        nodesHelper.validateCreateOrUpdate(project, group, path);
        String name = nodesHelper.resolveName(path, arguments.get("name"));
        Map<String,Object> body = nodesHelper.buildBody(name);
        Map<String,Object> resp = nodesHelper.putNode(project, team, group, path, body, API_VERSION_OVERRIDE);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        String formatted = nodesHelper.formatNodeResponse(resp);
        if (formatted != null) return success(formatted);
        return Map.of("isError", false, "raw", resp);
    }
}
