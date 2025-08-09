package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_work_item_icons_get
 * Obtiene un ícono específico (GET _apis/wit/workitemicons/{icon}).
 */
@Component
public class WorkItemIconsGetTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_work_item_icons_get";
    private static final String DESC = "Obtiene un ícono de work item por id (prechequea existencia salvo que se indique lo contrario).";
    private static final String API_VERSION = "7.2-preview.1";

    @Autowired
    public WorkItemIconsGetTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String, Object> getInputSchema() {
        // Crear copia mutable del schema base (el Map.of original es inmutable)
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String,Object>) base.get("properties");
        props.put("icon", Map.of("type","string","description","ID del ícono (ej: icon_insect)"));
        props.put("noPrecheck", Map.of("type","boolean","description","Si true, omite la validación previa de existencia"));
        @SuppressWarnings("unchecked") List<String> existingReq = (List<String>) base.get("required");
        List<String> req = new ArrayList<>(existingReq); // asegurar mutabilidad
        if (!req.contains("icon")) req.add("icon");
        base.put("required", req);
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado");
        Object iconObj = arguments.get("icon");
        if (iconObj == null || iconObj.toString().trim().isEmpty()) return error("'icon' es requerido");
        String icon = iconObj.toString().trim();
        boolean noPre = Boolean.TRUE.equals(arguments.get("noPrecheck"));

        // Prechequeo (listado) para sugerencias si el ícono no existe localmente
        if (!noPre) {
            Map<String,Object> listResp = azureService.getWitApiWithQuery(null,null, "workitemicons", null, API_VERSION);
            String listErr = tryFormatRemoteError(listResp);
            if (listErr != null) {
                // No interrumpimos, pero devolvemos el error de listado antes de intentar el GET directo
                return success(listErr);
            }
            @SuppressWarnings("unchecked") List<Map<String,Object>> values = (List<Map<String,Object>>) listResp.get("value");
            if (values != null) {
                boolean exists = false;
                List<String> ids = new ArrayList<>();
                for (Map<String,Object> v : values) {
                    Object id = v.get("id");
                    if (id != null) {
                        String s = id.toString();
                        ids.add(s);
                        if (s.equals(icon)) exists = true;
                    }
                }
                if (!exists) {
                    // Generar sugerencias (substring flexible reemplazando _ por .*)
                    String pattern = icon.replace("_", ".*").toLowerCase(Locale.ROOT);
                    List<String> sug = new ArrayList<>();
                    for (String id : ids) {
                        if (id.toLowerCase(Locale.ROOT).matches(".*"+pattern+".*")) {
                            sug.add(id);
                            if (sug.size() >= 10) break;
                        }
                    }
                    if (sug.isEmpty()) {
                        // fallback primeras 10
                        for (int i=0; i<Math.min(10, ids.size()); i++) sug.add(ids.get(i));
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("Icono ' ").append(icon).append(" ' no encontrado. Sugerencias:\n");
                    for (String s : sug) sb.append(" - ").append(s).append('\n');
                    return success(sb.toString());
                }
            }
        }

        String endpoint = "workitemicons/" + icon;
        Map<String,Object> resp = azureService.getWitApiWithQuery(null,null, endpoint, null, API_VERSION);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        return success(format(resp));
    }

    private String format(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Sin datos)";
        StringBuilder sb = new StringBuilder();
        sb.append("Icono: ").append(data.get("id"));
        Object upd = data.get("updatedTime");
        if (upd != null) sb.append(" | updated=").append(upd);
        Object url = data.get("url");
        if (url != null) sb.append("\nURL: ").append(url);
        return sb.toString();
    }
}
