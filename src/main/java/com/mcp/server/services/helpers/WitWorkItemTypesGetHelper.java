package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class WitWorkItemTypesGetHelper {
    private final AzureDevOpsClientService client;
    public WitWorkItemTypesGetHelper(AzureDevOpsClientService client) {
        this.client = client;
    }
    /**
     * Obtiene el detalle de un tipo de work item en un proyecto.
     * Uso correcto del área WIT: /{project}/_apis/wit/workitemtypes/{type}?api-version=...
     * Antes se usaba getWorkApi con path que incluía 'wit/' y query en el segmento, provocando:
     *  - Ruta errónea: /_apis/work/wit/workitemtypes/{type}?api-version=...
     *  - Inclusión de '?api-version=..' dentro de un pathSegment => 400 BAD_REQUEST (dangerous path).
     */
    public Map<String,Object> getType(String project, String type, String apiVersion) {
        Map<String,String> q = new LinkedHashMap<>();
        if (apiVersion != null && !apiVersion.isBlank()) {
            q.put("api-version", apiVersion);
        }
        // getWitApiWithQuery añadirá api-version default si no la pasamos; aquí la forzamos explícitamente.
        return client.getWitApiWithQuery(project, null, "workitemtypes/" + type, q, apiVersion);
    }
}
