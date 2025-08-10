package com.mcp.server.tools.azuredevops.workitemtrackingprocess;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PicklistsGetToolTest {

    @Test
    void testDefinition() {
        PicklistsGetTool tool = new PicklistsGetTool(null);
        var def = tool.getToolDefinition();
        assertEquals("azuredevops_workitemtrackingprocess_picklists_get", def.getName());
        assertNotNull(def.getDescription());
        assertNotNull(def.getInputSchema());
    assertNotNull(def.getInputSchema().getProperties());
    assertTrue(def.getInputSchema().getProperties().containsKey("id"));
    }
}
