package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitAttachmentsHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_work_item_attachment_add
 * Adjunta un archivo a un Work Item. Ahora acepta una URI de archivo (p. ej. file:///path/a.docx o ruta local)
 * y el tool se encarga de leerlo, (opcionalmente) inferir content-type y subirlo.
 * También mantiene compatibilidad con dataBase64 (deprecated).
 * Endpoint 1: POST /_apis/wit/attachments?fileName={name}&api-version=7.2-preview
 * Endpoint 2: PATCH /{project}/_apis/wit/workitems/{id} (JSON Patch add relation)
 */
@Component
public class WorkItemAttachmentAddTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_work_item_attachment_add";
    private static final String DESC = "Adjunta (sube y vincula) un archivo a un Work Item (AttachedFile).";

    private final WitAttachmentsHelper attachmentsHelper;

    @Autowired
    public WorkItemAttachmentAddTool(AzureDevOpsClientService service, WitAttachmentsHelper attachmentsHelper) {
        super(service);
        this.attachmentsHelper = attachmentsHelper;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        String project = Objects.toString(args.get("project"), "").trim();
        Object wiObj = args.get("workItemId");
        String fileName = Objects.toString(args.get("fileName"), null);
        String dataB64 = Objects.toString(args.get("dataBase64"), null); // legacy
        String fileUri = Objects.toString(args.get("fileUri"), null);
        if (project.isEmpty()) throw new IllegalArgumentException("'project' es requerido");
        if (wiObj == null) throw new IllegalArgumentException("'workItemId' es requerido");
        int id;
        try { id = Integer.parseInt(wiObj.toString()); } catch (NumberFormatException e) { throw new IllegalArgumentException("'workItemId' inválido"); }
        if (id <= 0) throw new IllegalArgumentException("'workItemId' debe ser > 0");
        // Validación: preferir fileUri. Si no viene, permitir dataBase64 (deprecated)
        if ((fileUri == null || fileUri.isBlank()) && (dataB64 == null || dataB64.isBlank())) {
            throw new IllegalArgumentException("Se requiere 'fileUri' o, en modo legacy, 'dataBase64'.");
        }
        if (fileUri != null && fileUri.isBlank()) {
            throw new IllegalArgumentException("'fileUri' no puede ser vacío si se especifica.");
        }
        if ((fileName == null || fileName.isBlank()) && (fileUri == null || fileUri.isBlank())) {
            throw new IllegalArgumentException("'fileName' es requerido cuando no se puede inferir desde 'fileUri'.");
        }
        // Si el llamado usa el modo legacy, validar base64 como antes
        if (dataB64 != null && !dataB64.isBlank()) {
            attachmentsHelper.validateCreate(fileName, dataB64);
        }
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("project", Map.of("type","string","description","Nombre o ID del proyecto (requerido)"));
        props.put("workItemId", Map.of("type","integer","description","ID numérico del work item (requerido)"));
        props.put("fileName", Map.of("type","string","description","Nombre del archivo a adjuntar (si no se provee, se infiere de fileUri si es posible)"));
        props.put("fileUri", Map.of("type","string","description","URI o ruta del archivo a adjuntar (p. ej. file:///tmp/x.png o /tmp/x.png). Recomendado."));
        props.put("dataBase64", Map.of("type","string","description","[DEPRECATED] Contenido base64 del archivo (solo para compatibilidad)"));
        props.put("contentType", Map.of("type","string","description","MIME type. Default application/octet-stream"));
        props.put("comment", Map.of("type","string","description","Comentario opcional de la relación"));
        props.put("apiVersion", Map.of("type","string","description","Override api-version para PATCH (default 7.2-preview)","default","7.2-preview"));
        props.put("raw", Map.of("type","boolean","description","Si true devuelve JSON crudo combinado"));
        return Map.of(
            "type","object",
            "properties", props,
            // Requerimos project/workItemId. fileUri es el camino recomendado; fileName puede inferirse.
            "required", List.of("project","workItemId")
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        String project = Objects.toString(arguments.get("project"), "").trim();
        int workItemId = Integer.parseInt(arguments.get("workItemId").toString());
        String fileName = Objects.toString(arguments.get("fileName"), null);
        String dataB64 = Objects.toString(arguments.get("dataBase64"), null); // legacy
        String fileUri = Objects.toString(arguments.get("fileUri"), null);
        String comment = Objects.toString(arguments.get("comment"), null);
        String apiVersion = Objects.toString(arguments.getOrDefault("apiVersion", "7.2-preview"));
        boolean raw = Boolean.TRUE.equals(arguments.get("raw"));

        byte[] data;
        String ctStr;
        // Fuente preferida: fileUri -> leer bytes. Si no, dataBase64 (legacy)
        if (fileUri != null && !fileUri.isBlank()) {
            ReadResult rr;
            try {
                rr = readFromUri(fileUri.trim());
            } catch (Exception ex) {
                return error("No se pudo leer 'fileUri': " + ex.getMessage());
            }
            data = rr.bytes;
            // Inferir fileName si falta
            if ((fileName == null || fileName.isBlank()) && rr.suggestedFileName != null) {
                fileName = rr.suggestedFileName;
            }
            String ctInput = Objects.toString(arguments.get("contentType"), null);
            ctStr = attachmentsHelper.sanitizeContentType(ctInput != null ? ctInput : rr.contentType);
        } else if (dataB64 != null && !dataB64.isBlank()) {
            data = attachmentsHelper.decodeBase64(dataB64);
            ctStr = attachmentsHelper.sanitizeContentType(
                arguments.get("contentType") == null ? null : arguments.get("contentType").toString()
            );
        } else {
            return error("Debe proporcionar 'fileUri' o 'dataBase64'.");
        }

        if (fileName == null || fileName.isBlank()) {
            return error("'fileName' no pudo inferirse; proporciónelo explícitamente.");
        }

        Map<String,Object> createResp = attachmentsHelper.createAttachment(fileName, data, ctStr);
        String createErrFmt = tryFormatRemoteError(createResp);
        if (createErrFmt != null && !raw) {
            return error("Error creando attachment: \n" + createErrFmt);
        }
        String attachmentUrl = Objects.toString(createResp.get("url"), null);
        if (attachmentUrl == null) {
            if (raw) return Map.of("isError", true, "raw", Map.of("attachment", createResp, "message", "No se obtuvo URL del attachment"));
            return error("No se obtuvo URL del attachment");
        }

        Map<String,Object> linkResp = attachmentsHelper.linkAttachmentToWorkItem(project, workItemId, attachmentUrl, comment, apiVersion);
        String linkErrFmt = tryFormatRemoteError(linkResp);

        if (raw) {
            Map<String,Object> combined = new LinkedHashMap<>();
            combined.put("attachment", createResp);
            combined.put("workItemPatch", linkResp);
            if (createErrFmt != null) combined.put("createErrorFormatted", createErrFmt);
            if (linkErrFmt != null) {
                combined.put("linkErrorFormatted", linkErrFmt);
                // Intentar rollback eliminando el attachment porque la asociación falló
                Map<String,Object> rollbackInfo = new LinkedHashMap<>();
                String attachmentId = Objects.toString(createResp.get("id"), null);
                if (attachmentId == null && attachmentUrl != null) {
                    int idx = attachmentUrl.lastIndexOf('/');
                    if (idx != -1) {
                        String tail = attachmentUrl.substring(idx+1);
                        int q = tail.indexOf('?');
                        if (q != -1) tail = tail.substring(0,q);
                        if (!tail.isBlank()) attachmentId = tail;
                    }
                }
                if (attachmentId != null) {
                    Map<String,Object> delResp = attachmentsHelper.deleteAttachment(project, attachmentId);
                    rollbackInfo.put("deleteResponse", delResp);
                    String delErrFmt = tryFormatRemoteError(delResp);
                    if (delErrFmt != null) rollbackInfo.put("deleteErrorFormatted", delErrFmt);
                } else {
                    rollbackInfo.put("message", "No se pudo determinar ID del attachment para rollback");
                }
                combined.put("rollback", rollbackInfo);
                combined.put("operationStatus", "FAILED_AND_ROLLED_BACK");
                return Map.of("isError", true, "raw", combined);
            }
            return Map.of("isError", false, "raw", combined);
        }

        if (linkErrFmt != null) {
            // Intentar rollback (eliminar adjunto creado) para consistencia
            String attachmentId = Objects.toString(createResp.get("id"), null);
            if (attachmentId == null && attachmentUrl != null) {
                int idx = attachmentUrl.lastIndexOf('/');
                if (idx != -1) {
                    String tail = attachmentUrl.substring(idx+1);
                    int q = tail.indexOf('?');
                    if (q != -1) tail = tail.substring(0,q);
                    if (!tail.isBlank()) attachmentId = tail;
                }
            }
            StringBuilder err = new StringBuilder();
            err.append("Error asociando attachment al work item. Operación revertida.\n");
            err.append(linkErrFmt).append("\n");
            if (attachmentId != null) {
                Map<String,Object> delResp = attachmentsHelper.deleteAttachment(project, attachmentId);
                String delErrFmt = tryFormatRemoteError(delResp);
                if (delErrFmt != null) {
                    err.append("Error eliminando attachment durante rollback:\n").append(delErrFmt).append("\n");
                } else {
                    err.append("Attachment temporal eliminado (rollback OK).\n");
                }
            } else {
                err.append("No se pudo determinar ID del attachment para rollback.\n");
            }
            return error(err.toString());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Attachment Adjuntado ===\n\n");
        sb.append("WorkItem: ").append(workItemId).append("\n");
        sb.append("Archivo: ").append(fileName).append("\n");
        sb.append("URL: ").append(attachmentUrl).append("\n");
        if (comment != null && !comment.isBlank()) sb.append("Comentario: ").append(comment).append("\n");
        return success(sb.toString());
    }

    // --- Utilidades locales ---
    private static class ReadResult {
        final byte[] bytes;
        final String contentType; // puede ser null si no se pudo inferir
        final String suggestedFileName; // puede ser null
        ReadResult(byte[] b, String ct, String name) { this.bytes = b; this.contentType = ct; this.suggestedFileName = name; }
    }

    private ReadResult readFromUri(String uriOrPath) throws Exception {
        // Soporta:
        // - data:[<mediatype>][;base64],<data>
        // - file:///absoluto o ruta local absoluta/relativa
        String lower = uriOrPath.toLowerCase(Locale.ROOT);
        if (lower.startsWith("data:")) {
            // data URI
            int comma = uriOrPath.indexOf(',');
            if (comma < 0) throw new IllegalArgumentException("Data URI inválida (sin ',')");
            String meta = uriOrPath.substring(5, comma); // sin 'data:'
            String payload = uriOrPath.substring(comma + 1);
            boolean isB64 = meta.contains(";base64");
            String ct = null;
            if (!meta.isBlank()) {
                String mt = meta.replace(";base64", "");
                ct = mt.isBlank() ? null : mt;
            }
            byte[] bytes = isB64 ? java.util.Base64.getDecoder().decode(payload) : payload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            String suggested = null;
            if (ct != null) {
                String ext = guessExtensionFromContentType(ct);
                if (ext != null) suggested = "attachment" + ext;
            }
            return new ReadResult(bytes, ct, suggested);
        }

        java.nio.file.Path path;
        try {
            if (lower.startsWith("file:")) {
                java.net.URI u = new java.net.URI(uriOrPath);
                path = java.nio.file.Paths.get(u);
            } else {
                path = java.nio.file.Paths.get(uriOrPath);
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("URI de archivo no válida: " + ex.getMessage(), ex);
        }
        if (!java.nio.file.Files.exists(path)) {
            throw new java.io.FileNotFoundException("No existe el archivo: " + path);
        }
        byte[] bytes = java.nio.file.Files.readAllBytes(path);
        String name = path.getFileName() != null ? path.getFileName().toString() : null;
        String ct;
        try {
            ct = java.nio.file.Files.probeContentType(path);
        } catch (Exception ignore) {
            ct = null;
        }
        return new ReadResult(bytes, ct, name);
    }

    private String guessExtensionFromContentType(String ct) {
        if (ct == null) return null;
        // Mapeo mínimo; si se requiere algo más robusto, usar biblioteca mime-types.
        switch (ct) {
            case "image/png": return ".png";
            case "image/jpeg": return ".jpg";
            case "image/gif": return ".gif";
            case "application/pdf": return ".pdf";
            case "text/plain": return ".txt";
            default: return null;
        }
    }
}
