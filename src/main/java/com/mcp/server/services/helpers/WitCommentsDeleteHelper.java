package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Helper para eliminar un comentario de un work item.
 */
@Component
public class WitCommentsDeleteHelper {
    private final AzureDevOpsClientService azureService;

    public WitCommentsDeleteHelper(AzureDevOpsClientService azureService) {
        this.azureService = azureService;
    }

    public void validate(String project, Object wiObj, Object ciObj) {
        if (project == null || project.isBlank()) throw new IllegalArgumentException("Parámetro 'project' es obligatorio.");
        if (wiObj == null || !wiObj.toString().matches("\\d+")) throw new IllegalArgumentException("'workItemId' es requerido y debe ser numérico");
        if (ciObj == null || !ciObj.toString().matches("\\d+")) throw new IllegalArgumentException("'commentId' es requerido y debe ser numérico");
    }

    public Map<String,Object> deleteComment(String project, String team, String wi, String ci, String apiVersion) {
        String endpoint = "workItems/" + wi + "/comments/" + ci;
        return azureService.deleteWitApi(project, team, endpoint, apiVersion);
    }

    public String formatDeleteResponse(Map<String,Object> resp) {
        return "Comentario eliminado (si existía).";
    }
}
