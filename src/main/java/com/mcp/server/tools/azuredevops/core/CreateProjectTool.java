package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CreateProjectTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_core_create_project";
    private static final String DESC = "Crea un proyecto (nombre requerido)";

    @Autowired
    public CreateProjectTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        String name = Optional.ofNullable(args.get("name")).map(Object::toString).map(String::trim).orElse("");
        String visibility = Optional.ofNullable(args.get("visibility")).map(Object::toString).map(String::trim).orElse("");
        String sourceControlType = Optional.ofNullable(args.get("sourceControlType")).map(Object::toString).map(String::trim).orElse("");
        String processTypeId = Optional.ofNullable(args.get("processTypeId")).map(Object::toString).map(String::trim).orElse("");
        if (name.isEmpty()) throw new IllegalArgumentException("'name' es requerido");
        if (visibility.isEmpty()) throw new IllegalArgumentException("'visibility' es requerido (private|public)");
        if (sourceControlType.isEmpty()) throw new IllegalArgumentException("'sourceControlType' es requerido (Git|TFVC)");
        if (processTypeId.isEmpty()) throw new IllegalArgumentException("'processTypeId' es requerido (GUID)");
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> s = new HashMap<>();
        s.put("type","object");
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("name", Map.of("type","string","description","Nombre del proyecto"));
        props.put("description", Map.of("type","string","description","Descripción (opcional)"));
        props.put("visibility", Map.of("type","string","description","Visibilidad (private|public)"));
        props.put("sourceControlType", Map.of("type","string","description","Tipo de control de versiones (Git|TFVC)"));
        props.put("processTypeId", Map.of("type","string","description","Process template typeId (GUID)"));
        s.put("properties", props);
        s.put("required", List.of("name","visibility","sourceControlType","processTypeId"));
        return s;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("name", arguments.get("name").toString().trim());
        Object desc = arguments.get("description");
        if (desc != null && !desc.toString().isBlank()) body.put("description", desc.toString());
        body.put("visibility", arguments.get("visibility").toString().trim());
        body.put("capabilities", Map.of(
            "versioncontrol", Map.of("sourceControlType", arguments.get("sourceControlType").toString().trim()),
            "processTemplate", Map.of("templateTypeId", arguments.get("processTypeId").toString().trim())
        ));
        Map<String,Object> resp = azureService.postCoreApi("projects", null, body, "7.2-preview.4");
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        if (resp.containsKey("id") || resp.containsKey("name")) {
            return success(String.format("%s [%s]", String.valueOf(resp.getOrDefault("name","<sin nombre>")), String.valueOf(resp.getOrDefault("id","?"))));
        }
        return Map.of("isError", false, "raw", resp);
    }
}
