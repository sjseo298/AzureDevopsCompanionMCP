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
}
        return resp.toString();
    }
    // --- Métodos para eliminar adjunto ---
    // --- Métodos para eliminar adjunto ---
    public void validateDelete(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("'id' es requerido");
    }

    public Map<String,Object> deleteAttachment(String id) {
        return azureService.deleteCoreApi("wit/attachments/"+id.trim(), null, null);
    }

    public String formatDeleteResponse(Map<String,Object> resp) {
        // Si hay error, el Tool lo formatea. Si no, salida simple.
        return "Adjunto eliminado (si existía)";
    }
package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Helper para operaciones de adjuntos (attachments) a nivel organización.
 */
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
        // El tipo MediaType debe resolverse en el Tool, aquí solo se pasa el string
        return azureService.postCoreBinary("wit/attachments", query, data, "7.2-preview", contentType);
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

    public Map<String,Object> deleteAttachment(String id) {
        return azureService.deleteCoreApi("wit/attachments/"+id.trim(), null, null);
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
