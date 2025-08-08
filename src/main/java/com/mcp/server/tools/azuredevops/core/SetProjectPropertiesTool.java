package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SetProjectPropertiesTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_core_set_project_properties";
    private static final String DESC = "Crea/actualiza/elimina propiedades personalizadas de un proyecto";

    @Autowired
    public SetProjectPropertiesTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type","object",
            "properties", Map.of(
                "projectId", Map.of("type","string","description","GUID del proyecto"),
                "name", Map.of("type","string","description","Nombre de la propiedad"),
                "value", Map.of("type","string","description","Valor de la propiedad")
            ),
            "required", List.of("projectId","name","value")
        );
    }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        String pid = Optional.ofNullable(args.get("projectId")).map(Object::toString).map(String::trim).orElse("");
        String name = Optional.ofNullable(args.get("name")).map(Object::toString).map(String::trim).orElse("");
        String value = Optional.ofNullable(args.get("value")).map(Object::toString).map(String::trim).orElse("");
        if (pid.isEmpty() || name.isEmpty() || value.isEmpty()) throw new IllegalArgumentException("'projectId', 'name' y 'value' son requeridos");
        if (!pid.matches("[0-9a-fA-F-]{36}")) throw new IllegalArgumentException("'projectId' debe ser GUID de 36 chars");
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        String pid = arguments.get("projectId").toString().trim();
        String name = arguments.get("name").toString().trim();
        String value = arguments.get("value").toString().trim();
        List<Object> patch = List.of(Map.of("op","add","path","/"+name,"value",value));
        Map<String,Object> resp = azureService.patchCoreApi("projects/"+pid+"/properties", null, patch, "7.2-preview.1");
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return Map.of("isError", false, "raw", resp);
    }
}
