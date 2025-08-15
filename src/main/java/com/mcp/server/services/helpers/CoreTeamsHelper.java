package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Helper de Core Teams: concentra la lógica de obtención y formateo de equipos categorizados.
 * Mantiene fuera de la clase Tool toda la lógica de negocio/formateo.
 */
@Service
public class CoreTeamsHelper {

    private final AzureDevOpsClientService azureService;

    public CoreTeamsHelper(AzureDevOpsClientService azureService) {
        this.azureService = azureService;
    }

    /**
     * Valida el GUID de proyecto esperado por este endpoint.
     * @param projectId GUID
     */
    public void validateProjectId(String projectId) {
        if (projectId == null || projectId.trim().isEmpty()) {
            throw new IllegalArgumentException("'projectId' es requerido");
        }
        String pid = projectId.trim();
        if (!pid.matches("[0-9a-fA-F-]{36}")) {
            throw new IllegalArgumentException("'projectId' debe ser GUID de 36 chars");
        }
    }

    /**
     * Ejecuta la llamada remota y devuelve el mapa crudo que retorna el cliente.
     */
    public Map<String,Object> fetchCategorizedTeams(String projectId, boolean mine) {
        Map<String,String> q = new LinkedHashMap<>();
        if (mine) q.put("mine","true");
        q.put("api-version","7.2-preview.1");
        return azureService.getCoreApi("projects/"+projectId.trim()+"/teams", q);
    }

    /**
     * Formatea la respuesta de listado (count + value[]).
     * Si no corresponde a la estructura esperada, retorna null para que el caller decida.
     */
    @SuppressWarnings("unchecked")
    public String formatTeamsList(Map<String,Object> resp) {
        Object count = resp.get("count");
        Object value = resp.get("value");
        if (!(count instanceof Number) || !(value instanceof List)) return null;
        List<Map<String,Object>> items = (List<Map<String,Object>>) value;
        if (items.isEmpty()) return "Sin resultados";
        StringBuilder sb = new StringBuilder();
        int i=1; for (Map<String,Object> it: items) {
            sb.append(i++).append(") ")
              .append(String.valueOf(it.getOrDefault("name","<sin nombre>")))
              .append(" [").append(String.valueOf(it.getOrDefault("id","?"))).append("]\n");
        }
        return sb.toString();
    }

    // --- List Teams by Project (with pagination) ---

    public void validateListProjectTeams(String projectId, Object topObj, Object skipObj) {
        validateProjectId(projectId);
        if (topObj != null) {
            int t;
            try { t = Integer.parseInt(topObj.toString()); } catch (NumberFormatException e) { throw new IllegalArgumentException("'top' debe ser numérico"); }
            if (t < 1 || t > 1000) throw new IllegalArgumentException("'top' debe estar entre 1 y 1000");
        }
        if (skipObj != null) {
            int s;
            try { s = Integer.parseInt(skipObj.toString()); } catch (NumberFormatException e) { throw new IllegalArgumentException("'skip' debe ser numérico"); }
            if (s < 0) throw new IllegalArgumentException("'skip' debe ser >= 0");
        }
    }

    public Map<String,String> buildListProjectTeamsQuery(Object topObj, Object skipObj) {
        Map<String,String> q = new LinkedHashMap<>();
        q.put("api-version","7.2-preview.3");
        if (topObj != null) q.put("top", topObj.toString());
        if (skipObj != null) q.put("skip", skipObj.toString());
        return q;
    }

    public Map<String,Object> fetchProjectTeams(String projectId, Map<String,String> query) {
        return azureService.getCoreApi("projects/"+projectId.trim()+"/teams", query);
    }

    // --- List All Teams (organization-level) ---

    public void validateListAllTeams(Object topObj, Object skipObj) {
        if (topObj != null) {
            int t;
            try { t = Integer.parseInt(topObj.toString()); } catch (NumberFormatException e) { throw new IllegalArgumentException("'top' debe ser numérico"); }
            if (t < 1 || t > 1000) throw new IllegalArgumentException("'top' debe estar entre 1 y 1000");
        }
        if (skipObj != null) {
            int s;
            try { s = Integer.parseInt(skipObj.toString()); } catch (NumberFormatException e) { throw new IllegalArgumentException("'skip' debe ser numérico"); }
            if (s < 0) throw new IllegalArgumentException("'skip' debe ser >= 0");
        }
    }

    public Map<String,String> buildAllTeamsQuery(boolean mine, Object topObj, Object skipObj) {
        Map<String,String> q = new LinkedHashMap<>();
        q.put("api-version","7.2-preview.3");
        if (mine) q.put("mine","true");
        if (topObj != null) q.put("top", topObj.toString());
        if (skipObj != null) q.put("skip", skipObj.toString());
        return q;
    }

    public Map<String,Object> fetchAllTeams(Map<String,String> query) {
        return azureService.getCoreApi("teams", query);
    }

    // --- Create Team helpers ---

    public void validateCreateTeam(String projectId, String name) {
        validateProjectId(projectId);
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("'name' es requerido");
        }
    }

    public Map<String,Object> buildCreateTeamBody(String name, String description) {
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("name", name.trim());
        if (description != null && !description.isBlank()) body.put("description", description);
        return body;
    }

    public Map<String,Object> createTeam(String projectId, Map<String,Object> body) {
        return azureService.postCoreApi("projects/"+projectId.trim()+"/teams", null, body, "7.2-preview.3");
    }

    public String formatCreateTeamResponse(Map<String,Object> resp) {
        if (resp.containsKey("id") || resp.containsKey("name")) {
            return String.format("%s [%s]", String.valueOf(resp.getOrDefault("name","<sin nombre>")), String.valueOf(resp.getOrDefault("id","?")));
        }
        return null;
    }

    // --- Team Members helpers ---

    public void validateTeamMembers(String projectId, String teamId) {
        validateProjectId(projectId);
        if (teamId == null || teamId.trim().isEmpty()) {
            throw new IllegalArgumentException("'teamId' es requerido");
        }
    }

    public Map<String,String> buildTeamMembersQuery() {
        Map<String,String> q = new LinkedHashMap<>();
        q.put("api-version","7.2-preview.3");
        return q;
    }

    public Map<String,Object> fetchTeamMembers(String projectId, String teamId) {
        return azureService.getCoreApi("projects/"+projectId.trim()+"/teams/"+teamId.trim()+"/members", buildTeamMembersQuery());
    }

    @SuppressWarnings("unchecked")
    public String formatTeamMembersList(Map<String,Object> resp) {
        Object count = resp.get("count");
        Object value = resp.get("value");
        if (!(count instanceof Number) || !(value instanceof List)) return null;
        List<Map<String,Object>> items = (List<Map<String,Object>>) value;
        if (items.isEmpty()) return "Sin resultados";
        StringBuilder sb = new StringBuilder();
        int idx = 1;
        for (Map<String,Object> it : items) {
            sb.append(idx++).append(") ")
              .append(String.valueOf(it.getOrDefault("displayName","<sin nombre>")))
              .append(" [").append(String.valueOf(it.getOrDefault("id","?"))).append("]\n");
        }
        return sb.toString();
    }

    // --- Single Team helpers ---

    public void validateGetTeam(String projectId, String teamId) {
        validateProjectId(projectId);
        if (teamId == null || teamId.trim().isEmpty()) {
            throw new IllegalArgumentException("'teamId' es requerido");
        }
    }

    public Map<String,String> buildGetTeamQuery() {
        Map<String,String> q = new LinkedHashMap<>();
        q.put("api-version","7.2-preview.3");
        return q;
    }

    public Map<String,Object> fetchTeam(String projectId, String teamId) {
        return azureService.getCoreApi("projects/"+projectId.trim()+"/teams/"+teamId.trim(), buildGetTeamQuery());
    }

    public String formatTeamResponse(Map<String,Object> resp) {
        if (resp.containsKey("id") || resp.containsKey("name")) {
            return String.format("%s [%s]", String.valueOf(resp.getOrDefault("name","<sin nombre>")), String.valueOf(resp.getOrDefault("id","?")));
        }
        return null;
    }

    // --- Delete Team helpers ---

    public void validateDeleteTeam(String projectId, String teamId) {
        validateProjectId(projectId);
        if (teamId == null || teamId.trim().isEmpty()) {
            throw new IllegalArgumentException("'teamId' es requerido");
        }
    }

    public Map<String,Object> deleteTeam(String projectId, String teamId) {
        return azureService.deleteCoreApi("projects/"+projectId.trim()+"/teams/"+teamId.trim(), null, "7.2-preview.3");
    }

    public String formatDeleteTeamResponse(Map<String,Object> resp) {
        Object id = resp.get("id");
        Object name = resp.get("name");
        Object status = resp.get("status");
        if (id != null || name != null || status != null) {
            StringBuilder sb = new StringBuilder("Eliminación de equipo");
            if (name != null) sb.append(": ").append(name);
            if (id != null) sb.append(" [").append(id).append("]");
            if (status != null) sb.append(" (status: ").append(status).append(")");
            return sb.toString();
        }
        return null;
    }

    // --- Update Team helpers ---

    public void validateUpdateTeam(String projectId, String teamId, Object nameObj, Object descObj) {
        validateProjectId(projectId);
        if (teamId == null || teamId.trim().isEmpty()) {
            throw new IllegalArgumentException("'teamId' es requerido");
        }
        boolean hasName = nameObj != null && !nameObj.toString().trim().isEmpty();
        boolean hasDesc = descObj != null && !descObj.toString().trim().isEmpty();
        if (!hasName && !hasDesc) {
            throw new IllegalArgumentException("Debe proporcionar 'name' y/o 'description' para actualizar");
        }
    }

    public Map<String,Object> buildUpdateTeamBody(Object nameObj, Object descObj) {
        Map<String,Object> body = new LinkedHashMap<>();
        if (nameObj != null && !nameObj.toString().isBlank()) body.put("name", nameObj.toString().trim());
        if (descObj != null && !descObj.toString().isBlank()) body.put("description", descObj.toString().trim());
        return body;
    }

    public Map<String,Object> updateTeam(String projectId, String teamId, Map<String,Object> body) {
        return azureService.patchCoreApi("projects/"+projectId.trim()+"/teams/"+teamId.trim(), null, body, "7.2-preview.3");
    }

    public String formatUpdateTeamResponse(Map<String,Object> resp) { return formatTeamResponse(resp); }
}
