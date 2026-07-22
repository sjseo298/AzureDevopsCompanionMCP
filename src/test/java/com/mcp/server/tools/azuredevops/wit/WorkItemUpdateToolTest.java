package com.mcp.server.tools.azuredevops.wit;

import java.util.Map;

public class WorkItemUpdateToolTest {

    public void testSchemaIncludesRepositoryId() {
        try {
            var tool = new WorkItemUpdateTool(null);
            Map<String, Object> schema = (Map<String, Object>) tool.getInputSchema();
            Map<String, Object> props = (Map<String, Object>) schema.get("properties");
            
            assert props.containsKey("repositoryId") : "Falta repositoryId en schema";
            String repoDesc = (String) ((Map<String, Object>) props.get("repositoryId")).get("description");
            assert repoDesc.contains("ArtifactLink:pr") : "Descripción de repositoryId debe mencionar ArtifactLink:pr";
            
            String relationsDesc = (String) ((Map<String, Object>) props.get("relations")).get("description");
            assert relationsDesc.contains("ArtifactLink:pr") : "Descripción de relations debe mencionar ArtifactLink:pr";
            
            System.out.println("✓ testSchemaIncludesRepositoryId passed");
        } catch (Exception e) {
            System.err.println("✗ testSchemaIncludesRepositoryId failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void testSchemaRequiredFields() {
        try {
            var tool = new WorkItemUpdateTool(null);
            Map<String, Object> schema = (Map<String, Object>) tool.getInputSchema();
            @SuppressWarnings("unchecked")
            java.util.List<String> required = (java.util.List<String>) schema.get("required");
            
            assert required.contains("project") : "project debe estar en required";
            assert required.contains("id") : "id debe estar en required";
            
            System.out.println("✓ testSchemaRequiredFields passed");
        } catch (Exception e) {
            System.err.println("✗ testSchemaRequiredFields failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        WorkItemUpdateToolTest test = new WorkItemUpdateToolTest();
        test.testSchemaIncludesRepositoryId();
        test.testSchemaRequiredFields();
    }
}
