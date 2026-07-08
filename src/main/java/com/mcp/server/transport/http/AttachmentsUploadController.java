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
            @RequestParam(value = "project", required = false) String project,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "fileName", required = false) String fileName,
            @RequestParam(value = "comment", required = false) String comment,
            @RequestParam(value = "contentType", required = false) String contentType,
            @RequestParam(value = "apiVersion", required = false, defaultValue = "7.2-preview") String apiVersion,
            @RequestParam(value = "raw", required = false, defaultValue = "false") boolean raw
    ) {
        try {
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

            Map<String,Object> result = attachmentsHelper.attachBytesToWorkItem(project, workItemId, effectiveFileName, file.getBytes(), ct, comment, apiVersion);
            if (raw) return ResponseEntity.status(Boolean.TRUE.equals(result.get("isError")) ? 502 : 200).body(Map.of("isError", result.get("isError"), "raw", result));
            if (Boolean.TRUE.equals(result.get("isError"))) {
                return ResponseEntity.status(502).body(Map.of(
                        "isError", true,
                        "message", Objects.toString(result.get("message"), "Error adjuntando archivo"),
                        "details", result
                ));
            }
            Map<String,Object> response = new LinkedHashMap<>();
            response.put("isError", false);
            response.put("message", "Archivo adjuntado correctamente");
            response.put("project", result.get("project"));
            response.put("workItemId", result.get("workItemId"));
            response.put("fileName", result.get("fileName"));
            response.put("contentType", result.get("contentType"));
            response.put("bytes", result.get("bytes"));
            response.put("attachmentId", result.get("attachmentId"));
            response.put("url", result.get("url"));
            if (result.get("comment") != null) response.put("comment", result.get("comment"));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error en uploadAndAttach", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

}
