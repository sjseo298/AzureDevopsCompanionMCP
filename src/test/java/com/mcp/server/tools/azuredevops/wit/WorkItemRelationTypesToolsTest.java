package com.mcp.server.tools.azuredevops.wit;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

public class WorkItemRelationTypesToolsTest {

    @Test
    void listDefinition() {
        var tool = new WorkItemRelationTypesListTool(null);
        assertEquals("azuredevops_wit_work_item_relation_types_list", tool.getName());
        var schema = tool.getInputSchema();
        assertTrue(schema.containsKey("properties"));
    }

    @Test
    void getDefinition() {
        var tool = new WorkItemRelationTypesGetTool(null);
        assertEquals("azuredevops_wit_work_item_relation_types_get", tool.getName());
        var schema = tool.getInputSchema();
        @SuppressWarnings("unchecked") var required = (java.util.List<String>) schema.get("required");
        assertTrue(required.contains("reference"));
    }

    @Test
    void validationReferenceRequired() {
        var tool = new WorkItemRelationTypesGetTool(null);
        var resp = tool.execute(Map.of("project","dummy"));
        assertTrue(Boolean.TRUE.equals(resp.get("isError")));
    }
}
