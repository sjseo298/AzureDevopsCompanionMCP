package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Map;
import org.springframework.http.MediaType;

/**
 * Helper para operaciones relacionadas con Avatares (Graph VSSPS).
 */
@Service
public class AvatarsHelper {

    private final AzureDevOpsClientService azureService;

    public AvatarsHelper(AzureDevOpsClientService azureService) {
        this.azureService = azureService;
    }

    public void validateSubjectDescriptor(String subjectDescriptor) {
        if (subjectDescriptor == null || subjectDescriptor.trim().isEmpty()) {
            throw new IllegalArgumentException("'subjectDescriptor' es requerido");
        }
    }

    /**
     * Recupera avatar binario (base64 + contentType envuelto por el cliente) desde VSSPS.
     */
    public Map<String,Object> fetchAvatar(String subjectDescriptor) {
        return azureService.getVsspsBinary("graph/avatars/"+subjectDescriptor.trim()+"?api-version=7.2-preview.1");
    }

    /**
     * Punto de extensión futuro para formatear salida (por ahora retorna null para usar raw).
     */
    public String formatAvatarResponse(Map<String,Object> resp) { return null; }

    // --- Set / Update Avatar helpers ---

    public void validateSetAvatar(String subjectDescriptor, String dataBase64) {
        if (subjectDescriptor == null || subjectDescriptor.trim().isEmpty()) {
            throw new IllegalArgumentException("'subjectDescriptor' es requerido");
        }
        if (dataBase64 == null || dataBase64.trim().isEmpty()) {
            throw new IllegalArgumentException("'dataBase64' es requerido");
        }
        try { Base64.getDecoder().decode(dataBase64.trim()); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("'dataBase64' no es base64 válido"); }
    }

    public String sanitizeContentType(String ct) {
        if (ct == null || ct.isBlank()) return "image/png"; // default
        return ct.trim();
    }

    public byte[] decodeBase64(String dataBase64) {
        return Base64.getDecoder().decode(dataBase64.trim());
    }

    public Map<String,Object> updateAvatar(String subjectDescriptor, byte[] bytes, String contentType) {
        MediaType mediaType = MediaType.parseMediaType(contentType);
        return azureService.putVsspsBinary("graph/avatars/"+subjectDescriptor.trim()+"?api-version=7.2-preview.1", bytes, mediaType);
    }
}
