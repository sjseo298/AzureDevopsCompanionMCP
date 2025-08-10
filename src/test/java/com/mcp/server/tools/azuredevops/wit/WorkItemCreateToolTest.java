package com.mcp.server.tools.azuredevops.wit;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class WorkItemCreateToolTest {

    @Test
    void definition() {
        var tool = new WorkItemCreateTool(null);
        assertEquals("azuredevops_wit_work_item_create", tool.getName());
        var schema = tool.getInputSchema();
        @SuppressWarnings("unchecked") var required = (java.util.List<String>) schema.get("required");
        assertTrue(required.contains("project"));
        assertTrue(required.contains("type"));
        assertTrue(required.contains("title"));
        @SuppressWarnings("unchecked") var props = (java.util.Map<String,Object>) schema.get("properties");
        assertTrue(props.containsKey("state"));
        assertTrue(props.containsKey("fields"));
        assertTrue(props.containsKey("parentId"));
        assertTrue(props.containsKey("relations"));
        assertTrue(props.containsKey("validateOnly"));
        assertTrue(props.containsKey("apiVersion"));
    }

    @Test
    void missingTitleValidation() {
        var tool = new WorkItemCreateTool(null);
        var resp = tool.execute(Map.of("project","p","type","Bug"));
        assertTrue(Boolean.TRUE.equals(resp.get("isError")));
    }
}
