package com.mcp.server.tools.azuredevops;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class AccountsTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_accounts_get_accounts";
    private static final String DESC = "Lista cuentas (Accounts) o devuelve el detalle de una cuenta por ID";

    @Autowired
    public AccountsTool(AzureDevOpsClientService client) {
        super(client);
    }

    @Override
    public String getName() { return NAME; }

    @Override
    public String getDescription() { return DESC; }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        // Accounts no requiere 'project'
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> schema = new HashMap<>();
        Map<String,Object> props = new HashMap<>();
        props.put("accountId", Map.of(
                "type", "string",
                "description", "GUID de la cuenta a consultar (si se omite, se listan cuentas)"
        ));
        props.put("ownerId", Map.of(
                "type", "string",
                "description", "Filtrar por GUID del propietario (list)"
        ));
        props.put("memberId", Map.of(
                "type", "string",
                "description", "Filtrar por GUID de miembro (list)"
        ));
        props.put("properties", Map.of(
                "type", "string",
                "description", "Lista separada por comas de propiedades adicionales (list)"
        ));
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", new ArrayList<>()); // nada requerido
        return schema;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) {
            return error("Servicio Azure DevOps no configurado en este entorno");
        }
        String accountId = opt(arguments, "accountId");

        Map<String,Object> resp;
        if (accountId != null) {
            resp = azureService.getVsspsApi("accounts/" + accountId);
        } else {
            StringBuilder q = new StringBuilder("accounts");
            List<String> query = new ArrayList<>();
            String ownerId = opt(arguments, "ownerId");
            String memberId = opt(arguments, "memberId");
            String properties = opt(arguments, "properties");
            if (ownerId != null) query.add("ownerId=" + encode(ownerId));
            if (memberId != null) query.add("memberId=" + encode(memberId));
            if (properties != null) query.add("properties=" + encode(properties));
            if (!query.isEmpty()) {
                q.append("?").append(String.join("&", query));
            }
            resp = azureService.getVsspsApi(q.toString());
        }

        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);

        return success(format(resp));
    }

    private String opt(Map<String,Object> args, String key) {
        Object v = args.get(key);
        if (v == null) return null;
        String s = Objects.toString(v, "").trim();
        return s.isEmpty() ? null : s;
    }

    private String encode(String s) { return s.replace(" ", "%20"); }

    private String format(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Respuesta vacía)";
        if (data.containsKey("error")) return "Error remoto: " + data.get("error");
        if (data.containsKey("value") && data.get("value") instanceof List) {
            StringBuilder sb = new StringBuilder("=== Accounts ===\n\n");
            List<?> list = (List<?>) data.get("value");
            int i=1;
            for (Object o : list) {
                if (o instanceof Map) {
                    Map<?,?> m = (Map<?,?>) o;
                    Object name = m.get("accountName");
                    Object id = m.get("accountId");
                    Object type = m.get("accountType");
                    sb.append(i++).append(". ")
                      .append(name!=null? name: "(sin nombre)")
                      .append(" [").append(id).append("]");
                    if (type!=null) sb.append(" (type ").append(type).append(")");
                    sb.append("\n");
                }
            }
            return sb.toString();
        }
        if (data.containsKey("accountId")) {
            StringBuilder sb = new StringBuilder("=== Account Detail ===\n\n");
            sb.append("ID: ").append(data.get("accountId")).append("\n");
            sb.append("Nombre: ").append(data.get("accountName")).append("\n");
            if (data.get("organizationName")!=null) sb.append("Organización: ").append(data.get("organizationName")).append("\n");
            if (data.get("createdDate")!=null) sb.append("Creado: ").append(data.get("createdDate")).append("\n");
            if (data.get("accountType")!=null) sb.append("Tipo: ").append(data.get("accountType")).append("\n");
            return sb.toString();
        }
        return data.toString();
    }
}
