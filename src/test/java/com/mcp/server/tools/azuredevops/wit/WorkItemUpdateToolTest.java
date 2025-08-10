package com.mcp.server.tools.azuredevops.wit;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class WorkItemUpdateToolTest {

    @Test
    void definition() {
        var tool = new WorkItemUpdateTool(null);
        assertEquals("azuredevops_wit_work_item_update", tool.getName());
        var schema = tool.getInputSchema();
        @SuppressWarnings("unchecked") var required = (java.util.List<String>) schema.get("required");
        assertTrue(required.contains("project"));
        assertTrue(required.contains("id"));
        @SuppressWarnings("unchecked") var props = (java.util.Map<String,Object>) schema.get("properties");
        assertTrue(props.containsKey("add"));
        assertTrue(props.containsKey("replace"));
        assertTrue(props.containsKey("remove"));
        assertTrue(props.containsKey("parentId"));
        assertTrue(props.containsKey("relations"));
        assertTrue(props.containsKey("validateOnly"));
    }

    @Test
    void idValidation() {
        var tool = new WorkItemUpdateTool(null);
        var resp = tool.execute(Map.of("project","p"));
        assertTrue(Boolean.TRUE.equals(resp.get("isError")));
    }
}
