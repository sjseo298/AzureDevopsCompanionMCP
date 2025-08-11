package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Helper para operaciones de proyectos (Core) aislando la lógica de las clases Tool.
 */
@Service
public class ProjectsHelper {

    private final AzureDevOpsClientService azureService;

    public ProjectsHelper(AzureDevOpsClientService azureService) {
        this.azureService = azureService;
    }

    /**
     * Valida parámetros requeridos para creación de proyecto.
     */
    public void validateCreateProject(String name, String visibility, String sourceControlType, String processTypeId) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("'name' es requerido");
        if (!Set.of("private","public").contains(nonNullTrim(visibility))) throw new IllegalArgumentException("'visibility' debe ser private|public");
        if (!Set.of("Git","TFVC").contains(nonNullTrim(sourceControlType))) throw new IllegalArgumentException("'sourceControlType' debe ser Git|TFVC");
        String p = nonNullTrim(processTypeId);
        if (!p.matches("[0-9a-fA-F-]{36}")) throw new IllegalArgumentException("'processTypeId' debe ser GUID de 36 chars");
    }

    private String nonNullTrim(String s) { return s == null ? "" : s.trim(); }

    /**
     * Construye el body JSON para la creación.
     */
    public Map<String,Object> buildCreateProjectBody(String name, String description, String visibility, String sourceControlType, String processTypeId) {
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("name", name.trim());
        if (description != null && !description.isBlank()) body.put("description", description);
        body.put("visibility", visibility.trim());
        body.put("capabilities", Map.of(
            "versioncontrol", Map.of("sourceControlType", sourceControlType.trim()),
            "processTemplate", Map.of("templateTypeId", processTypeId.trim())
        ));
        return body;
    }

    /**
     * Ejecuta la creación del proyecto devolviendo el mapa crudo de la API.
     */
    public Map<String,Object> createProject(Map<String,Object> body) {
        return azureService.postCoreApi("projects", null, body, "7.2-preview.4");
    }

    /**
     * Formatea la respuesta exitosa devolviendo nombre y ID si están presentes.
     * Devuelve null si no se cumple estructura simple y se debe retornar raw.
     */
    public String formatCreateProjectResponse(Map<String,Object> resp) {
        if (resp.containsKey("id") || resp.containsKey("name")) {
            return String.format("%s [%s]", String.valueOf(resp.getOrDefault("name","<sin nombre>")), String.valueOf(resp.getOrDefault("id","?")));
        }
        return null;
    }

    // --- Delete Project helpers ---

    public void validateProjectId(String projectId) {
        if (projectId == null || projectId.trim().isEmpty()) throw new IllegalArgumentException("'projectId' es requerido");
        if (!projectId.trim().matches("[0-9a-fA-F-]{36}")) throw new IllegalArgumentException("'projectId' debe ser GUID de 36 chars");
    }

    public Map<String,Object> deleteProject(String projectId) {
        return azureService.deleteCoreApi("projects/"+projectId.trim(), null, "7.2-preview.4");
    }

    public String formatDeleteProjectResponse(Map<String,Object> resp) {
        // Azure DevOps retorna info del operation o proyecto; si hay id o status, lo mostramos.
        Object id = resp.get("id");
        Object name = resp.get("name");
        Object status = resp.get("status");
        if (id != null || status != null) {
            StringBuilder sb = new StringBuilder("Eliminación de proyecto iniciada");
            if (name != null) sb.append(": ").append(name);
            if (id != null) sb.append(" [").append(id).append("]");
            if (status != null) sb.append(" (status: ").append(status).append(")");
            return sb.toString();
        }
        return null;
    }

    // --- Get Project helpers ---

    public Map<String,Object> fetchProject(String projectId) {
        Map<String,String> q = new LinkedHashMap<>();
        q.put("api-version","7.2-preview.4");
        return azureService.getCoreApi("projects/"+projectId.trim(), q);
    }

    public String formatProjectResponse(Map<String,Object> resp) {
        if (resp.containsKey("id") || resp.containsKey("name")) {
            return String.format("%s [%s]", String.valueOf(resp.getOrDefault("name","<sin nombre>")), String.valueOf(resp.getOrDefault("id","?")));
        }
        return null;
    }

    // --- Update Project helpers ---

    public void validateUpdateProject(String projectId, Object nameObj, Object descObj) {
        validateProjectId(projectId);
        boolean hasName = nameObj != null && !nameObj.toString().trim().isEmpty();
        boolean hasDesc = descObj != null && !descObj.toString().trim().isEmpty();
        if (!hasName && !hasDesc) {
            throw new IllegalArgumentException("Debe proporcionar 'name' y/o 'description' para actualizar");
        }
    }

    public Map<String,Object> buildUpdateProjectBody(Object nameObj, Object descObj) {
        Map<String,Object> body = new LinkedHashMap<>();
        if (nameObj != null && !nameObj.toString().isBlank()) body.put("name", nameObj.toString().trim());
        if (descObj != null && !descObj.toString().isBlank()) body.put("description", descObj.toString().trim());
        return body;
    }

    public Map<String,Object> updateProject(String projectId, Map<String,Object> body) {
        return azureService.patchCoreApi("projects/"+projectId.trim(), null, body, "7.2-preview.4");
    }

    public String formatUpdateProjectResponse(Map<String,Object> resp) {
        return formatProjectResponse(resp); // reutiliza formato simple
    }

    // --- List Projects helpers ---

    public void validateListProjects(Object state, Object top) {
        if (state != null && !state.toString().isBlank()) {
            String s = state.toString().trim();
            if (!Set.of("WellFormed","CreatePending","Deleted").contains(s)) {
                throw new IllegalArgumentException("'state' debe ser uno de: WellFormed|CreatePending|Deleted");
            }
        }
        if (top != null) {
            try {
                int t = Integer.parseInt(top.toString());
                if (t < 1 || t > 1000) throw new IllegalArgumentException("'top' debe estar entre 1 y 1000");
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("'top' debe ser numérico");
            }
        }
    }

    public Map<String,String> buildListProjectsQuery(Object state, Object top, Object continuationToken) {
        Map<String,String> q = new LinkedHashMap<>();
        if (state != null && !state.toString().isBlank()) q.put("stateFilter", state.toString().trim());
        if (top != null) q.put("$top", top.toString());
        if (continuationToken != null && !continuationToken.toString().isBlank()) q.put("continuationToken", continuationToken.toString().trim());
        q.put("api-version","7.2-preview.4");
        return q;
    }

    public Map<String,Object> fetchProjects(Map<String,String> query) {
        return azureService.getCoreApi("projects", query);
    }

    @SuppressWarnings("unchecked")
    public String formatProjectsList(Map<String,Object> resp) {
        Object count = resp.get("count"); Object value = resp.get("value");
        if (!(count instanceof Number) || !(value instanceof java.util.List)) return null;
        java.util.List<java.util.Map<String,Object>> items = (java.util.List<java.util.Map<String,Object>>) value;
        if (items.isEmpty()) return "Sin resultados";
        StringBuilder sb = new StringBuilder(); int idx=1; for (java.util.Map<String,Object> it: items) {
            sb.append(idx++).append(") ")
              .append(String.valueOf(it.getOrDefault("name","<sin nombre>")))
              .append(" [").append(String.valueOf(it.getOrDefault("id","?"))).append("]\n");
        }
        return sb.toString();
    }

    // --- Project Properties helpers ---

    public void validateProjectProperties(String projectId, Object keysArg) {
        validateProjectId(projectId);
        if (keysArg instanceof Iterable<?>) {
            for (Object k : (Iterable<?>) keysArg) {
                if (k == null || k.toString().trim().isEmpty()) {
                    throw new IllegalArgumentException("'keys' no debe contener elementos vacíos");
                }
            }
        }
    }

    public Map<String,String> buildProjectPropertiesQuery(Object keysArg) {
        Map<String,String> q = new LinkedHashMap<>();
        q.put("api-version","7.2-preview.1");
        if (keysArg != null) {
            String keysCsv;
            if (keysArg instanceof Iterable<?>) {
                StringBuilder sb = new StringBuilder();
                for (Object k : (Iterable<?>) keysArg) {
                    if (k == null) continue;
                    String s = k.toString().trim();
                    if (s.isEmpty()) continue;
                    if (sb.length() > 0) sb.append(',');
                    sb.append(s);
                }
                keysCsv = sb.toString();
            } else {
                keysCsv = keysArg.toString().trim();
            }
            if (!keysCsv.isEmpty()) q.put("keys", keysCsv);
        }
        return q;
    }

    public Map<String,Object> fetchProjectProperties(String projectId, Map<String,String> query) {
        return azureService.getCoreApi("projects/"+projectId.trim()+"/properties", query);
    }
}
