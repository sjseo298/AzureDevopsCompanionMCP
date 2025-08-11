package com.mcp.server.tools.azuredevops;

import java.util.Map;

public class AccountsToolTest {

    public void testGetToolDefinition() {
        try {
            var tool = new AccountsTool(null);
            var def = tool.getToolDefinition();
            assert def != null : "ToolDefinition no puede ser null";
            assert "azuredevops_accounts_get_accounts".equals(def.getName()) : "Nombre incorrecto";
            assert def.getDescription() != null : "Descripción no puede ser null";
            assert def.getInputSchema() != null : "InputSchema no puede ser null";
            System.out.println("✓ testGetToolDefinition passed");
        } catch (Exception e) {
            System.err.println("✗ testGetToolDefinition failed: " + e.getMessage());
        }
    }

    public void testInputSchemaStructure() {
        try {
            var tool = new AccountsTool(null);
            var schema = tool.getInputSchema();
            assert "object".equals(schema.get("type")) : "Tipo de schema incorrecto";
            @SuppressWarnings("unchecked")
            var props = (Map<String, Object>) schema.get("properties");
            assert props.containsKey("accountId") : "Falta propiedad 'accountId'";
            assert props.containsKey("ownerId") : "Falta propiedad 'ownerId'";
            assert props.containsKey("memberId") : "Falta propiedad 'memberId'";
            assert props.containsKey("properties") : "Falta propiedad 'properties'";
            System.out.println("✓ testInputSchemaStructure passed");
        } catch (Exception e) {
            System.err.println("✗ testInputSchemaStructure failed: " + e.getMessage());
        }
    }

    public void testExecuteWithoutService() {
        try {
            var tool = new AccountsTool(null);
            var resp = tool.execute(Map.of());
            assert Boolean.TRUE.equals(resp.get("isError")) : "Debería haber error sin service";
            System.out.println("✓ testExecuteWithoutService passed");
        } catch (Exception e) {
            System.err.println("✗ testExecuteWithoutService failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        AccountsToolTest test = new AccountsToolTest();
        test.testGetToolDefinition();
        test.testInputSchemaStructure();
        test.testExecuteWithoutService();
    }
}
