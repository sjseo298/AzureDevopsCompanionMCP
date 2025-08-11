package com.mcp.server.tools.azuredevops;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.mcp.server.services.helpers.ProfileGetMyMemberIdHelper;

import java.util.List;
import java.util.Map;

@Component
public class GetMyMemberIdTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_profile_get_my_memberid";
    private static final String DESC = "Obtiene el memberId (GUID) del usuario autenticado desde Profiles (VSSPS)";

    private final ProfileGetMyMemberIdHelper helper;

    @Autowired
    public GetMyMemberIdTool(AzureDevOpsClientService service, ProfileGetMyMemberIdHelper helper) {
        super(service);
        this.helper = helper;
    }

    @Override
    public String getName() { return NAME; }

    @Override
    public String getDescription() { return DESC; }

    @Override
    protected void validateCommon(Map<String, Object> args) {
        // No requiere 'project' ni 'team'
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(),
            "required", List.of()
        );
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        if (azureService == null) {
            return error("Servicio Azure DevOps no configurado en este entorno");
        }
        Map<String,Object> resp = helper.fetchMyProfile();
        String formattedErr = tryFormatRemoteError(resp);
        if (formattedErr != null) return success(formattedErr);
        return success(helper.formatProfileResponse(resp));
    }
}
