package com.mcp.server.tools.azuredevops;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class BacklogsTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_work_get_backlogs";
    private static final String DESC = "Obtiene niveles de backlog o work items asociados en Azure DevOps";

    @Autowired
    public BacklogsTool(AzureDevOpsClientService service) {
        super(service);
    }

    @Override
    public String getName() { return NAME; }

    @Override
    public String getDescription() { return DESC; }

    @Override
    public Map<String,Object> getInputSchema() {
        Map<String,Object> base = createBaseSchema();
        @SuppressWarnings("unchecked")
        Map<String,Object> props = (Map<String,Object>) base.get("properties");
        props.put("backlogId", Map.of(
            "type", "string",
            "description", "ID del backlog (opcional)"
        ));
        props.put("includeWorkItems", Map.of(
            "type", "boolean",
            "description", "Si true y backlogId presente, devuelve sus work items"
        ));
        // Hacer 'team' obligatorio para este endpoint (según comportamiento observado)
        Map<String,Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", List.of("project", "team"));
        return schema;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) {
            return error("Servicio Azure DevOps no configurado en este entorno de prueba");
        }
        String project = getProject(arguments);
        String team = getTeam(arguments);
        // Validaciones específicas
        if (team == null || team.isBlank()) {
            return error("El parámetro 'team' es obligatorio para Backlogs en esta organización");
        }
        if (containsSlash(project)) return error("'project' no debe contener '/'");
        if (containsSlash(team)) return error("'team' no debe contener '/'");
        String backlogId = opt(arguments, "backlogId");
        if (backlogId != null) {
            if (containsSlash(backlogId)) return error("'backlogId' no debe contener '/'");
            if (!backlogId.matches("[A-Za-z0-9._-]+")) {
                return error("'backlogId' tiene un formato inválido (permitido: letras, números, '.', '_' y '-')");
            }
        }
        boolean includeWorkItems = Boolean.TRUE.equals(arguments.get("includeWorkItems"));

        Map<String,Object> resp;
        if (backlogId == null) {
            resp = azureService.getWorkApi(project, team, "backlogs");
        } else if (includeWorkItems) {
            resp = azureService.getWorkApi(project, team, "backlogs/" + backlogId + "/workItems");
        } else {
            resp = azureService.getWorkApi(project, team, "backlogs/" + backlogId);
        }

        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);

        return success(format(resp));
    }

    private boolean containsSlash(String s) { return s != null && (s.contains("/") || s.contains("\\")); }

    private String opt(Map<String,Object> args, String k) {
        Object v = args.get(k);
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty()? null : s;
    }

    private String format(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Respuesta vacía)";
        if (data.containsKey("error")) return "Error remoto: " + data.get("error");
        if (data.containsKey("value") && data.get("value") instanceof List) {
            StringBuilder sb = new StringBuilder("=== Backlog Levels ===\n\n");
            List<?> list = (List<?>) data.get("value");
            int i=1;
            for (Object o : list) {
                if (o instanceof Map) {
                    Map<?,?> m = (Map<?,?>) o;
                    sb.append(i++).append(". ");
                    Object name = m.get("name");
                    Object id = m.get("id");
                    Object rank = m.get("rank");
                    sb.append(name!=null? name: "(sin nombre)");
                    if (id!=null) sb.append(" [").append(id).append("]");
                    if (rank!=null) sb.append(" (rank ").append(rank).append(")");
                    Object wits = m.get("workItemTypes");
                    if (wits instanceof List) {
                        List<?> wl = (List<?>) wits;
                        sb.append(" - WITs: ").append(wl.size());
                    }
                    sb.append("\n");
                }
            }
            return sb.toString();
        }
        if (data.containsKey("workItems") && data.get("workItems") instanceof List) {
            StringBuilder sb = new StringBuilder("=== Backlog Work Items ===\n\n");
            List<?> wi = (List<?>) data.get("workItems");
            int i=1;
            for (Object o: wi) {
                sb.append(i++).append(". ");
                if (o instanceof Map) {
                    Map<?,?> m = (Map<?,?>) o;
                    Object id = m.get("targetWorkItemId");
                    if (id==null) id = m.get("id");
                    sb.append("ID: ").append(id);
                } else sb.append(o);
                sb.append("\n");
            }
            return sb.toString();
        }
        if (data.containsKey("id") && data.containsKey("name")) {
            StringBuilder sb = new StringBuilder("=== Backlog Detail ===\n\n");
            sb.append("ID: ").append(data.get("id")).append("\n");
            sb.append("Nombre: ").append(data.get("name")).append("\n");
            if (data.get("rank")!=null) sb.append("Rank: ").append(data.get("rank")).append("\n");
            Object wits = data.get("workItemTypes");
            if (wits instanceof List) {
                List<?> wl = (List<?>) wits;
                StringBuilder tipos = new StringBuilder();
                for (Object x : wl) {
                    if (tipos.length() > 0) tipos.append(", ");
                    if (x instanceof Map) {
                        Map<?,?> mm = (Map<?,?>) x;
                        Object n = mm.get("name");
                        tipos.append(n != null ? n.toString() : "?");
                    } else {
                        tipos.append(x.toString());
                    }
                }
                if (tipos.length()==0) tipos.append("-");
                sb.append("Work Item Types: ").append(tipos).append("\n");
            }
            return sb.toString();
        }
        return data.toString();
    }
}
