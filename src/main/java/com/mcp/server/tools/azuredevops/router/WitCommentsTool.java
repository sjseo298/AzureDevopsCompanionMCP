package com.mcp.server.tools.azuredevops.router;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import com.mcp.server.tools.azuredevops.wit.CommentReactionsEngagedUsersListTool;
import com.mcp.server.tools.azuredevops.wit.CommentsAddTool;
import com.mcp.server.tools.azuredevops.wit.CommentsDeleteTool;
import com.mcp.server.tools.azuredevops.wit.CommentsListTool;
import com.mcp.server.tools.azuredevops.wit.CommentsReactionsAddTool;
import com.mcp.server.tools.azuredevops.wit.CommentsReactionsDeleteTool;
import com.mcp.server.tools.azuredevops.wit.CommentsReactionsListTool;
import com.mcp.server.tools.azuredevops.wit.CommentsUpdateTool;
import com.mcp.server.tools.azuredevops.wit.CommentsVersionsGetTool;
import com.mcp.server.tools.azuredevops.wit.CommentsVersionsListTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class WitCommentsTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_comments";
    private static final String DESC = "Operaciones WIT Comments. operation: list|add|update|delete|versions_list|versions_get|reactions_list|reactions_add|reactions_delete|reactions_engaged_users.";

    private final CommentsListTool listTool;
    private final CommentsAddTool addTool;
    private final CommentsUpdateTool updateTool;
    private final CommentsDeleteTool deleteTool;
    private final CommentsVersionsListTool versionsListTool;
    private final CommentsVersionsGetTool versionsGetTool;
    private final CommentsReactionsListTool reactionsListTool;
    private final CommentsReactionsAddTool reactionsAddTool;
    private final CommentsReactionsDeleteTool reactionsDeleteTool;
    private final CommentReactionsEngagedUsersListTool engagedUsersTool;

    @Autowired
    public WitCommentsTool(
            AzureDevOpsClientService svc,
            CommentsListTool listTool,
            CommentsAddTool addTool,
            CommentsUpdateTool updateTool,
            CommentsDeleteTool deleteTool,
            CommentsVersionsListTool versionsListTool,
            CommentsVersionsGetTool versionsGetTool,
            CommentsReactionsListTool reactionsListTool,
            CommentsReactionsAddTool reactionsAddTool,
            CommentsReactionsDeleteTool reactionsDeleteTool,
            CommentReactionsEngagedUsersListTool engagedUsersTool
    ) {
        super(svc);
        this.listTool = listTool;
        this.addTool = addTool;
        this.updateTool = updateTool;
        this.deleteTool = deleteTool;
        this.versionsListTool = versionsListTool;
        this.versionsGetTool = versionsGetTool;
        this.reactionsListTool = reactionsListTool;
        this.reactionsAddTool = reactionsAddTool;
        this.reactionsDeleteTool = reactionsDeleteTool;
        this.engagedUsersTool = engagedUsersTool;
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
                "enum", List.of(
                        "list", "add", "update", "delete",
                        "versions_list", "versions_get",
                        "reactions_list", "reactions_add", "reactions_delete",
                        "reactions_engaged_users"
                ),
                "description", "Operación a ejecutar"
        ));

        // parámetros frecuentes
        props.put("workItemId", Map.of("type", "integer", "description", "ID del work item"));
        props.put("commentId", Map.of("type", "integer", "description", "ID del comentario"));
        props.put("text", Map.of("type", "string", "description", "Texto/markdown"));
        props.put("reactionType", Map.of("type", "string", "description", "Tipo de reacción"));
        props.put("top", Map.of("type", "integer", "description", "Límite"));
        props.put("skip", Map.of("type", "integer", "description", "Offset"));
        props.put("raw", Map.of("type", "boolean", "description", "Devuelve JSON crudo"));
        props.put("apiVersion", Map.of("type", "string", "description", "Override apiVersion"));
        props.put("api-version", Map.of("type", "string", "description", "Alias script apiVersion"));

        base.put("required", List.of("project", "operation"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        String op = arguments.get("operation") == null ? "" : arguments.get("operation").toString().trim();
        if (op.isEmpty()) return error("'operation' es requerido");

        return switch (op) {
            case "list" -> delegate(listTool, arguments);
            case "add" -> delegate(addTool, arguments);
            case "update" -> delegate(updateTool, arguments);
            case "delete" -> delegate(deleteTool, arguments);
            case "versions_list" -> delegate(versionsListTool, arguments);
            case "versions_get" -> delegate(versionsGetTool, arguments);
            case "reactions_list" -> delegate(reactionsListTool, arguments);
            case "reactions_add" -> delegate(reactionsAddTool, arguments);
            case "reactions_delete" -> delegate(reactionsDeleteTool, arguments);
            case "reactions_engaged_users" -> delegate(engagedUsersTool, arguments);
            default -> error("Operación no soportada: " + op);
        };
    }

    private Map<String, Object> delegate(AbstractAzureDevOpsTool tool, Map<String, Object> arguments) {
        Map<String, Object> copy = new LinkedHashMap<>(arguments);
        copy.remove("operation");
        return tool.execute(copy);
    }
}
