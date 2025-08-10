package com.mcp.server.tools.azuredevops.wit;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

public class WorkItemGetToolTest {

    @Test
    void definition() {
        var tool = new WorkItemGetTool(null);
        assertEquals("azuredevops_wit_work_item_get", tool.getName());
        var schema = tool.getInputSchema();
        @SuppressWarnings("unchecked") var required = (java.util.List<String>) schema.get("required");
        assertTrue(required.contains("project"));
        assertTrue(required.contains("id"));
        @SuppressWarnings("unchecked") var props = (java.util.Map<String,Object>) schema.get("properties");
        assertTrue(props.containsKey("fields"));
        assertTrue(props.containsKey("expand"));
        assertTrue(props.containsKey("asOf"));
        assertTrue(props.containsKey("raw"));
        assertTrue(props.containsKey("apiVersion"));
    }

    @Test
    void idRequiredValidation() {
        var tool = new WorkItemGetTool(null);
        var resp = tool.execute(Map.of("project","p"));
        assertTrue(Boolean.TRUE.equals(resp.get("isError")));
    }
}
