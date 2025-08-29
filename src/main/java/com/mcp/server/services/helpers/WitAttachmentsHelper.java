package com.mcp.server.services.helpers;

import org.springframework.http.MediaType;
import com.mcp.server.services.AzureDevOpsClientService;
import java.util.Base64;
import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;

/**
 * Helper para operaciones de adjuntos (attachments) a nivel organización.
 */
@Component
public class WitAttachmentsHelper {

    private final AzureDevOpsClientService azureService;

    public WitAttachmentsHelper(AzureDevOpsClientService azureService) {
        this.azureService = azureService;
    }

    public void validateCreate(String fileName, String dataBase64) {
        if (fileName == null || fileName.isBlank() || dataBase64 == null || dataBase64.isBlank()) {
            throw new IllegalArgumentException("'fileName' y 'dataBase64' son requeridos");
        }
        try { Base64.getDecoder().decode(dataBase64); } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("'dataBase64' no es base64 válido");
        }
    }

    public byte[] decodeBase64(String dataBase64) {
        return Base64.getDecoder().decode(dataBase64.trim());
    }

    public String sanitizeContentType(String ct) {
        if (ct == null || ct.isBlank()) return "application/octet-stream";
        return ct.trim();
    }

    public Map<String,Object> createAttachment(String fileName, byte[] data, String contentType) {
        Map<String,String> query = new LinkedHashMap<>();
        query.put("fileName", fileName);
        // Azure DevOps requiere application/octet-stream para subir el binario del attachment
        // Ignoramos 'contentType' para el header de subida y forzamos octet-stream
        MediaType ct = MediaType.APPLICATION_OCTET_STREAM;
        return azureService.postCoreBinary("wit/attachments", query, data, "7.2-preview", ct);
    }

    /**
     * Asocia un attachment recién creado (url completa) a un Work Item agregando relación AttachedFile.
     * Devuelve respuesta del PATCH del work item o un Map con error.
     */
    public Map<String,Object> linkAttachmentToWorkItem(String project, int workItemId, String attachmentUrl, String comment, String apiVersion) {
        if (project == null || project.isBlank()) {
            return Map.of("isHttpError", true, "message", "'project' es requerido para asociar el adjunto");
        }
        if (attachmentUrl == null || attachmentUrl.isBlank()) {
            return Map.of("isHttpError", true, "message", "'attachmentUrl' vacío");
        }
        List<Map<String,Object>> patch = new ArrayList<>();
        Map<String,Object> rel = new LinkedHashMap<>();
        rel.put("rel", "AttachedFile");
        rel.put("url", attachmentUrl.trim());
        if (comment != null && !comment.isBlank()) {
            rel.put("attributes", Map.of("comment", comment));
        }
        patch.add(Map.of(
            "op", "add",
            "path", "/relations/-",
            "value", rel
        ));
        Map<String,String> query = new LinkedHashMap<>();
        query.put("api-version", apiVersion != null && !apiVersion.isBlank() ? apiVersion : "7.2-preview");
        return azureService.patchWitApiWithQuery(project, null, "workitems/"+workItemId, query, patch, apiVersion, MediaType.valueOf("application/json-patch+json"));
    }

    public String formatCreateResponse(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Respuesta vacía)";
        String id = Objects.toString(data.get("id"), null);
        String url = Objects.toString(data.get("url"), null);
        if (id != null || url != null) {
            StringBuilder sb = new StringBuilder("=== Attachment Created ===\n\n");
            if (id != null) sb.append("ID: ").append(id).append("\n");
            if (url != null) sb.append("URL: ").append(url).append("\n");
            return sb.toString();
        }
        return data.toString();
    }

    // --- Métodos para eliminar adjunto ---
    public void validateDelete(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("'id' es requerido");
    }

    /**
     * Elimina un attachment. Requiere proyecto para el endpoint.
     * @param project Nombre del proyecto
     * @param id ID del attachment
     * @return Respuesta del DELETE
     */
    public Map<String,Object> deleteAttachment(String project, String id) {
        if (project == null || project.isBlank()) {
            // Fallback al método sin proyecto (puede fallar en algunos casos)
            return azureService.deleteCoreApi("wit/attachments/"+id.trim(), null, "7.2-preview");
        }
        return azureService.deleteWitApiWithQuery(project, null, "attachments/"+id.trim(), 
            Map.of("api-version", "7.2-preview"), "7.2-preview");
    }

    /**
     * Método legacy - elimina attachment sin especificar proyecto
     * @deprecated Usar deleteAttachment(project, id) preferentemente
     */
    @Deprecated
    public Map<String,Object> deleteAttachment(String id) {
        return deleteAttachment(null, id);
    }

    public String formatDeleteResponse(Map<String,Object> resp) {
        // Si hay error, el Tool lo formatea. Si no, salida simple.
        return "Adjunto eliminado (si existía)";
    }

    // --- Métodos para obtener adjunto ---
    public void validateGet(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("'id' es requerido");
    }

    public Map<String,Object> getAttachment(String id) {
        Map<String,String> query = new LinkedHashMap<>();
        query.put("download","true");
        return azureService.getCoreBinary("wit/attachments/"+id.trim(), query, "7.2-preview");
    }

    public String formatGetResponse(Map<String,Object> resp) {
        String dataB64 = Objects.toString(resp.get("data"), null);
        String ct = Objects.toString(resp.get("contentType"), null);
        if (dataB64 != null) {
            int size = java.util.Base64.getDecoder().decode(dataB64).length;
            StringBuilder sb = new StringBuilder("=== Attachment Downloaded ===\n\n");
            if (ct != null) sb.append("Content-Type: ").append(ct).append("\n");
            sb.append("Bytes: ").append(size).append("\n");
            return sb.toString();
        }
        return resp.toString();
    }
}
