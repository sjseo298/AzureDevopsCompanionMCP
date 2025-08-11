package com.mcp.server.tools.azuredevops.core;

public class ProjectsToolTest {

    // Test básico de definición
    public void testToolDefinition() {
        // Para tests unitarios simples, verificamos que la clase compile correctamente
        // y que los métodos básicos funcionen
        try {
            var tool = new ProjectsTool(null, null);
            var def = tool.getToolDefinition();
            assert def != null : "ToolDefinition no puede ser null";
            assert "azuredevops_core_get_projects".equals(def.getName()) : "Nombre incorrecto";
            assert def.getDescription() != null : "Descripción no puede ser null";
            assert def.getInputSchema() != null : "InputSchema no puede ser null";
            System.out.println("✓ testToolDefinition passed");
        } catch (Exception e) {
            System.err.println("✗ testToolDefinition failed: " + e.getMessage());
        }
    }

    // Test básico de esquema
    public void testSchemaProperties() {
        try {
            var tool = new ProjectsTool(null, null);
            var schema = tool.getInputSchema();
            assert "object".equals(schema.get("type")) : "Tipo de schema incorrecto";
            @SuppressWarnings("unchecked")
            var props = (java.util.Map<String, Object>) schema.get("properties");
            assert props.containsKey("state") : "Falta propiedad 'state'";
            assert props.containsKey("top") : "Falta propiedad 'top'";
            assert props.containsKey("continuationToken") : "Falta propiedad 'continuationToken'";
            System.out.println("✓ testSchemaProperties passed");
        } catch (Exception e) {
            System.err.println("✗ testSchemaProperties failed: " + e.getMessage());
        }
    }

    // Método para ejecutar todos los tests
    public static void main(String[] args) {
        ProjectsToolTest test = new ProjectsToolTest();
        test.testToolDefinition();
        test.testSchemaProperties();
    }
}
