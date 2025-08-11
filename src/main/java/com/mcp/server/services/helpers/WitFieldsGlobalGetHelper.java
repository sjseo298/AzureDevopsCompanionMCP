package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper para obtener definición global de un campo (org-level).
 */
public class WitFieldsGlobalGetHelper {
    private final AzureDevOpsClientService azureService;
    private static final String API_VERSION = "7.2-preview";

    public WitFieldsGlobalGetHelper(AzureDevOpsClientService azureService) {
        this.azureService = azureService;
    }

    public void validate(Object fieldObj) {
        if (fieldObj == null || fieldObj.toString().trim().isEmpty()) throw new IllegalArgumentException("'field' es requerido");
    }

    public Map<String,Object> fetchField(String fieldRef) {
        String path = "fields/" + fieldRef;
        Map<String,String> q = new LinkedHashMap<>();
        q.put("api-version", API_VERSION);
        return azureService.getCoreApi("wit/" + path, q);
    }

    /**
     * Devuelve el JSON crudo de la definición global del campo
     */
    public Map<String,Object> getFieldGlobal(String fieldRef, boolean raw) {
        return fetchField(fieldRef);
    }

    public String formatFieldResponse(Map<String,Object> data) {
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
