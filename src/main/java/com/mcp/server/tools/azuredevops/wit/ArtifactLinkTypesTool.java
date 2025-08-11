package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import com.mcp.server.services.helpers.WitArtifactLinkTypesHelper;
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

    private final WitArtifactLinkTypesHelper artifactLinkTypesHelper;

    @Autowired
    public ArtifactLinkTypesTool(AzureDevOpsClientService service, WitArtifactLinkTypesHelper artifactLinkTypesHelper) {
        super(service);
        this.artifactLinkTypesHelper = artifactLinkTypesHelper;
    }

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
        artifactLinkTypesHelper.validate();
        Map<String,Object> resp = artifactLinkTypesHelper.fetchArtifactLinkTypes();
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        String formatted = artifactLinkTypesHelper.formatArtifactLinkTypes(resp);
        if (formatted != null) return success(formatted);
        return Map.of("isError", false, "raw", resp);
    }
}
