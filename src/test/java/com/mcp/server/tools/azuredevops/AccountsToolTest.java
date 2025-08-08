package com.mcp.server.tools.azuredevops;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AccountsToolTest {

    @Test
    void testGetToolDefinition() {
        AccountsTool tool = new AccountsTool(null);
        var def = tool.getToolDefinition();
        assertNotNull(def);
        assertEquals("azuredevops_accounts_get_accounts", def.getName());
        assertNotNull(def.getDescription());
        assertNotNull(def.getInputSchema());
    }

    @Test
    void testInputSchemaStructure() {
        AccountsTool tool = new AccountsTool(null);
        Map<String,Object> schema = tool.getInputSchema();
        assertEquals("object", schema.get("type"));
        @SuppressWarnings("unchecked")
        Map<String,Object> props = (Map<String, Object>) schema.get("properties");
        assertTrue(props.containsKey("accountId"));
        assertTrue(props.containsKey("ownerId"));
        assertTrue(props.containsKey("memberId"));
        assertTrue(props.containsKey("properties"));
    }

    @Test
    void testExecuteWithoutService() {
        AccountsTool tool = new AccountsTool(null);
        Map<String,Object> resp = tool.execute(Map.of());
        assertTrue(Boolean.TRUE.equals(resp.get("isError")));
    }
}
