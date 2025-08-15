package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Helper para operaciones de Recycle Bin (papelera de work items) en Azure DevOps.
 */
@Component
public class WitRecycleBinHelper {
    private final AzureDevOpsClientService azureService;

    public WitRecycleBinHelper(AzureDevOpsClientService svc) {
        this.azureService = svc;
    }

    public void validateId(Object id) {
        if (id == null) throw new IllegalArgumentException("'id' es requerido");
    }

    public void validateIds(Object ids) {
        if (ids == null || ids.toString().trim().isEmpty()) throw new IllegalArgumentException("'ids' es requerido");
    }

    public Map<String,Object> destroy(String project, Object id) {
        String idStr = id.toString().trim();
        return azureService.deleteWitApi(project,null,"recyclebin/"+idStr, "7.2-preview");
    }

    public Map<String,Object> restore(String project, Object id) {
        String idStr = id.toString().trim();
        return azureService.patchWitApi(project,null,"recyclebin/"+idStr, Map.of(), "7.2-preview");
    }

    public Map<String,Object> get(String project, Object id) {
        String idStr = id.toString().trim();
        return azureService.getWitApi(project,null,"recyclebin/"+idStr);
    }

    public Map<String,Object> getBatch(String project, Object ids) {
        List<String> idList = Arrays.stream(ids.toString().split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        Map<String,String> query = new LinkedHashMap<>();
        query.put("ids", String.join(",", idList));
        return azureService.getWitApiWithQuery(project,null,"recyclebin", query, "7.2-preview");
    }

    public Map<String,Object> list(String project) {
        return azureService.getWitApi(project,null,"recyclebin");
    }

    public Map<String,Object> listWithPagination(String project, Object skip, Object top) {
        Map<String,String> query = new LinkedHashMap<>();
        if (skip != null) query.put("$skip", skip.toString());
        if (top != null) query.put("$top", top.toString());
        if (query.isEmpty()) {
            return azureService.getWitApi(project,null,"recyclebin");
        } else {
            return azureService.getWitApiWithQuery(project,null,"recyclebin", query, "7.2-preview");
        }
    }

    public String formatDestroyResponse(Map<String,Object> resp) {
        if (Boolean.TRUE.equals(resp.get("isHttpError"))) {
            Object status = resp.get("httpStatus");
            if (Objects.equals(status, 404)) {
                return "No es posible destruir (404). Probablemente no cuentas con el permiso de eliminación permanente; solo puedes hacer soft delete.";
            }
            return "Error remoto: " + (resp.get("message") != null ? resp.get("message") : "desconocido");
        }
        return "Destroy ejecutado (si el item existía).";
    }

    public String formatRestoreResponse(Map<String,Object> resp) {
        StringBuilder sb = new StringBuilder("Restaurado ID=").append(resp.get("id"));
        Object name = resp.get("name"); if (name != null) sb.append(" | name=").append(name);
        Object url = resp.get("url"); if (url != null) sb.append(" | url=").append(url);
        return sb.toString();
    }

    public String formatGetResponse(Map<String,Object> resp) {
        StringBuilder sb = new StringBuilder();
        sb.append("RecycleBin Item ID=").append(resp.get("id"));
        Object name = resp.get("name");
        if (name != null) sb.append(" | name=").append(name);
        Object deletedDate = resp.get("deletedDate");
        if (deletedDate != null) sb.append(" | deletedDate=").append(deletedDate);
        return sb.toString();
    }

    public String formatGetBatchResponse(Map<String,Object> resp) {
        Object count = resp.get("count");
        @SuppressWarnings("unchecked") List<Map<String,Object>> value = (List<Map<String,Object>>) resp.get("value");
        if (value == null) return "Sin resultados";
        StringBuilder sb = new StringBuilder();
        sb.append("Batch eliminados: ").append(count != null ? count : value.size()).append('\n');
        for (Map<String,Object> v : value) {
            sb.append("ID=").append(v.get("id"));
            Object name = v.get("name"); if (name != null) sb.append(" | name=").append(name);
            sb.append('\n');
        }
        return sb.toString();
    }

    public String formatListResponse(Map<String,Object> resp) {
        Object count = resp.get("count");
        @SuppressWarnings("unchecked") List<Map<String,Object>> value = (List<Map<String,Object>>) resp.get("value");
        if (value == null) {
            return "Sin datos o límite excedido. Use azuredevops_wit_recyclebin_get para IDs específicos.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Items eliminados: ").append(count != null ? count : value.size()).append('\n');
        int i = 1;
        for (Map<String,Object> v : value) {
            sb.append(i++).append(") ID=").append(v.get("id"));
            Object url = v.get("url"); if (url != null) sb.append(" | url=").append(url);
            sb.append('\n');
        }
        return sb.toString();
    }
}
