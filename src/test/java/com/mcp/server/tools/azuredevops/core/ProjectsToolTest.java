package com.mcp.server.tools.azuredevops.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ProjectsToolTest {

    @Test
    void testToolDefinition() {
        ProjectsTool tool = new ProjectsTool(null);
        var def = tool.getToolDefinition();
        assertNotNull(def);
        assertEquals("azuredevops_core_get_projects", def.getName());
        assertNotNull(def.getDescription());
        assertNotNull(def.getInputSchema());
    }

    @Test
    void testSchemaProperties() {
        ProjectsTool tool = new ProjectsTool(null);
        Map<String,Object> schema = tool.getInputSchema();
        assertEquals("object", schema.get("type"));
        @SuppressWarnings("unchecked")
        Map<String,Object> props = (Map<String, Object>) schema.get("properties");
        assertTrue(props.containsKey("state"));
        assertTrue(props.containsKey("top"));
        assertTrue(props.containsKey("continuationToken"));
    }
}
