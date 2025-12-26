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
    private static final String DESC = "Operaciones WIT Attachments. operation: get|delete|add_to_work_item.";

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
    public Map<String, Object> getInputSchema() {
        Map<String, Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) base.get("properties");
        props.put("operation", Map.of(
                "type", "string",
                "enum", List.of("get", "delete", "add_to_work_item"),
                "description", "Operación a ejecutar"
        ));
        props.put("attachmentId", Map.of("type", "string", "description", "ID del attachment (get/delete)"));
        props.put("workItemId", Map.of("type", "integer", "description", "Work item ID (add_to_work_item)"));
        props.put("fileName", Map.of("type", "string", "description", "Nombre del archivo"));
        props.put("dataUrl", Map.of("type", "string", "description", "Data URI inline"));
        props.put("filePath", Map.of("type", "string", "description", "Ruta local o file://"));
        props.put("dataBase64", Map.of("type", "string", "description", "[legacy] base64"));
        props.put("contentType", Map.of("type", "string", "description", "MIME"));
        props.put("comment", Map.of("type", "string", "description", "Comentario"));
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
            case "add_to_work_item" -> delegate(addToWorkItemTool, arguments);
            default -> error("Operación no soportada: " + op);
        };
    }

    private Map<String, Object> delegate(AbstractAzureDevOpsTool tool, Map<String, Object> arguments) {
        Map<String, Object> copy = new LinkedHashMap<>(arguments);
        copy.remove("operation");
        return tool.execute(copy);
    }
}
