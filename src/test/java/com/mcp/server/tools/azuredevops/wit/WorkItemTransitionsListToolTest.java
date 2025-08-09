package com.mcp.server.tools.azuredevops.wit;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

public class WorkItemTransitionsListToolTest {

    @Test
    void definition() {
        var tool = new WorkItemTransitionsListTool(null);
        assertEquals("azuredevops_wit_work_item_transitions_list", tool.getName());
        var schema = tool.getInputSchema();
        @SuppressWarnings("unchecked") var required = (java.util.List<String>) schema.get("required");
        assertTrue(required.contains("ids"));
        assertFalse(required.contains("project"));
    }

    @Test
    void validationIdsRequired() {
        var tool = new WorkItemTransitionsListTool(null);
        var resp = tool.execute(Map.of());
        assertTrue(Boolean.TRUE.equals(resp.get("isError")));
    }
}
