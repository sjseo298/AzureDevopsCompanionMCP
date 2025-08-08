package com.mcp.server.tools.azuredevops.core;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TeamsToolTest {

    @Test
    void testToolDefinition() {
        TeamsTool tool = new TeamsTool(null);
        var def = tool.getToolDefinition();
        assertNotNull(def);
        assertEquals("azuredevops_core_get_teams", def.getName());
        assertNotNull(def.getDescription());
        assertNotNull(def.getInputSchema());
    }

    @Test
    void testSchemaRequiresProjectId() {
        TeamsTool tool = new TeamsTool(null);
        Map<String,Object> schema = tool.getInputSchema();
        assertEquals("object", schema.get("type"));
        @SuppressWarnings("unchecked")
        Map<String,Object> props = (Map<String, Object>) schema.get("properties");
        assertTrue(props.containsKey("projectId"));
        @SuppressWarnings("unchecked")
        var required = (java.util.List<String>) schema.get("required");
        assertTrue(required.contains("projectId"));
    }

    @Test
    void testValidationMissingProjectId() {
        TeamsTool tool = new TeamsTool(null);
        Map<String,Object> args = new HashMap<>();
        Map<String,Object> res = tool.execute(args);
        assertTrue(Boolean.TRUE.equals(res.get("isError")));
    }
}
