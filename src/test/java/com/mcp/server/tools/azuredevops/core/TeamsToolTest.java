package com.mcp.server.tools.azuredevops.core;

import java.util.HashMap;
import java.util.Map;

public class TeamsToolTest {

    public void testToolDefinition() {
        try {
            var tool = new TeamsTool(null, null);
            var def = tool.getToolDefinition();
            assert def != null : "ToolDefinition no puede ser null";
            assert "azuredevops_core_get_teams".equals(def.getName()) : "Nombre incorrecto";
            assert def.getDescription() != null : "Descripción no puede ser null";
            assert def.getInputSchema() != null : "InputSchema no puede ser null";
            System.out.println("✓ testToolDefinition passed");
        } catch (Exception e) {
            System.err.println("✗ testToolDefinition failed: " + e.getMessage());
        }
    }

    public void testSchemaRequiresProjectId() {
        try {
            var tool = new TeamsTool(null, null);
            var schema = tool.getInputSchema();
            assert "object".equals(schema.get("type")) : "Tipo de schema incorrecto";
            @SuppressWarnings("unchecked")
            var props = (Map<String, Object>) schema.get("properties");
            assert props.containsKey("projectId") : "Falta propiedad 'projectId'";
            @SuppressWarnings("unchecked")
            var required = (java.util.List<String>) schema.get("required");
            assert required != null && required.contains("projectId") : "projectId debería ser requerido";
            System.out.println("✓ testSchemaRequiresProjectId passed");
        } catch (Exception e) {
            System.err.println("✗ testSchemaRequiresProjectId failed: " + e.getMessage());
        }
    }

    public void testValidationMissingProjectId() {
        try {
            var tool = new TeamsTool(null, null);
            Map<String,Object> args = new HashMap<>();
            Map<String,Object> res = tool.execute(args);
            assert Boolean.TRUE.equals(res.get("isError")) : "Debería haber error por projectId faltante";
            System.out.println("✓ testValidationMissingProjectId passed");
        } catch (Exception e) {
            System.err.println("✗ testValidationMissingProjectId failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        TeamsToolTest test = new TeamsToolTest();
        test.testToolDefinition();
        test.testSchemaRequiresProjectId();
        test.testValidationMissingProjectId();
    }
}
