package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import java.util.Map;

/**
 * Helper para agregar un comentario a un work item.
 */
public class WitCommentsAddHelper {
    private final AzureDevOpsClientService azureService;

    public WitCommentsAddHelper(AzureDevOpsClientService azureService) {
        this.azureService = azureService;
    }

    public void validate(String project, Object wiObj, Object textObj) {
        if (project == null || project.isBlank()) throw new IllegalArgumentException("Parámetro 'project' es obligatorio.");
        if (wiObj == null || !wiObj.toString().matches("\\d+")) throw new IllegalArgumentException("'workItemId' es requerido y debe ser numérico");
        if (textObj == null || textObj.toString().trim().isEmpty()) throw new IllegalArgumentException("'text' es requerido");
    }

    public Map<String,Object> addComment(String project, String team, String wi, String text, String apiVersion) {
        String endpoint = "workItems/" + wi + "/comments";
        Map<String,Object> body = Map.of("text", text);
        return azureService.postWitApi(project, team, endpoint, body, apiVersion);
    }

    public String formatAddResponse(Map<String,Object> resp) {
        Object id = resp.get("id");
        Object ver = resp.get("version");
        return "Comentario creado id=" + (id!=null?id:"?") + ", version=" + (ver!=null?ver:"?");
    }
}
