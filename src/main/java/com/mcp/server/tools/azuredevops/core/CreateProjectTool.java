package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.ProjectsHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CreateProjectTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_core_create_project";
    private static final String DESC = "Crea un proyecto (nombre requerido)";

    private final ProjectsHelper projectsHelper;

    @Autowired
    public CreateProjectTool(AzureDevOpsClientService service, ProjectsHelper projectsHelper) {
        super(service);
        this.projectsHelper = projectsHelper;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        projectsHelper.validateCreateProject(
            Optional.ofNullable(args.get("name")).map(Object::toString).orElse(null),
            Optional.ofNullable(args.get("visibility")).map(Object::toString).orElse(null),
            Optional.ofNullable(args.get("sourceControlType")).map(Object::toString).orElse(null),
            Optional.ofNullable(args.get("processTypeId")).map(Object::toString).orElse(null)
        );
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> s = new HashMap<>();
        s.put("type","object");
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("name", Map.of("type","string","description","Nombre del proyecto"));
        props.put("description", Map.of("type","string","description","Descripción (opcional)"));
        props.put("visibility", Map.of("type","string","enum", List.of("private","public"), "description","Visibilidad (private|public)"));
        props.put("sourceControlType", Map.of("type","string","enum", List.of("Git","TFVC"), "description","Tipo de control de versiones (Git|TFVC)"));
        props.put("processTypeId", Map.of("type","string","description","Process template typeId (GUID)"));
        s.put("properties", props);
        s.put("required", List.of("name","visibility","sourceControlType","processTypeId"));
        return s;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        Map<String,Object> body = projectsHelper.buildCreateProjectBody(
            arguments.get("name").toString(),
            Optional.ofNullable(arguments.get("description")).map(Object::toString).orElse(null),
            arguments.get("visibility").toString(),
            arguments.get("sourceControlType").toString(),
            arguments.get("processTypeId").toString()
        );
        Map<String,Object> resp = projectsHelper.createProject(body);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        String formatted = projectsHelper.formatCreateProjectResponse(resp);
        if (formatted != null) return success(formatted);
        return Map.of("isError", false, "raw", resp);
    }
}
