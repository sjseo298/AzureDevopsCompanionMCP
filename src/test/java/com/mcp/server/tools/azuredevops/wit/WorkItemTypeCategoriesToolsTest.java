package com.mcp.server.tools.azuredevops.wit;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

public class WorkItemTypeCategoriesToolsTest {

    @Test
    void listDefinition() {
        var tool = new WorkItemTypeCategoriesListTool(null);
        assertEquals("azuredevops_wit_work_item_type_categories_list", tool.getName());
        var schema = tool.getInputSchema();
        @SuppressWarnings("unchecked") var required = (java.util.List<String>) schema.get("required");
        assertTrue(required.contains("project"));
    }

    @Test
    void getDefinition() {
        var tool = new WorkItemTypeCategoriesGetTool(null);
        assertEquals("azuredevops_wit_work_item_type_categories_get", tool.getName());
        var schema = tool.getInputSchema();
        @SuppressWarnings("unchecked") var required = (java.util.List<String>) schema.get("required");
        assertTrue(required.contains("category"));
    }

    @Test
    void validationCategoryRequired() {
        var tool = new WorkItemTypeCategoriesGetTool(null);
        var resp = tool.execute(Map.of("project","p"));
        assertTrue(Boolean.TRUE.equals(resp.get("isError")));
    }
}
