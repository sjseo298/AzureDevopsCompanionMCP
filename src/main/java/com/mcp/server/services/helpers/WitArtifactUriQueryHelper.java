package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Helper para consultar work items vinculados a URIs de artefactos (artifacturiquery) a nivel organización.
 */
@Service
public class WitArtifactUriQueryHelper {

    private final AzureDevOpsClientService azureService;

    public WitArtifactUriQueryHelper(AzureDevOpsClientService azureService) {
        this.azureService = azureService;
    }

    public void validateUris(Object urisArg) {
        if (!(urisArg instanceof List) || ((List<?>) urisArg).isEmpty()) {
            throw new IllegalArgumentException("El parámetro 'uris' (array) es requerido y no puede ser vacío");
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String,Object> buildBody(Object urisArg) {
        return Map.of("uris", (List<String>) urisArg);
    }

    public Map<String,Object> executeQuery(Map<String,Object> body) {
        return azureService.postCoreApi("wit/artifacturiquery", null, body, null);
    }

    @SuppressWarnings("unchecked")
    public String formatResponse(Map<String,Object> resp) {
        if (resp == null || resp.isEmpty()) return "(Respuesta vacía)";
        Object val = resp.get("value");
        if (!(val instanceof List)) return null; // dejar raw
        List<?> list = (List<?>) val;
        if (list.isEmpty()) return "Sin resultados";
        StringBuilder sb = new StringBuilder("=== Artifact URI Query ===\n\n");
        int i=1;
        for (Object o : list) {
            if (o instanceof Map) {
                Map<?,?> m = (Map<?,?>) o;
                Object uri = m.get("artifactUri");
                Object ids = m.get("workItemIds");
                sb.append(i++).append(". ").append(uri != null ? uri : "(sin uri)");
                if (ids instanceof List) sb.append(" -> ").append(((List<?>) ids).size()).append(" item(s)");
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
