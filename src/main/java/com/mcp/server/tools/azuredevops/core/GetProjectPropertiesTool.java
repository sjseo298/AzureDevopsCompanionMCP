package com.mcp.server.tools.azuredevops.core;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetProjectPropertiesTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_core_get_project_properties";
    private static final String DESC = "Obtiene propiedades de un proyecto (opcionalmente filtradas por 'keys')";

    @Autowired
    public GetProjectPropertiesTool(AzureDevOpsClientService service) { super(service); }

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
        String pid = Optional.ofNullable(args.get("projectId")).map(Object::toString).map(String::trim).orElse("");
        if (pid.isEmpty()) throw new IllegalArgumentException("'projectId' es requerido");
        if (!pid.matches("[0-9a-fA-F-]{36}")) throw new IllegalArgumentException("'projectId' debe ser GUID de 36 chars");
        // 'keys' es opcional; si vienen como array, validar que no tenga elementos vacíos
        Object keys = args.get("keys");
        if (keys instanceof List) {
            @SuppressWarnings("unchecked") List<Object> list = (List<Object>) keys;
            for (Object k : list) {
                if (k == null || k.toString().trim().isEmpty()) {
                    throw new IllegalArgumentException("'keys' no debe contener elementos vacíos");
                }
            }
        }
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio no disponible en tests");
        String pid = arguments.get("projectId").toString().trim();
        Map<String,String> q = new LinkedHashMap<>(); q.put("api-version","7.2-preview.1");

        // Construir query param 'keys' si se proporcionó
        Object keysArg = arguments.get("keys");
        if (keysArg != null) {
            String keysCsv;
            if (keysArg instanceof List) {
                @SuppressWarnings("unchecked") List<Object> list = (List<Object>) keysArg;
                keysCsv = String.join(",", list.stream().map(o -> o == null ? "" : o.toString().trim()).filter(s -> !s.isEmpty()).toList());
            } else {
                keysCsv = keysArg.toString().trim();
            }
            if (!keysCsv.isEmpty()) q.put("keys", keysCsv);
        }

        Map<String,Object> resp = azureService.getCoreApi("projects/"+pid+"/properties", q);
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return Map.of("isError", false, "raw", resp);
    }
}
