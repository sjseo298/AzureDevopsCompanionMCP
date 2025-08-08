package com.mcp.server.tools.azuredevops;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Herramienta MCP para obtener tableros (boards) de Azure DevOps.
 * - Lista todos los tableros del equipo
 * - Obtiene un tablero específico por ID/nombre
 *
 * Parámetros obligatorios:
 *   - project (string): ID o nombre del proyecto (requerido)
 *   - team (string): ID o nombre del equipo (opcional)
 *   - boardId (string): ID/nombre del tablero (opcional, para obtener uno específico)
 *
 * Ejemplo de uso:
 *   {
 *     "project": "MiProyecto",
 *     "team": "MiEquipo" // opcional
 *   }
 *
 * El agente debe validar que 'project' esté presente y no vacío.
 */
@Component
public class BoardsTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_work_get_boards";
    private static final String DESC = "Lista los tableros de un equipo o proyecto en Azure DevOps. Requiere 'project' (string, obligatorio), 'team' (string, opcional), 'boardId' (string, opcional para obtener uno específico).";

    @Autowired
    public BoardsTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("project", Map.of("type","string","description","ID o nombre del proyecto (obligatorio)"));
        props.put("team", Map.of("type","string","description","ID o nombre del equipo (opcional)"));
        props.put("boardId", Map.of("type","string","description","ID o nombre del tablero (opcional)"));
        return Map.of(
            "type", "object",
            "properties", props,
            "required", List.of("project")
        );
    }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        String project = Optional.ofNullable(args.get("project")).map(Object::toString).map(String::trim).orElse("");
        if (project.isEmpty()) throw new IllegalArgumentException("El parámetro 'project' es obligatorio y no puede estar vacío");
        // 'team' y 'boardId' son opcionales
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) {
            return error("Servicio Azure DevOps no configurado en este entorno de prueba");
        }
        String project = getProject(arguments);
        String team = getTeam(arguments);
        String boardId = opt(arguments, "boardId");

        Map<String,Object> resp = (boardId == null)
                ? azureService.getWorkApi(project, team, "boards")
                : azureService.getWorkApi(project, team, "boards/" + boardId);

        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);

        return success(format(resp));
    }

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
            StringBuilder sb = new StringBuilder("=== Boards ===\n\n");
            List<?> list = (List<?>) data.get("value");
            int i=1;
            for (Object o : list) {
                if (o instanceof Map) {
                    Map<?,?> m = (Map<?,?>) o;
                    sb.append(i++).append(". ");
                    Object name = m.get("name");
                    Object id = m.get("id");
                    sb.append(name != null ? name : "(sin nombre)");
                    if (id != null) sb.append(" [").append(id).append("]");
                    sb.append("\n");
                }
            }
            return sb.toString();
        }

        if (data.containsKey("id") || data.containsKey("name")) {
            StringBuilder sb = new StringBuilder("=== Board Detail ===\n\n");
            if (data.get("name") != null) sb.append("Nombre: ").append(data.get("name")).append("\n");
            if (data.get("id") != null) sb.append("ID: ").append(data.get("id")).append("\n");
            if (data.get("url") != null) sb.append("URL: ").append(data.get("url")).append("\n");
            return sb.toString();
        }

        return data.toString();
    }
}
