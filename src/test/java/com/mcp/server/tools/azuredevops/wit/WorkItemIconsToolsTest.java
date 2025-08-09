package com.mcp.server.tools.azuredevops.wit;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

public class WorkItemIconsToolsTest {

    @Test
    void listDefinition() {
        var tool = new WorkItemIconsListTool(null);
        assertEquals("azuredevops_wit_work_item_icons_list", tool.getName());
        assertTrue(tool.getDescription().contains("íconos"));
        var schema = tool.getInputSchema();
        assertTrue(schema.containsKey("properties"));
    }

    @Test
    void getDefinition() {
        var tool = new WorkItemIconsGetTool(null);
        assertEquals("azuredevops_wit_work_item_icons_get", tool.getName());
        var schema = tool.getInputSchema();
        @SuppressWarnings("unchecked") var required = (java.util.List<String>) schema.get("required");
        assertTrue(required.contains("icon"));
    }

    @Test
    void validationIconRequired() {
        var tool = new WorkItemIconsGetTool(null);
        var resp = tool.execute(Map.of());
        assertTrue(Boolean.TRUE.equals(resp.get("isError")));
    }
}
