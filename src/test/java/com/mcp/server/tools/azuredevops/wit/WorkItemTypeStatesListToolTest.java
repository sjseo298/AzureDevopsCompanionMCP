package com.mcp.server.tools.azuredevops.wit;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

public class WorkItemTypeStatesListToolTest {

    @Test
    void definition() {
        var tool = new WorkItemTypeStatesListTool(null);
        assertEquals("azuredevops_wit_work_item_type_states_list", tool.getName());
        var schema = tool.getInputSchema();
        @SuppressWarnings("unchecked") var req = (java.util.List<String>) schema.get("required");
        assertTrue(req.contains("project"));
        assertTrue(req.contains("type"));
    }

    @Test
    void validationTypeRequired() {
        var tool = new WorkItemTypeStatesListTool(null);
        var resp = tool.execute(Map.of("project","p"));
        assertTrue(Boolean.TRUE.equals(resp.get("isError")));
    }
}
