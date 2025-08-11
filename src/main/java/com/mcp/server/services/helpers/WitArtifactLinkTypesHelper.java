package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Helper para listado de tipos de enlaces de artefactos (Artifact Link Types) a nivel organización.
 */
@Service
public class WitArtifactLinkTypesHelper {

    private final AzureDevOpsClientService azureService;

    public WitArtifactLinkTypesHelper(AzureDevOpsClientService azureService) {
        this.azureService = azureService;
    }

    public void validate() { /* org-level sin parámetros */ }

    public Map<String,Object> fetchArtifactLinkTypes() {
        return azureService.getCoreApi("wit/artifactlinktypes", null);
    }

    @SuppressWarnings("unchecked")
    public String formatArtifactLinkTypes(Map<String,Object> resp) {
        if (resp == null || resp.isEmpty()) return "(Respuesta vacía)";
        Object val = resp.get("value");
        if (!(val instanceof List)) return null; // dejar raw
        List<?> list = (List<?>) val;
        if (list.isEmpty()) return "Sin resultados";
        StringBuilder sb = new StringBuilder("=== Artifact Link Types ===\n\n");
        int i = 1;
        for (Object o : list) {
            if (o instanceof Map) {
                Map<?,?> m = (Map<?,?>) o;
                Object name = m.get("name");
                Object type = m.get("artifactType");
                Object linkType = m.get("linkType");
                sb.append(i++).append('.').append(' ')
                  .append(name != null ? name : "(sin nombre)");
                if (type != null) sb.append(" [").append(type).append(']');
                if (linkType != null) sb.append(" (linkType: ").append(linkType).append(')');
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
