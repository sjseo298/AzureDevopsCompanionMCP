package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import java.util.Map;

/**
 * Helper para obtener una versión específica de un comentario de un work item.
 */
public class WitCommentsVersionsGetHelper {
    private final AzureDevOpsClientService azureService;

    public WitCommentsVersionsGetHelper(AzureDevOpsClientService azureService) {
        this.azureService = azureService;
    }

    public void validate(String project, Object wiObj, Object ciObj, Object verObj) {
        if (project == null || project.isBlank()) throw new IllegalArgumentException("Parámetro 'project' es obligatorio.");
        if (wiObj == null || !wiObj.toString().matches("\\d+")) throw new IllegalArgumentException("'workItemId' es requerido y debe ser numérico");
        if (ciObj == null || !ciObj.toString().matches("\\d+")) throw new IllegalArgumentException("'commentId' es requerido y debe ser numérico");
        if (verObj == null || !verObj.toString().matches("\\d+")) throw new IllegalArgumentException("'version' es requerido y debe ser numérico");
    }

    public Map<String,Object> fetchVersion(String project, String team, String wi, String ci, String ver, String apiVersion) {
        String endpoint = "workItems/" + wi + "/comments/" + ci + "/versions/" + ver;
        return azureService.getWitApiWithQuery(project, team, endpoint, null, apiVersion);
    }

    public String formatVersionResponse(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Sin datos)";
        Object ver = data.get("version");
        Object text = data.get("text");
        return "Comentario v" + (ver != null ? ver : "?") + ":\n" + (text != null ? text.toString() : "");
    }
}
