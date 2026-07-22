package com.mcp.server.tools.azuredevops.router;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitWorkItemGetFieldsHelper;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import com.mcp.server.tools.azuredevops.wit.WorkItemCreateTool;
import com.mcp.server.tools.azuredevops.wit.WorkItemDeleteTool;
import com.mcp.server.tools.azuredevops.wit.WorkItemGetTool;
import com.mcp.server.tools.azuredevops.wit.WorkItemUpdateTool;
import com.mcp.server.tools.azuredevops.wit.WorkItemsBatchTool;
import com.mcp.server.tools.azuredevops.wit.WorkItemsDeleteListTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class WitWorkItemsTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_work_items";
    private static final String DESC = "Operaciones WIT Work Items. operation: get|create|update|delete|batch_get|bulk_delete|get_fields.";

    private final WorkItemGetTool getTool;
    private final WorkItemCreateTool createTool;
    private final WorkItemUpdateTool updateTool;
    private final WorkItemDeleteTool deleteTool;
    private final WorkItemsBatchTool batchTool;
    private final WorkItemsDeleteListTool bulkDeleteTool;
    private final WitWorkItemGetFieldsHelper getFieldsHelper;

    @Autowired
    public WitWorkItemsTool(
            AzureDevOpsClientService svc,
            WorkItemGetTool getTool,
            WorkItemCreateTool createTool,
            WorkItemUpdateTool updateTool,
            WorkItemDeleteTool deleteTool,
            WorkItemsBatchTool batchTool,
            WorkItemsDeleteListTool bulkDeleteTool,
            WitWorkItemGetFieldsHelper getFieldsHelper
    ) {
        super(svc);
        this.getTool = getTool;
        this.createTool = createTool;
        this.updateTool = updateTool;
        this.deleteTool = deleteTool;
        this.batchTool = batchTool;
        this.bulkDeleteTool = bulkDeleteTool;
        this.getFieldsHelper = getFieldsHelper;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESC;
    }

    @Override
    protected boolean isProjectRequired() {
        // Algunas operaciones (get por id) aceptan project opcional
        return false;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) base.get("properties");

        props.put("operation", Map.of(
                "type", "string",
                "enum", List.of("get", "create", "update", "delete", "batch_get", "bulk_delete", "get_fields"),
                "description", "Operación a ejecutar"
        ));

        // Unión (subset) de parámetros de los tools delegados
        props.put("id", Map.of("type", "integer", "description", "Work item ID (get/update/delete)"));
        props.put("ids", Map.of("type", "string", "description", "IDs separados por coma (batch_get/bulk_delete)"));
        props.put("fields", Map.of("type", "string", "description", "Campos referenceName separados por coma o punto y coma. Campos HTML conocidos se normalizan y las tablas reciben estilos compatibles automáticamente."));
        props.put("expand", Map.of("type", "string", "description", "None|Relations|Links|All (get)"));
        props.put("asOf", Map.of("type", "string", "description", "Fecha/hora ISO (get/batch_get)"));

        props.put("type", Map.of("type", "string", "description", "Tipo de work item (create)"));
        props.put("title", Map.of("type", "string", "description", "Título (create/update shortcut)"));
        props.put("description", Map.of("type", "string", "description", "Descripción HTML enriquecida segura (create/update shortcut). NO markdown, NO texto plano. Use etiquetas HTML como <p>, <b>, <i>, <ul>, <li>, <table>; el MCP estiliza tablas automáticamente."));
        props.put("acceptanceCriteria", Map.of("type", "string", "description", "Criterios de aceptación HTML enriquecidos. NO markdown, NO texto plano. Alias de Microsoft.VSTS.Common.AcceptanceCriteria; tablas estilizadas automáticamente."));
        props.put("criteria", Map.of("type", "string", "description", "Alias de acceptanceCriteria"));
        props.put("state", Map.of("type", "string", "description", "Estado (create/update shortcut)"));
        props.put("area", Map.of("type", "string", "description", "AreaPath (create/update shortcut)"));
        props.put("iteration", Map.of("type", "string", "description", "IterationPath (create/update shortcut)"));
        props.put("parentId", Map.of("type", "integer", "description", "Work item padre (create/update)"));
        props.put("parent", Map.of("type", "integer", "description", "Alias de parentId"));
        props.put("repositoryId", Map.of("type", "string", "description", "ID del repositorio (requerido para ArtifactLink:pr en relations)"));
        props.put("relations", Map.of("type", "string", "description", "Relaciones extra: ArtifactLink:pr:repoId/prId para PRs, tipo:id[:comentario] para WIs"));
        props.put("add", Map.of("type", "string", "description", "Campos extra k=v para create/update (alias de fields en create, op add en update). Soporta Custom.<referenceName>=<valor>."));
        props.put("tags", Map.of("type", "string", "description", "System.Tags para create/update, separadas por punto y coma. Ej: TAG1; TAG2"));
        props.put("replace", Map.of("type", "string", "description", "Patch replace k=v (update)"));
        props.put("remove", Map.of("type", "string", "description", "Patch remove fields (update)"));

        props.put("apiVersion", Map.of("type", "string", "description", "Override apiVersion"));
        props.put("api-version", Map.of("type", "string", "description", "Alias script apiVersion"));
        props.put("raw", Map.of("type", "boolean", "description", "Devuelve JSON crudo"));
        props.put("validateOnly", Map.of("type", "boolean", "description", "validateOnly (create/update). En create usa el mismo JSON Patch que la operación real."));
        props.put("validate-only", Map.of("type", "boolean", "description", "Alias validateOnly"));
        props.put("bypassRules", Map.of("type", "boolean", "description", "bypassRules"));
        props.put("bypass-rules", Map.of("type", "boolean", "description", "Alias bypassRules"));
        props.put("suppressNotifications", Map.of("type", "boolean", "description", "suppressNotifications"));
        props.put("suppress-notifications", Map.of("type", "boolean", "description", "Alias suppressNotifications"));
        props.put("errorPolicy", Map.of("type", "string", "description", "Omit|Fail (batch_get)"));

        // Parámetros para get_fields
        props.put("showPicklistItems", Map.of("type", "boolean", "description", "Incluye items de picklist en get_fields"));

        base.put("required", List.of("operation"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        String op = arguments.get("operation") == null ? "" : arguments.get("operation").toString().trim();
        if (op.isEmpty()) return error("'operation' es requerido");

        return switch (op) {
            case "get" -> delegate(getTool, arguments);
            case "create" -> delegate(createTool, arguments);
            case "update" -> delegate(updateTool, arguments);
            case "delete" -> delegate(deleteTool, arguments);
            case "batch_get" -> delegate(batchTool, arguments);
            case "bulk_delete" -> delegate(bulkDeleteTool, arguments);
            case "get_fields" -> executeGetFields(arguments);
            default -> error("Operación no soportada: " + op);
        };
    }

    private Map<String, Object> delegate(AbstractAzureDevOpsTool tool, Map<String, Object> arguments) {
        Map<String, Object> copy = new LinkedHashMap<>(arguments);
        copy.remove("operation");
        return tool.execute(copy);
    }

    private Map<String, Object> executeGetFields(Map<String, Object> arguments) {
        String project = arguments.getOrDefault("project", "").toString().trim();
        String type = arguments.getOrDefault("type", "").toString().trim();
        String apiVersion = arguments.getOrDefault("apiVersion", "7.2-preview").toString().trim();
        boolean showPicklistItems = Boolean.TRUE.equals(arguments.get("showPicklistItems"));
        boolean raw = Boolean.TRUE.equals(arguments.get("raw"));

        if (project.isEmpty()) {
            return error("El parámetro 'project' es requerido para get_fields");
        }

        if (type.isEmpty()) {
            // Si no se especifica type, solo listar tipos disponibles
            try {
                Map<String, Object> typesResp = getFieldsHelper.listTypesForProject(project, apiVersion);
                List<String> typeNames = getFieldsHelper.getTypeNames(typesResp);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("workItemTypes", typeNames);
                result.put("message", "Tipos disponibles en el proyecto '" + project + "'. Especifica 'type' para ver campos.");
                return success(result.isEmpty() ? "No se encontraron tipos." : toJson(result));
            } catch (Exception e) {
                return error("Error al listar tipos: " + e.getMessage());
            }
        }

        try {
            Map<String, Object> fieldsData = getFieldsHelper.getFieldsForType(project, type, apiVersion, showPicklistItems, raw);
            StringBuilder sb = new StringBuilder();
            sb.append("## Campos para tipo: ").append(type).append("\n\n");

            // Work item types disponibles
            if (fieldsData.get("workItemTypes") instanceof List) {
                List<?> types = (List<?>) fieldsData.get("workItemTypes");
                sb.append("### Tipos disponibles (").append(types.size()).append(")\n");
                for (Object t : types) {
                    sb.append("- ").append(t).append("\n");
                }
                sb.append("\n");
            }

            // Campos requeridos
            if (fieldsData.get("requiredFields") instanceof List) {
                List<?> reqFields = (List<?>) fieldsData.get("requiredFields");
                sb.append("### Campos Requeridos (").append(reqFields.size()).append(")\n");
                if (reqFields.isEmpty()) {
                    sb.append("No hay campos estrictamente requeridos en el WIT.\n");
                } else {
                    for (Object f : reqFields) {
                        if (f instanceof Map) {
                            Map<?, ?> field = (Map<?, ?>) f;
                            sb.append("- **").append(field.get("referenceName")).append("**");
                            if (field.get("name") != null && !field.get("name").equals(field.get("referenceName"))) {
                                sb.append(" (").append(field.get("name")).append(")");
                            }
                            if (Boolean.TRUE.equals(field.get("isPicklist"))) {
                                Object items = field.get("picklistItems");
                                if (items instanceof List) {
                                    sb.append(" | Valores: ").append(String.join(", ", ((List<?>) items).stream().map(Object::toString).toArray(String[]::new)));
                                }
                            }
                            sb.append("\n");
                        }
                    }
                }
                sb.append("\n");
            }

            // Campos con picklist
            if (fieldsData.get("picklistFields") instanceof List) {
                List<?> pickFields = (List<?>) fieldsData.get("picklistFields");
                if (!pickFields.isEmpty()) {
                    sb.append("### Campos con Valores Permitidos (").append(pickFields.size()).append(")\n");
                    for (Object f : pickFields) {
                        if (f instanceof Map) {
                            Map<?, ?> field = (Map<?, ?>) f;
                            sb.append("- **").append(field.get("referenceName")).append("**");
                            Object items = field.get("picklistItems");
                            if (items instanceof List && !((List<?>) items).isEmpty()) {
                                sb.append(" | Valores: ").append(String.join(", ", ((List<?>) items).stream().map(Object::toString).toArray(String[]::new)));
                            } else {
                                Object limited = field.get("limitedToValues");
                                if (limited instanceof List && !((List<?>) limited).isEmpty()) {
                                    sb.append(" | Valores: ").append(String.join(", ", ((List<?>) limited).stream().map(Object::toString).toArray(String[]::new)));
                                }
                            }
                            sb.append("\n");
                        }
                    }
                    sb.append("\n");
                }
            }

            // Campos sugeridos
            if (fieldsData.get("suggestedFields") instanceof List) {
                List<?> sugFields = (List<?>) fieldsData.get("suggestedFields");
                if (!sugFields.isEmpty()) {
                    sb.append("### Campos Sugeridos\n");
                    for (Object f : sugFields) {
                        if (f instanceof Map) {
                            Map<?, ?> field = (Map<?, ?>) f;
                            sb.append("- ").append(field.get("referenceName"));
                            if (field.get("name") != null && !field.get("name").equals(field.get("referenceName"))) {
                                sb.append(" (").append(field.get("name")).append(")");
                            }
                            sb.append("\n");
                        }
                    }
                    sb.append("\n");
                }
            }

            // Campos calculados (solo lectura)
            if (fieldsData.get("readOnlyFields") instanceof List) {
                List<?> roFields = (List<?>) fieldsData.get("readOnlyFields");
                if (!roFields.isEmpty()) {
                    sb.append("### Campos Calculados (solo lectura)\n");
                    for (Object f : roFields) {
                        if (f instanceof Map) {
                            Map<?, ?> field = (Map<?, ?>) f;
                            sb.append("- ").append(field.get("referenceName"));
                            sb.append("\n");
                        }
                    }
                    sb.append("\n");
                }
            }

            // Estados válidos
            if (fieldsData.get("validStates") instanceof List) {
                List<?> states = (List<?>) fieldsData.get("validStates");
                if (!states.isEmpty()) {
                    sb.append("### Estados Válidos\n");
                    sb.append("Valores para System.State: ").append(String.join(", ", states.stream().map(Object::toString).toArray(String[]::new))).append("\n");
                    sb.append("\n");
                }
            }

            return success(sb.toString());
        } catch (Exception e) {
            return error("Error al obtener campos: " + e.getMessage());
        }
    }
}
