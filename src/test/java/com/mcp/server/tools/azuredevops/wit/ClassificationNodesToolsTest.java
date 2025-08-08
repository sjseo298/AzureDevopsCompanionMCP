package com.mcp.server.tools.azuredevops.wit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClassificationNodesToolsTest {

    @Test
    void testGetDefinition() {
        ClassificationNodesGetTool tool = new ClassificationNodesGetTool(null);
        var def = tool.getToolDefinition();
        assertEquals("azuredevops_wit_classification_nodes_get", def.getName());
        assertNotNull(def.getDescription());
        assertNotNull(def.getInputSchema());
    }

    @Test
    void testPutDefinition() {
        ClassificationNodesCreateOrUpdateTool tool = new ClassificationNodesCreateOrUpdateTool(null);
        var def = tool.getToolDefinition();
        assertEquals("azuredevops_wit_classification_nodes_create_or_update", def.getName());
        assertNotNull(def.getDescription());
        assertNotNull(def.getInputSchema());
    }

    @Test
    void testPatchDefinition() {
        ClassificationNodesUpdateTool tool = new ClassificationNodesUpdateTool(null);
        var def = tool.getToolDefinition();
        assertEquals("azuredevops_wit_classification_nodes_update", def.getName());
        assertNotNull(def.getDescription());
        assertNotNull(def.getInputSchema());
    }

    @Test
    void testDeleteDefinition() {
        ClassificationNodesDeleteTool tool = new ClassificationNodesDeleteTool(null);
        var def = tool.getToolDefinition();
        assertEquals("azuredevops_wit_classification_nodes_delete", def.getName());
        assertNotNull(def.getDescription());
        assertNotNull(def.getInputSchema());
    }

    @Test
    void testGetRootDefinition() {
        ClassificationNodesGetRootTool tool = new ClassificationNodesGetRootTool(null);
        var def = tool.getToolDefinition();
        assertEquals("azuredevops_wit_classification_nodes_get_root", def.getName());
        assertNotNull(def.getDescription());
        assertNotNull(def.getInputSchema());
    }

    @Test
    void testGetByIdsDefinition() {
        ClassificationNodesGetByIdsTool tool = new ClassificationNodesGetByIdsTool(null);
        var def = tool.getToolDefinition();
        assertEquals("azuredevops_wit_classification_nodes_get_by_ids", def.getName());
        assertNotNull(def.getDescription());
        assertNotNull(def.getInputSchema());
    }
}
