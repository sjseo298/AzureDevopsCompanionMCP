package com.mcp.server.transport.http;

import com.mcp.server.services.helpers.WitAttachmentsHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP endpoint para subir un archivo vía multipart y adjuntarlo a un Work Item en Azure DevOps.
 * Evita depender de base64 o filesystem compartido entre cliente y servidor MCP.
 */
@RestController
@RequestMapping("/mcp/uploads")
@ConditionalOnProperty(name = "mcp.http", havingValue = "true")
public class AttachmentsUploadController {

    private static final Logger log = LoggerFactory.getLogger(AttachmentsUploadController.class);

    @Autowired
    private WitAttachmentsHelper attachmentsHelper;

    /**
     * Subir archivo y adjuntar al Work Item.
     *
     * Ejemplo: POST /mcp/uploads/wit/workitems/123/attachment?project=MyProject
     * Content-Type: multipart/form-data; file=<archivo>, comment=<opcional>, contentType=<opcional>, fileName=<opcional>
     */
    @PostMapping(value = "/wit/workitems/{workItemId}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAndAttach(
            @PathVariable("workItemId") int workItemId,
            @RequestParam("project") String project,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "fileName", required = false) String fileName,
            @RequestParam(value = "comment", required = false) String comment,
            @RequestParam(value = "contentType", required = false) String contentType,
            @RequestParam(value = "apiVersion", required = false, defaultValue = "7.2-preview") String apiVersion,
            @RequestParam(value = "raw", required = false, defaultValue = "false") boolean raw
    ) {
        try {
            if (project == null || project.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "'project' es requerido"));
            }
            if (workItemId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "'workItemId' debe ser > 0"));
            }
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "'file' es requerido"));
            }

            String effectiveFileName = (fileName != null && !fileName.isBlank()) ? fileName : file.getOriginalFilename();
            if (effectiveFileName == null || effectiveFileName.isBlank()) {
                effectiveFileName = "attachment.bin";
            }
            String ct = attachmentsHelper.sanitizeContentType(
                    (contentType != null && !contentType.isBlank()) ? contentType : Objects.toString(file.getContentType(), null)
            );

            // 1) Crear attachment con binario
            Map<String, Object> createResp = attachmentsHelper.createAttachment(effectiveFileName, file.getBytes(), ct);
            String createErrFmt = tryFormatRemoteError(createResp);
            if (createErrFmt != null && !raw) {
                return ResponseEntity.status(502).body(Map.of("error", "Error creando attachment", "details", createErrFmt));
            }
            String attachmentUrl = Objects.toString(createResp.get("url"), null);
            if (attachmentUrl == null) {
                if (raw) return ResponseEntity.status(502).body(Map.of("isError", true, "raw", Map.of("attachment", createResp, "message", "No se obtuvo URL del attachment")));
                return ResponseEntity.status(502).body(Map.of("error", "No se obtuvo URL del attachment"));
            }

            // 2) Vincular al Work Item
            Map<String, Object> linkResp = attachmentsHelper.linkAttachmentToWorkItem(project, workItemId, attachmentUrl, comment, apiVersion);
            String linkErrFmt = tryFormatRemoteError(linkResp);

            if (raw) {
                Map<String, Object> combined = new LinkedHashMap<>();
                combined.put("attachment", createResp);
                combined.put("workItemPatch", linkResp);
                if (createErrFmt != null) combined.put("createErrorFormatted", createErrFmt);
                if (linkErrFmt != null) {
                    combined.put("linkErrorFormatted", linkErrFmt);
                    // Rollback: eliminar attachment si la asociación falla
                    Map<String, Object> rollbackInfo = new LinkedHashMap<>();
                    String attachmentId = Objects.toString(createResp.get("id"), null);
                    if (attachmentId == null) {
                        int idx = attachmentUrl.lastIndexOf('/');
                        if (idx != -1) {
                            String tail = attachmentUrl.substring(idx + 1);
                            int q = tail.indexOf('?');
                            if (q != -1) tail = tail.substring(0, q);
                            if (!tail.isBlank()) attachmentId = tail;
                        }
                    }
                    if (attachmentId != null) {
                        Map<String, Object> delResp = attachmentsHelper.deleteAttachment(project, attachmentId);
                        rollbackInfo.put("deleteResponse", delResp);
                        String delErrFmt = tryFormatRemoteError(delResp);
                        if (delErrFmt != null) rollbackInfo.put("deleteErrorFormatted", delErrFmt);
                    } else {
                        rollbackInfo.put("message", "No se pudo determinar ID del attachment para rollback");
                    }
                    combined.put("rollback", rollbackInfo);
                    combined.put("operationStatus", "FAILED_AND_ROLLED_BACK");
                    return ResponseEntity.status(502).body(Map.of("isError", true, "raw", combined));
                }
                combined.put("operationStatus", "SUCCESS");
                return ResponseEntity.ok(Map.of("isError", false, "raw", combined));
            }

            if (linkErrFmt != null) {
                // Rollback si falla el PATCH
                String attachmentId = Objects.toString(createResp.get("id"), null);
                if (attachmentId == null) {
                    int idx = attachmentUrl.lastIndexOf('/');
                    if (idx != -1) {
                        String tail = attachmentUrl.substring(idx + 1);
                        int q = tail.indexOf('?');
                        if (q != -1) tail = tail.substring(0, q);
                        if (!tail.isBlank()) attachmentId = tail;
                    }
                }
                if (attachmentId != null) {
                    Map<String, Object> delResp = attachmentsHelper.deleteAttachment(project, attachmentId);
                    String delErrFmt = tryFormatRemoteError(delResp);
                    if (delErrFmt != null) {
                        log.warn("Rollback delete error: {}", delErrFmt);
                    }
                }
                return ResponseEntity.status(502).body(Map.of(
                        "error", "Error asociando attachment al work item",
                        "details", linkErrFmt
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Attachment adjuntado",
                    "workItemId", workItemId,
                    "fileName", effectiveFileName,
                    "url", attachmentUrl
            ));

        } catch (Exception e) {
            log.error("Error en uploadAndAttach", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // Reutiliza el formateador consistente de errores remotos
    private String tryFormatRemoteError(Map<String, Object> resp) {
        if (resp == null) return "Respuesta nula";
        Object isHttpError = resp.get("isHttpError");
        if (isHttpError instanceof Boolean && (Boolean) isHttpError) {
            StringBuilder sb = new StringBuilder();
            Object status = resp.get("httpStatus");
            Object reason = resp.get("httpReason");
            if (status != null) sb.append("HTTP Status: ").append(status).append('\n');
            if (reason != null) sb.append("Reason: ").append(reason).append('\n');
            Object body = resp.get("body");
            if (body != null) sb.append("Body: ").append(body);
            return sb.toString();
        }
        return null;
    }
}
