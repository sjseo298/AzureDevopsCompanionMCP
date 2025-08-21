package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitAttachmentsHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_work_item_attachment_add
 * Sube un archivo (base64) y lo asocia inmediatamente a un Work Item como AttachedFile.
 * Reemplaza los antiguos tools separados (create/get/delete) simplificando el flujo.
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
        String dataB64 = Objects.toString(args.get("dataBase64"), null);
        if (project.isEmpty()) throw new IllegalArgumentException("'project' es requerido");
        if (wiObj == null) throw new IllegalArgumentException("'workItemId' es requerido");
        int id;
        try { id = Integer.parseInt(wiObj.toString()); } catch (NumberFormatException e) { throw new IllegalArgumentException("'workItemId' inválido"); }
        if (id <= 0) throw new IllegalArgumentException("'workItemId' debe ser > 0");
        attachmentsHelper.validateCreate(fileName, dataB64);
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("project", Map.of("type","string","description","Nombre o ID del proyecto (requerido)"));
        props.put("workItemId", Map.of("type","integer","description","ID numérico del work item (requerido)"));
        props.put("fileName", Map.of("type","string","description","Nombre del archivo a adjuntar"));
        props.put("dataBase64", Map.of("type","string","description","Contenido base64 del archivo"));
        props.put("contentType", Map.of("type","string","description","MIME type. Default application/octet-stream"));
        props.put("comment", Map.of("type","string","description","Comentario opcional de la relación"));
        props.put("apiVersion", Map.of("type","string","description","Override api-version para PATCH (default 7.2-preview)","default","7.2-preview"));
        props.put("raw", Map.of("type","boolean","description","Si true devuelve JSON crudo combinado"));
        return Map.of(
            "type","object",
            "properties", props,
            "required", List.of("project","workItemId","fileName","dataBase64")
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        String project = Objects.toString(arguments.get("project"), "").trim();
        int workItemId = Integer.parseInt(arguments.get("workItemId").toString());
        String fileName = arguments.get("fileName").toString();
        String dataB64 = arguments.get("dataBase64").toString();
        String comment = Objects.toString(arguments.get("comment"), null);
        String apiVersion = Objects.toString(arguments.getOrDefault("apiVersion", "7.2-preview"));
        boolean raw = Boolean.TRUE.equals(arguments.get("raw"));

        byte[] data = attachmentsHelper.decodeBase64(dataB64);
        String ctStr = attachmentsHelper.sanitizeContentType(
            arguments.get("contentType") == null ? null : arguments.get("contentType").toString()
        );

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
}
