package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;

import java.util.*;

/**
 * Tool MCP: azuredevops_wit_recyclebin_destroy
 * Destruye permanentemente (Destroy) un work item que está en la Recycle Bin.
 * Endpoint: DELETE /{project}/_apis/wit/recyclebin/{id}?api-version=7.2-preview
 */
public class RecycleBinDestroyTool extends AbstractAzureDevOpsTool {
    private static final String NAME = "azuredevops_wit_recyclebin_destroy";
    private static final String DESC = "Elimina permanentemente un work item eliminado. Si retorna 404, se informa que no hay permisos y solo se puede soft delete.";

    public RecycleBinDestroyTool(AzureDevOpsClientService svc) { super(svc); }

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESC; }

    @Override
    public Map<String,Object> getInputSchema() {
        Map<String,Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String,Object>) base.get("properties");
        props.put("id", Map.of("type","integer","description","ID del work item en Recycle Bin"));
        @SuppressWarnings("unchecked") List<String> req = (List<String>) base.get("required");
        if (!req.contains("id")) req.add("id");
        return base;
    }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        super.validateCommon(args);
        if (args.get("id") == null) throw new IllegalArgumentException("'id' es requerido");
    }

    @Override
    protected Map<String,Object> executeInternal(Map<String,Object> args) {
        String project = getProject(args);
        String id = args.get("id").toString().trim();
        Map<String,Object> resp = azureService.deleteWitApi(project,null,"recyclebin/"+id, "7.2-preview");
        // Para delete devolvemos {} en éxito, errores vienen marcados
        if (Boolean.TRUE.equals(resp.get("isHttpError"))) {
            Object status = resp.get("httpStatus");
            if (Objects.equals(status, 404)) {
                return success("No es posible destruir (404). Probablemente no cuentas con el permiso de eliminación permanente; solo puedes hacer soft delete.");
            }
            String formatted = tryFormatRemoteError(resp);
            return success(formatted != null ? formatted : "Error remoto desconocido");
        }
        return success("Destroy ejecutado (si el item existía)." );
    }
}
