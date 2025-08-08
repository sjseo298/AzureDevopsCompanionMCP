package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool: azuredevops_wit_get_artifact_link_types
 * Endpoint org-level: GET /_apis/wit/artifactlinktypes
 */
@Component
public class ArtifactLinkTypesTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_get_artifact_link_types";
    private static final String DESC = "Lista tipos de enlaces de artefactos (nivel organización).";

    @Autowired
    public ArtifactLinkTypesTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        // Org-level, no requiere project
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", new LinkedHashMap<>(),
            "required", List.of()
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        Map<String,Object> resp = azureService.getCoreApi("wit/artifactlinktypes", null);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return success(format(resp));
    }

    private String format(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Respuesta vacía)";
        if (data.containsKey("error")) return "Error remoto: " + data.get("error");
        Object val = data.get("value");
        if (val instanceof List) {
            StringBuilder sb = new StringBuilder("=== Artifact Link Types ===\n\n");
            int i = 1;
            for (Object o : (List<?>) val) {
                if (o instanceof Map) {
                    Map<?,?> m = (Map<?,?>) o;
                    Object name = m.get("name");
                    Object type = m.get("artifactType");
                    Object linkType = m.get("linkType");
                    sb.append(i++).append(". ").append(name != null ? name : "(sin nombre)");
                    if (type != null) sb.append(" [").append(type).append("]");
                    if (linkType != null) sb.append(" (linkType: ").append(linkType).append(")");
                    sb.append("\n");
                }
            }
            return sb.toString();
        }
        return data.toString();
    }
}
