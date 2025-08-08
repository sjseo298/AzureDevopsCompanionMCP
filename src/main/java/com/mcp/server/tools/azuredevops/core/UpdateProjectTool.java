package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class UpdateProjectTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_core_update_project";
    private static final String DESC = "Actualiza nombre/descripcion/visibilidad de un proyecto";

    @Autowired
    public UpdateProjectTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> s = new HashMap<>();
        s.put("type","object");
        s.put("properties", Map.of(
            "projectId", Map.of("type","string","description","GUID del proyecto"),
            "name", Map.of("type","string","description","Nuevo nombre"),
            "description", Map.of("type","string","description","Nueva descripción")
        ));
        s.put("required", List.of("projectId"));
        return s;
    }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        String pid = Optional.ofNullable(args.get("projectId")).map(Object::toString).map(String::trim).orElse("");
        if (pid.isEmpty()) throw new IllegalArgumentException("'projectId' es requerido");
        if (!pid.matches("[0-9a-fA-F-]{36}")) throw new IllegalArgumentException("'projectId' debe ser GUID de 36 chars");
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        String pid = arguments.get("projectId").toString().trim();
        Map<String,Object> body = new LinkedHashMap<>();
        Object n = arguments.get("name"); if (n!=null && !n.toString().isBlank()) body.put("name", n.toString());
        Object d = arguments.get("description"); if (d!=null && !d.toString().isBlank()) body.put("description", d.toString());
        Map<String,Object> resp = azureService.patchCoreApi("projects/"+pid, null, body, "7.2-preview.4");
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return Map.of("isError", false, "raw", resp);
    }
}
