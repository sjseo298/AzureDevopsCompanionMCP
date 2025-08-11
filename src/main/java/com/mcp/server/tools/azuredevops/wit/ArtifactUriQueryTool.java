package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import com.mcp.server.services.helpers.WitArtifactUriQueryHelper;
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

    private final WitArtifactUriQueryHelper artifactUriQueryHelper;

    @Autowired
    public ArtifactUriQueryTool(AzureDevOpsClientService service, WitArtifactUriQueryHelper artifactUriQueryHelper) {
        super(service);
        this.artifactUriQueryHelper = artifactUriQueryHelper;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    protected void validateCommon(Map<String, Object> args) { artifactUriQueryHelper.validateUris(args.get("uris")); }

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
        Map<String,Object> body = artifactUriQueryHelper.buildBody(arguments.get("uris"));
        Map<String,Object> resp = artifactUriQueryHelper.executeQuery(body);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        String formatted = artifactUriQueryHelper.formatResponse(resp);
        if (formatted != null) return success(formatted);
        return Map.of("isError", false, "raw", resp);
    }
}
