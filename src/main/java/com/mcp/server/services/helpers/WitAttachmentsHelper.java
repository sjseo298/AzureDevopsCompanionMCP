package com.mcp.server.services.helpers;

import org.springframework.http.MediaType;
import com.mcp.server.services.AzureDevOpsClientService;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public String inferProjectFromWorkItem(int workItemId, String apiVersion) {
        Map<String,String> query = new LinkedHashMap<>();
        query.put("fields", "System.TeamProject");
        query.put("api-version", apiVersion != null && !apiVersion.isBlank() ? apiVersion : "7.2-preview");
        Map<String,Object> wi = azureService.getWitApiWithQuery(null, null, "workitems/" + workItemId, query, apiVersion);
        Object fields = wi != null ? wi.get("fields") : null;
        if (fields instanceof Map<?,?> fm) {
            Object project = fm.get("System.TeamProject");
            if (project != null && !project.toString().isBlank()) return project.toString();
        }
        return null;
    }

    public Map<String,Object> attachBytesToWorkItem(String project, int workItemId, String fileName, byte[] data,
                                                    String contentType, String comment, String apiVersion) {
        String effectiveProject = project;
        String effectiveApiVersion = apiVersion != null && !apiVersion.isBlank() ? apiVersion : "7.2-preview";
        if (effectiveProject == null || effectiveProject.isBlank()) {
            effectiveProject = inferProjectFromWorkItem(workItemId, effectiveApiVersion);
        }
        if (effectiveProject == null || effectiveProject.isBlank()) {
            return Map.of("isError", true, "message", "No se pudo inferir 'project' desde workItemId=" + workItemId + ". Envíe project explícitamente.");
        }

        String effectiveFileName = fileName != null && !fileName.isBlank() ? fileName.trim() : "attachment.bin";
        String effectiveContentType = sanitizeContentType(contentType);
        Map<String,Object> createResp = createAttachment(effectiveFileName, data, effectiveContentType);
        String attachmentUrl = Objects.toString(createResp.get("url"), null);
        if (attachmentUrl == null || attachmentUrl.isBlank()) {
            Map<String,Object> out = new LinkedHashMap<>();
            out.put("isError", true);
            out.put("message", "No se obtuvo URL del attachment");
            out.put("attachment", createResp);
            return out;
        }

        Map<String,Object> linkResp = linkAttachmentToWorkItem(effectiveProject, workItemId, attachmentUrl, comment, effectiveApiVersion);
        boolean linkFailed = Boolean.TRUE.equals(linkResp.get("isHttpError")) || linkResp.get("message") != null || linkResp.get("typeKey") != null;
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("project", effectiveProject);
        result.put("workItemId", workItemId);
        result.put("fileName", effectiveFileName);
        result.put("contentType", effectiveContentType);
        result.put("bytes", data != null ? data.length : 0);
        result.put("url", attachmentUrl);
        String attachmentId = extractAttachmentId(createResp, attachmentUrl);
        if (attachmentId != null) result.put("attachmentId", attachmentId);
        if (comment != null && !comment.isBlank()) result.put("comment", comment);
        result.put("attachment", createResp);
        result.put("workItemPatch", linkResp);

        if (linkFailed) {
            result.put("isError", true);
            result.put("message", "Error asociando attachment al work item. Se intentó rollback.");
            if (attachmentId != null) result.put("rollback", deleteAttachment(effectiveProject, attachmentId));
            return result;
        }
        result.put("isError", false);
        result.put("message", "Archivo adjuntado correctamente");
        return result;
    }

    public String extractAttachmentId(Map<String,Object> createResp, String attachmentUrl) {
        String attachmentId = Objects.toString(createResp != null ? createResp.get("id") : null, null);
        if (attachmentId == null && attachmentUrl != null) {
            int idx = attachmentUrl.lastIndexOf('/');
            if (idx != -1) {
                String tail = attachmentUrl.substring(idx + 1);
                int q = tail.indexOf('?');
                if (q != -1) tail = tail.substring(0, q);
                if (!tail.isBlank()) attachmentId = tail;
            }
        }
        return attachmentId;
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

    public Map<String,Object> buildGetResponse(
            String id,
            Map<String,Object> resp,
            boolean includeBase64,
            Integer maxBase64Chars,
            boolean includeTextPreview,
            Integer maxTextChars,
            String outputPath
    ) {
        String dataB64 = Objects.toString(resp.get("data"), null);
        String ct = Objects.toString(resp.get("contentType"), MediaType.APPLICATION_OCTET_STREAM_VALUE);

        if (dataB64 == null) {
            return new LinkedHashMap<>(resp);
        }

        byte[] bytes = Base64.getDecoder().decode(dataB64);
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("attachmentId", id);
        out.put("contentType", ct);
        out.put("bytes", bytes.length);

        if (includeBase64) {
            int limit = (maxBase64Chars != null && maxBase64Chars > 0) ? maxBase64Chars : 200_000;
            if (dataB64.length() > limit) {
                out.put("dataBase64", dataB64.substring(0, limit));
                out.put("base64Truncated", true);
                out.put("base64ReturnedChars", limit);
                out.put("base64TotalChars", dataB64.length());
            } else {
                out.put("dataBase64", dataB64);
                out.put("base64Truncated", false);
            }
        }

        if (includeTextPreview && isTextLike(ct)) {
            int limit = (maxTextChars != null && maxTextChars > 0) ? maxTextChars : 8_000;
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (text.length() > limit) {
                out.put("textPreview", text.substring(0, limit));
                out.put("textPreviewTruncated", true);
                out.put("textPreviewChars", limit);
            } else {
                out.put("textPreview", text);
                out.put("textPreviewTruncated", false);
                out.put("textPreviewChars", text.length());
            }
        }

        if (outputPath != null && !outputPath.isBlank()) {
            try {
                Path path = Path.of(outputPath.trim()).toAbsolutePath().normalize();
                Path parent = path.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.write(path, bytes);
                out.put("savedToPath", path.toString());
            } catch (Exception e) {
                out.put("saveError", "No se pudo escribir outputPath: " + e.getMessage());
            }
        }

        return out;
    }

    private boolean isTextLike(String contentType) {
        if (contentType == null || contentType.isBlank()) return false;
        String ct = contentType.toLowerCase();
        return ct.startsWith("text/")
                || ct.contains("json")
                || ct.contains("xml")
                || ct.contains("yaml")
                || ct.contains("javascript")
                || ct.contains("x-www-form-urlencoded");
    }
}
