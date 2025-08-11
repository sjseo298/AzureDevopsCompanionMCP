package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Helper para obtener el memberId del usuario autenticado desde Profiles (VSSPS).
 */
@Component
public class ProfileGetMyMemberIdHelper {
    private final AzureDevOpsClientService azureService;

    public ProfileGetMyMemberIdHelper(AzureDevOpsClientService svc) {
        this.azureService = svc;
    }

    public Map<String,Object> fetchMyProfile() {
        return azureService.getVsspsApi("profile/profiles/me");
    }

    public String formatProfileResponse(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Respuesta vacía)";
        if (data.containsKey("error")) return "Error remoto: " + data.get("error");
        StringBuilder sb = new StringBuilder("=== Mi Perfil (memberId) ===\n\n");
        sb.append("ID: ").append(data.get("id")).append("\n");
        Object dn = data.get("displayName");
        if (dn != null) sb.append("Nombre: ").append(dn).append("\n");
        Object mail = data.get("emailAddress");
        if (mail != null) sb.append("Email: ").append(mail).append("\n");
        return sb.toString();
    }
}
