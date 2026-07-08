package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitAttachmentsHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_work_item_attachment_add
 * Adjunta un archivo a un Work Item. Acepta:
 *  - dataUrl (data: URI inline)
 *  - filePath (ruta local o URI file://)
 *  - dataBase64 (LEGACY)
 * El tool se encarga de leer bytes, inferir content-type cuando es posible y subirlo.
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
    String dataUrl = Objects.toString(args.get("dataUrl"), null); // data: URI inline en el payload JSON
    String filePath = Objects.toString(args.get("filePath"), null); // ruta local o file://
        if (wiObj == null) throw new IllegalArgumentException("'workItemId' es requerido");
        int id;
        try { id = Integer.parseInt(wiObj.toString()); } catch (NumberFormatException e) { throw new IllegalArgumentException("'workItemId' inválido"); }
        if (id <= 0) throw new IllegalArgumentException("'workItemId' debe ser > 0");
        // Validación: admitir dataUrl (data:), filePath (ruta local/file://) o dataBase64 (legacy)
        boolean hasDataUrl = dataUrl != null && !dataUrl.isBlank();
        boolean hasFilePath = filePath != null && !filePath.isBlank();
        boolean hasB64 = dataB64 != null && !dataB64.isBlank();
        if (!(hasDataUrl || hasFilePath || hasB64)) {
            throw new IllegalArgumentException("Se requiere 'dataUrl' (MCP remoto/archivo pequeño), multipart vía prepare_upload, 'filePath' (solo MCP local) o 'dataBase64' legacy.");
        }
        if (hasB64 && (fileName == null || fileName.isBlank())) {
            throw new IllegalArgumentException("'fileName' es requerido cuando se usa 'dataBase64'.");
        }
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("project", Map.of("type","string","description","Nombre o ID del proyecto. Opcional: si no viene, el MCP intenta inferirlo desde workItemId."));
        props.put("workItemId", Map.of("type","integer","description","ID numérico del work item (requerido)"));
    props.put("fileName", Map.of("type","string","description","Nombre del archivo. Opcional con dataUrl/multipart; requerido si no puede inferirse."));
    props.put("dataUrl", Map.of("type","string","description","MCP remoto/archivos pequeños: Data URI inline (data:image/png;base64,AAAA...). Para archivos reales/grandes use prepare_upload + multipart."));
    props.put("filePath", Map.of("type","string","description","Solo MCP local: ruta accesible por el servidor MCP. No usar en MCP remoto."));
    props.put("dataBase64", Map.of("type","string","description","[DEPRECATED] Contenido base64 del archivo (solo compatibilidad)"));
        props.put("contentType", Map.of("type","string","description","MIME type. Default application/octet-stream"));
        props.put("comment", Map.of("type","string","description","Comentario opcional de la relación"));
        props.put("apiVersion", Map.of("type","string","description","Override api-version para PATCH (default 7.2-preview)","default","7.2-preview"));
        props.put("raw", Map.of("type","boolean","description","Si true devuelve JSON crudo combinado"));
        return Map.of(
            "type","object",
            "properties", props,
            "required", List.of("workItemId")
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        String project = Objects.toString(arguments.get("project"), "").trim();
        int workItemId = Integer.parseInt(arguments.get("workItemId").toString());
        String fileName = Objects.toString(arguments.get("fileName"), null);
        String dataB64 = Objects.toString(arguments.get("dataBase64"), null); // legacy
    String dataUrl = Objects.toString(arguments.get("dataUrl"), null); // recomendado si cliente y servidor están separados
    String filePath = Objects.toString(arguments.get("filePath"), null); // ruta local o file://
    String comment = Objects.toString(arguments.get("comment"), null);
        String apiVersion = Objects.toString(arguments.getOrDefault("apiVersion", "7.2-preview"));
        boolean raw = Boolean.TRUE.equals(arguments.get("raw"));

        byte[] data;
        String ctStr;
        // Prioridad: dataUrl > filePath > dataBase64
        if (dataUrl != null && !dataUrl.isBlank()) {
            try {
                ReadResult rr = readFromSource(dataUrl.trim());
                data = rr.bytes;
                if ((fileName == null || fileName.isBlank()) && rr.suggestedFileName != null) fileName = rr.suggestedFileName;
                String ctInput = Objects.toString(arguments.get("contentType"), null);
                ctStr = attachmentsHelper.sanitizeContentType(ctInput != null ? ctInput : rr.contentType);
            } catch (Exception ex) {
                return error("No se pudo procesar 'dataUrl': " + ex.getMessage());
            }
        } else if (filePath != null && !filePath.isBlank()) {
            try {
                ReadResult rr = readFromSource(filePath.trim());
                data = rr.bytes;
                if ((fileName == null || fileName.isBlank()) && rr.suggestedFileName != null) fileName = rr.suggestedFileName;
                String ctInput = Objects.toString(arguments.get("contentType"), null);
                ctStr = attachmentsHelper.sanitizeContentType(ctInput != null ? ctInput : rr.contentType);
            } catch (Exception ex) {
                return error("No se pudo leer 'filePath': " + ex.getMessage());
            }
        } else if (dataB64 != null && !dataB64.isBlank()) {
            data = attachmentsHelper.decodeBase64(dataB64);
            ctStr = attachmentsHelper.sanitizeContentType(
                arguments.get("contentType") == null ? null : arguments.get("contentType").toString()
            );
        } else {
            return error("Debe proporcionar 'dataUrl' o 'dataBase64'.");
        }

        if (fileName == null || fileName.isBlank()) {
            return error("'fileName' no pudo inferirse; proporciónelo explícitamente.");
        }

        Map<String,Object> attached = attachmentsHelper.attachBytesToWorkItem(project, workItemId, fileName, data, ctStr, comment, apiVersion);
        if (raw) return Map.of("isError", Boolean.TRUE.equals(attached.get("isError")), "raw", attached);
        if (Boolean.TRUE.equals(attached.get("isError"))) return error(Objects.toString(attached.get("message"), "Error adjuntando archivo"));

        StringBuilder sb = new StringBuilder();
        sb.append("=== Attachment Adjuntado ===\n\n");
        sb.append("Project: ").append(attached.get("project")).append("\n");
        sb.append("WorkItem: ").append(workItemId).append("\n");
        sb.append("Archivo: ").append(attached.get("fileName")).append("\n");
        sb.append("URL: ").append(attached.get("url")).append("\n");
        if (comment != null && !comment.isBlank()) sb.append("Comentario: ").append(comment).append("\n");

        // Estructura programática simple para agentes
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("workItemId", workItemId);
        result.put("project", attached.get("project"));
        result.put("fileName", attached.get("fileName"));
        result.put("url", attached.get("url"));
        if (comment != null && !comment.isBlank()) result.put("comment", comment);
        // Intentar inferir attachmentId a partir de la URL
        String attachmentId = Objects.toString(attached.get("attachmentId"), null);
        if (attachmentId != null) result.put("attachmentId", attachmentId);

        Map<String,Object> out = new LinkedHashMap<>();
        out.put("isError", false);
        out.put("result", result);
        out.put("content", java.util.List.of(java.util.Map.of("type","text","text", sb.toString())));
        return out;
    }

    // --- Utilidades locales ---
    private static class ReadResult {
        final byte[] bytes;
        final String contentType; // puede ser null si no se pudo inferir
        final String suggestedFileName; // puede ser null
        ReadResult(byte[] b, String ct, String name) { this.bytes = b; this.contentType = ct; this.suggestedFileName = name; }
    }

    private ReadResult readFromSource(String uriOrPath) throws Exception {
        // Soporta:
        // - data:[<mediatype>][;base64],<data>
        // - file:///absolute/path o rutas locales (absolutas o relativas)
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
        // file:// URI o ruta local
        java.nio.file.Path path;
        try {
            if (lower.startsWith("file:")) {
                java.net.URI uri = java.net.URI.create(uriOrPath);
                path = java.nio.file.Paths.get(uri);
            } else {
                path = java.nio.file.Paths.get(uriOrPath);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Ruta inválida: " + e.getMessage());
        }
        if (!java.nio.file.Files.exists(path)) {
            throw new IllegalArgumentException("Archivo no existe: " + path);
        }
        byte[] bytes = java.nio.file.Files.readAllBytes(path);
        String ct = null;
        try { ct = java.nio.file.Files.probeContentType(path); } catch (Exception ignore) {}
        String suggested = path.getFileName() != null ? path.getFileName().toString() : null;
        return new ReadResult(bytes, ct, suggested);
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
