package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_fields_global_get
 * Derivado de script scripts/curl/wit/fields_global_get.sh
 * Endpoint org-level: GET /_apis/wit/fields/{referenceName}?api-version=7.2-preview
 */
@Component
public class FieldsGlobalGetTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_wit_fields_global_get";
    private static final String DESC = "Obtiene definición global de un campo (tipo, usage, readOnly, picklistId, valores).";
    private static final String API_VERSION = "7.2-preview"; // según script

    @Autowired
    public FieldsGlobalGetTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    // Org-level, no requiere project
    @Override protected void validateCommon(Map<String,Object> args) { /* no project */ }

    @Override
    public Map<String,Object> getInputSchema() {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("field", Map.of("type","string","description","referenceName del campo"));
        props.put("raw", Map.of("type","boolean","description","Si true, devuelve JSON crudo"));
        return Map.of(
            "type","object",
            "properties", props,
            "required", List.of("field")
        );
    }

    @Override
    protected Map<String,Object> executeInternal(Map<String,Object> arguments) {
        if (azureService == null) return error("Servicio Azure DevOps no configurado");
        Object fObj = arguments.get("field");
        if (fObj == null || fObj.toString().trim().isEmpty()) return error("'field' es requerido");
        String fieldRef = fObj.toString().trim();
        boolean raw = Boolean.TRUE.equals(arguments.get("raw"));

        // GET global field metadata
        String path = "fields/" + fieldRef; // org-level _apis/wit/fields/{ref}
        Map<String,String> q = new LinkedHashMap<>();
        q.put("api-version", API_VERSION);
        // Reutilizamos getCoreApi con prefijo 'wit/' ya que es org-level sin {project}
        Map<String,Object> resp = azureService.getCoreApi("wit/" + path, q);
        String err = tryFormatRemoteError(resp);
        if (err != null) return success(err);
        if (raw) return Map.of("isError", false, "raw", resp);
        return success(format(resp));
    }

    private String format(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(sin datos)";
        StringBuilder sb = new StringBuilder();
        Object referenceName = data.get("referenceName");
        Object name = data.get("name");
        Object type = data.get("type");
        Object usage = data.get("usage");
        Object readOnly = data.get("readOnly");
        Object picklistId = data.get("picklistId");
        sb.append(referenceName != null ? referenceName : "(sin ref)");
        sb.append(" - ").append(name != null ? name : "(sin nombre)");
        if (type != null) sb.append(" | type=").append(type);
        if (usage != null) sb.append(" | usage=").append(usage);
        if (readOnly != null) sb.append(" | readOnly=").append(readOnly);
        if (picklistId != null) sb.append(" | picklistId=").append(picklistId);
        Object values = data.get("values");
        if (values instanceof List<?>) {
            @SuppressWarnings("unchecked") List<Object> list = (List<Object>) values;
            sb.append("\nValores (").append(list.size()).append("):");
            int i=1; for (Object v : list) { sb.append("\n  ").append(i++).append(") ").append(v); }
        }
        return sb.toString();
    }
}
