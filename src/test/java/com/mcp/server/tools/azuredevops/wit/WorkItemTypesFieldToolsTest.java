package com.mcp.server.tools.azuredevops.wit;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

public class WorkItemTypesFieldToolsTest {

    @Test
    void listDefinition() {
        var tool = new WorkItemTypesFieldListTool(null);
        assertEquals("azuredevops_wit_work_item_types_field_list", tool.getName());
        var schema = tool.getInputSchema();
        @SuppressWarnings("unchecked") var required = (java.util.List<String>) schema.get("required");
        assertTrue(required.contains("project"));
        assertTrue(required.contains("type"));
    }

    @Test
    void getDefinition() {
        var tool = new WorkItemTypesFieldGetTool(null);
        assertEquals("azuredevops_wit_work_item_types_field_get", tool.getName());
        var schema = tool.getInputSchema();
        @SuppressWarnings("unchecked") var required = (java.util.List<String>) schema.get("required");
        assertTrue(required.contains("field"));
    }

    @Test
    void validationFieldRequired() {
        var tool = new WorkItemTypesFieldGetTool(null);
        var resp = tool.execute(Map.of("project","p","type","Bug"));
        assertTrue(Boolean.TRUE.equals(resp.get("isError")));
    }
}
