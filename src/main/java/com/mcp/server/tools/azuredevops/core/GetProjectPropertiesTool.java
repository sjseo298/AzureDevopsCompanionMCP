package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.ProjectsHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetProjectPropertiesTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_core_get_project_properties";
    private static final String DESC = "Obtiene propiedades de un proyecto (opcionalmente filtradas por 'keys')";

    private final ProjectsHelper projectsHelper;

    @Autowired
    public GetProjectPropertiesTool(AzureDevOpsClientService service, ProjectsHelper projectsHelper) {
        super(service);
        this.projectsHelper = projectsHelper;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type","object",
            "properties", Map.of(
                "projectId", Map.of("type","string","description","GUID del proyecto"),
                "keys", Map.of(
                    "oneOf", List.of(
                        Map.of("type","string","description","Lista separada por comas de claves a recuperar"),
                        Map.of("type","array","items", Map.of("type","string"), "description","Arreglo de claves a recuperar")
                    ),
                    "description","Claves de propiedades a recuperar (si se omite, puede responder 'The request is invalid' en algunas organizaciones)"
                )
            ),
            "required", List.of("projectId")
        );
    }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        projectsHelper.validateProjectProperties(
            Optional.ofNullable(args.get("projectId")).map(Object::toString).orElse(null),
            args.get("keys")
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        String pid = arguments.get("projectId").toString();
        Map<String,String> q = projectsHelper.buildProjectPropertiesQuery(arguments.get("keys"));
        Map<String,Object> resp = projectsHelper.fetchProjectProperties(pid, q);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return Map.of("isError", false, "raw", resp);
    }
}
