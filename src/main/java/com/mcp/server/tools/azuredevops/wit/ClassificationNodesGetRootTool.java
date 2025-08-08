package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
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

    @Autowired
    public ClassificationNodesGetRootTool(AzureDevOpsClientService service) { super(service); }

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
        Map<String,Object> resp = azureService.getWitApiWithQuery(project, team, "classificationnodes", null, API_VERSION_OVERRIDE);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return success(format(resp));
    }

    private String format(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Respuesta vacía)";
        if (data.containsKey("value") && data.get("value") instanceof List) {
            List<?> list = (List<?>) data.get("value");
            StringBuilder sb = new StringBuilder("=== Root Classification Nodes ===\n\n");
            int i = 1;
            for (Object o : list) {
                if (o instanceof Map) {
                    Map<?,?> m = (Map<?,?>) o;
                    sb.append(i++).append(". ");
                    Object name = m.get("name");
                    Object type = m.get("structureType");
                    sb.append(name != null ? name : "(sin nombre)");
                    if (type != null) sb.append(" [").append(type).append("]");
                    sb.append('\n');
                }
            }
            return sb.toString();
        }
        // Algunas respuestas pueden no estar envueltas en 'value'
        return data.toString();
    }
}
