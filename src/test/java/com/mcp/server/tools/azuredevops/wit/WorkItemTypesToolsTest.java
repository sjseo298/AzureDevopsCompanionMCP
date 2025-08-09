package com.mcp.server.tools.azuredevops.wit;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

public class WorkItemTypesToolsTest {

    @Test
    void listDefinition() {
        var tool = new WorkItemTypesListTool(null);
        assertEquals("azuredevops_wit_work_item_types_list", tool.getName());
        var schema = tool.getInputSchema();
        @SuppressWarnings("unchecked") var required = (java.util.List<String>) schema.get("required");
        assertTrue(required.contains("project"));
    }

    @Test
    void getDefinition() {
        var tool = new WorkItemTypesGetTool(null);
        assertEquals("azuredevops_wit_work_item_types_get", tool.getName());
        var schema = tool.getInputSchema();
        @SuppressWarnings("unchecked") var required = (java.util.List<String>) schema.get("required");
        assertTrue(required.contains("project"));
        assertTrue(required.contains("type"));
    }

    @Test
    void validationTypeRequired() {
        var tool = new WorkItemTypesGetTool(null);
        var resp = tool.execute(Map.of("project","p"));
        assertTrue(Boolean.TRUE.equals(resp.get("isError")));
    }
}
