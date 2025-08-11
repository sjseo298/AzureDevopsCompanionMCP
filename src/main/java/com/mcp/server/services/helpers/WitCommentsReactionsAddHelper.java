package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

/**
 * Helper para agregar una reacción a un comentario de un work item.
 */
@Component
public class WitCommentsReactionsAddHelper {
    private final AzureDevOpsClientService azureService;
    private static final List<String> VALID_TYPES = List.of("like","dislike","heart","hooray","smile","confused");

    public WitCommentsReactionsAddHelper(AzureDevOpsClientService azureService) {
        this.azureService = azureService;
    }

    public void validate(String project, Object wiObj, Object ciObj, Object typeObj) {
        if (project == null || project.isBlank()) throw new IllegalArgumentException("Parámetro 'project' es obligatorio.");
        if (wiObj == null || !wiObj.toString().matches("\\d+")) throw new IllegalArgumentException("'workItemId' es requerido y debe ser numérico");
        if (ciObj == null || !ciObj.toString().matches("\\d+")) throw new IllegalArgumentException("'commentId' es requerido y debe ser numérico");
        if (typeObj == null) throw new IllegalArgumentException("'type' es requerido");
        String type = typeObj.toString().toLowerCase();
        if (!VALID_TYPES.contains(type)) throw new IllegalArgumentException("'type' inválido. Valores: " + String.join(", ", VALID_TYPES));
    }

    public Map<String,Object> addReaction(String project, String team, String wi, String ci, String type, String apiVersion) {
        String endpoint = "workItems/" + wi + "/comments/" + ci + "/reactions/" + type;
        return azureService.putWitApi(project, team, endpoint, Map.of(), apiVersion);
    }

    public String formatAddResponse(Map<String,Object> resp, String type) {
        Object count = resp.get("count");
        Object me = resp.get("isCurrentUserEngaged");
        return "Reacción agregada: " + type + " (count=" + (count!=null?count:1) + ", me=" + Boolean.TRUE.equals(me) + ")";
    }
}
