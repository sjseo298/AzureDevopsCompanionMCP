package com.mcp.server.tools.azuredevops.wit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FieldsGlobalGetToolTest {

    @Test
    void testDefinition() {
        FieldsGlobalGetTool tool = new FieldsGlobalGetTool(null);
        var def = tool.getToolDefinition();
        assertEquals("azuredevops_wit_fields_global_get", def.getName());
        assertNotNull(def.getDescription());
        assertEquals("object", def.getInputSchema().getType());
        assertTrue(def.getInputSchema().getProperties().containsKey("field"));
    }
}
