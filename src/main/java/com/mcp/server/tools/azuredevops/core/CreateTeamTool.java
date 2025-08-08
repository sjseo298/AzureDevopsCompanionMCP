package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CreateTeamTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_core_create_team";
    private static final String DESC = "Crea un equipo dentro de un proyecto";

    @Autowired
    public CreateTeamTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> s = new HashMap<>();
        s.put("type","object");
        s.put("properties", Map.of(
            "projectId", Map.of("type","string","description","GUID del proyecto"),
            "name", Map.of("type","string","description","Nombre del equipo"),
            "description", Map.of("type","string","description","Descripción del equipo (opcional)")
        ));
        s.put("required", List.of("projectId","name"));
        return s;
    }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        String pid = Optional.ofNullable(args.get("projectId")).map(Object::toString).map(String::trim).orElse("");
        String name = Optional.ofNullable(args.get("name")).map(Object::toString).map(String::trim).orElse("");
        if (pid.isEmpty() || name.isEmpty()) throw new IllegalArgumentException("'projectId' y 'name' son requeridos");
        if (!pid.matches("[0-9a-fA-F-]{36}")) throw new IllegalArgumentException("'projectId' debe ser GUID de 36 chars");
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        String pid = arguments.get("projectId").toString().trim();
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("name", arguments.get("name").toString().trim());
        Object d = arguments.get("description"); if (d!=null && !d.toString().isBlank()) body.put("description", d.toString());
        Map<String,Object> resp = azureService.postCoreApi("projects/"+pid+"/teams", null, body, "7.2-preview.3");
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        if (resp.containsKey("id") || resp.containsKey("name")) {
            return success(String.format("%s [%s]", String.valueOf(resp.getOrDefault("name","<sin nombre>")), String.valueOf(resp.getOrDefault("id","?"))));
        }
        return Map.of("isError", false, "raw", resp);
    }
}
