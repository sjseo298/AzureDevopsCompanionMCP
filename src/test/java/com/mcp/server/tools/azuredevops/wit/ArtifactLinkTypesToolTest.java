package com.mcp.server.tools.azuredevops.wit;

public class ArtifactLinkTypesToolTest {

    public void testDefinition() {
        try {
            var tool = new ArtifactLinkTypesTool(null, null);
            var def = tool.getToolDefinition();
            assert "azuredevops_wit_get_artifact_link_types".equals(def.getName()) : "Nombre incorrecto";
            assert def.getDescription() != null : "Descripción no puede ser null";
            assert def.getInputSchema() != null : "InputSchema no puede ser null";
            System.out.println("✓ testDefinition passed");
        } catch (Exception e) {
            System.err.println("✗ testDefinition failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        ArtifactLinkTypesToolTest test = new ArtifactLinkTypesToolTest();
        test.testDefinition();
    }
}
