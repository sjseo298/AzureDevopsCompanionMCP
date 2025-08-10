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
        @SuppressWarnings("unchecked") var props = (java.util.Map<String,Object>) schema.get("properties");
        assertTrue(props.containsKey("requiredOnly"));
        assertTrue(props.containsKey("filterRef"));
        assertTrue(props.containsKey("filterName"));
        assertTrue(props.containsKey("summary"));
    assertTrue(props.containsKey("showPicklistItems"));
    assertTrue(props.containsKey("raw"));
    assertTrue(props.containsKey("apiVersion"));
    }

    @Test
    void getDefinition() {
        var tool = new WorkItemTypesFieldGetTool(null);
        assertEquals("azuredevops_wit_work_item_types_field_get", tool.getName());
        var schema = tool.getInputSchema();
        @SuppressWarnings("unchecked") var required = (java.util.List<String>) schema.get("required");
        assertTrue(required.contains("field"));
    @SuppressWarnings("unchecked") var props = (java.util.Map<String,Object>) schema.get("properties");
    assertTrue(props.containsKey("summary"));
    assertTrue(props.containsKey("showPicklistItems"));
    assertTrue(props.containsKey("noEnrich"));
    assertTrue(props.containsKey("raw"));
    assertTrue(props.containsKey("apiVersion"));
    }
    @Test
    void validationFieldRequired() {
        var tool = new WorkItemTypesFieldGetTool(null);
        var resp = tool.execute(Map.of("project","p","type","Bug"));
        assertTrue(Boolean.TRUE.equals(resp.get("isError")));
    }
}
