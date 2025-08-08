package com.mcp.server.tools.azuredevops.wit;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AccountMyWorkRecentActivityToolTest {

    @Test
    void testDefinition() {
        AccountMyWorkRecentActivityTool tool = new AccountMyWorkRecentActivityTool(null);
        var def = tool.getToolDefinition();
        assertEquals("azuredevops_wit_get_account_my_work_recent_activity", def.getName());
        assertNotNull(def.getDescription());
        assertNotNull(def.getInputSchema());
    }

    @Test
    void testExecuteWithoutService() {
        AccountMyWorkRecentActivityTool tool = new AccountMyWorkRecentActivityTool(null);
        Map<String,Object> resp = tool.execute(Map.of());
        assertTrue(Boolean.TRUE.equals(resp.get("isError")));
    }
}
