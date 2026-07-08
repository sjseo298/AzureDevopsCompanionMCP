package com.mcp.server.tools.azuredevops.router;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import com.mcp.server.tools.azuredevops.wit.AttachmentsDeleteTool;
import com.mcp.server.tools.azuredevops.wit.AttachmentsGetTool;
import com.mcp.server.tools.azuredevops.wit.WorkItemAttachmentAddTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class WitAttachmentsTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_attachments";
    private static final String DESC = "Operaciones WIT Attachments. operation: get|delete|attach|prepare_upload|add_to_work_item.";

    private final AttachmentsGetTool getTool;
    private final AttachmentsDeleteTool deleteTool;
    private final WorkItemAttachmentAddTool addToWorkItemTool;

    @Autowired
    public WitAttachmentsTool(
            AzureDevOpsClientService svc,
            AttachmentsGetTool getTool,
            AttachmentsDeleteTool deleteTool,
            WorkItemAttachmentAddTool addToWorkItemTool
    ) {
        super(svc);
        this.getTool = getTool;
        this.deleteTool = deleteTool;
        this.addToWorkItemTool = addToWorkItemTool;
    }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    protected boolean isProjectRequired() {
        return false;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) base.get("properties");
        props.put("operation", Map.of(
                "type", "string",
                "enum", List.of("get", "delete", "attach", "prepare_upload", "add_to_work_item"),
                "description", "Operación a ejecutar"
        ));
        props.put("attachmentId", Map.of("type", "string", "description", "ID del attachment (get/delete)"));
        props.put("workItemId", Map.of("type", "integer", "description", "Work item ID (attach/add_to_work_item/prepare_upload)"));
        props.put("fileName", Map.of("type", "string", "description", "Nombre del archivo. Opcional si se usa multipart o dataUrl con MIME inferible."));
        props.put("dataUrl", Map.of("type", "string", "description", "Para MCP remoto y archivos pequeños: data URI inline (data:<mime>;base64,...). Preferir multipart para archivos reales/grandes."));
        props.put("filePath", Map.of("type", "string", "description", "Solo MCP local: ruta accesible por el servidor MCP. No usar en MCP remoto; use prepare_upload/multipart o dataUrl."));
        props.put("dataBase64", Map.of("type", "string", "description", "[legacy] base64; preferir dataUrl o multipart."));
        props.put("contentType", Map.of("type", "string", "description", "MIME"));
        props.put("comment", Map.of("type", "string", "description", "Comentario"));
        props.put("includeBase64", Map.of("type", "boolean", "description", "(get) incluir contenido base64 en la respuesta"));
        props.put("maxBase64Chars", Map.of("type", "integer", "description", "(get) máximo de caracteres base64 a retornar"));
        props.put("includeTextPreview", Map.of("type", "boolean", "description", "(get) incluir preview UTF-8 para tipos de texto"));
        props.put("maxTextChars", Map.of("type", "integer", "description", "(get) máximo de caracteres en textPreview"));
        props.put("outputPath", Map.of("type", "string", "description", "(get) ruta local para guardar el adjunto"));
        props.put("apiVersion", Map.of("type", "string", "description", "Override apiVersion"));
        props.put("raw", Map.of("type", "boolean", "description", "Devuelve JSON crudo"));
        base.put("required", List.of("project", "operation"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        String op = arguments.get("operation") == null ? "" : arguments.get("operation").toString().trim();
        if (op.isEmpty()) return error("'operation' es requerido");
        return switch (op) {
            case "get" -> delegate(getTool, arguments);
            case "delete" -> delegate(deleteTool, arguments);
            case "attach", "add_to_work_item" -> delegate(addToWorkItemTool, arguments);
            case "prepare_upload" -> prepareUpload(arguments);
            default -> error("Operación no soportada: " + op);
        };
    }

    private Map<String, Object> prepareUpload(Map<String, Object> arguments) {
        Object wiObj = arguments.get("workItemId");
        if (wiObj == null || wiObj.toString().isBlank()) return error("'workItemId' es requerido para prepare_upload");
        String project = arguments.get("project") == null ? null : arguments.get("project").toString().trim();
        StringBuilder path = new StringBuilder("/mcp/uploads/wit/workitems/").append(wiObj).append("/attachment");
        if (project != null && !project.isBlank()) path.append("?project=").append(project);
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("isError", false);
        out.put("method", "POST");
        out.put("uploadPath", path.toString());
        out.put("contentType", "multipart/form-data");
        out.put("fileField", "file");
        out.put("optionalFields", List.of("comment", "fileName", "contentType", "apiVersion", "raw"));
        out.put("message", "Envíe el archivo real como multipart/form-data en el campo 'file'. En MCP remoto no use filePath.");
        out.put("content", List.of(Map.of("type", "text", "text", "POST " + path + " con multipart/form-data; campo obligatorio: file. Campos opcionales: comment, fileName, contentType.")));
        return out;
    }

    private Map<String, Object> delegate(AbstractAzureDevOpsTool tool, Map<String, Object> arguments) {
        Map<String, Object> copy = new LinkedHashMap<>(arguments);
        copy.remove("operation");
        return tool.execute(copy);
    }
}
