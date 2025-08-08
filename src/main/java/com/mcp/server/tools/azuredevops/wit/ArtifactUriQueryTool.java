package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool: azuredevops_wit_artifact_uri_query
 * Endpoint org-level: POST /_apis/wit/artifacturiquery
 */
@Component
public class ArtifactUriQueryTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_artifact_uri_query";
    private static final String DESC = "Consulta work items vinculados a URIs de artefactos (nivel organización).";

    @Autowired
    public ArtifactUriQueryTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        // Org-level, no requiere project
        Object uris = args.get("uris");
        if (!(uris instanceof List) || ((List<?>) uris).isEmpty()) {
            throw new IllegalArgumentException("El parámetro 'uris' (array) es requerido y no puede ser vacío");
        }
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("uris", Map.of(
            "type", "array",
            "description", "Lista de URIs de artefactos",
            "items", Map.of("type", "string")
        ));
        return Map.of(
            "type", "object",
            "properties", props,
            "required", List.of("uris")
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado en este entorno");
        @SuppressWarnings("unchecked")
        List<String> uris = (List<String>) arguments.get("uris");
        Map<String,Object> body = Map.of("uris", uris);
        Map<String,Object> resp = azureService.postCoreApi("wit/artifacturiquery", null, body, null);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return success(format(resp));
    }

    private String format(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Respuesta vacía)";
        if (data.containsKey("error")) return "Error remoto: " + data.get("error");
        Object val = data.get("value");
        if (val instanceof List) {
            StringBuilder sb = new StringBuilder("=== Artifact URI Query ===\n\n");
            int i = 1;
            for (Object o : (List<?>) val) {
                if (o instanceof Map) {
                    Map<?,?> m = (Map<?,?>) o;
                    Object uri = m.get("artifactUri");
                    Object ids = m.get("workItemIds");
                    sb.append(i++).append(". ").append(uri != null ? uri : "(sin uri)");
                    if (ids instanceof List) sb.append(" -> ").append(((List<?>) ids).size()).append(" item(s)");
                    sb.append("\n");
                }
            }
            return sb.toString();
        }
        return data.toString();
    }
}
