package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Helper para actualizar el texto de un comentario de un work item.
 */
@Component
public class WitCommentsUpdateHelper {
    private final AzureDevOpsClientService azureService;

    public WitCommentsUpdateHelper(AzureDevOpsClientService azureService) {
        this.azureService = azureService;
    }

    public void validate(String project, Object wiObj, Object ciObj, Object textObj) {
        if (project == null || project.isBlank()) throw new IllegalArgumentException("Parámetro 'project' es obligatorio.");
        if (wiObj == null || !wiObj.toString().matches("\\d+")) throw new IllegalArgumentException("'workItemId' es requerido y debe ser numérico");
        if (ciObj == null || !ciObj.toString().matches("\\d+")) throw new IllegalArgumentException("'commentId' es requerido y debe ser numérico");
        if (textObj == null || textObj.toString().trim().isEmpty()) throw new IllegalArgumentException("'text' es requerido");
    }

    public Map<String,Object> updateComment(String project, String team, String wi, String ci, String text, String apiVersion) {
        String endpoint = "workItems/" + wi + "/comments/" + ci;
        Map<String,Object> body = Map.of("text", text);
        return azureService.patchWitApi(project, team, endpoint, body, apiVersion);
    }

    public String formatUpdateResponse(Map<String,Object> resp) {
        Object id = resp.get("id");
        Object ver = resp.get("version");
        return "Comentario actualizado id=" + (id!=null?id:"?") + ", version=" + (ver!=null?ver:"?");
    }
}
