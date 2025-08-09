package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

public class RecycleBinToolsTest {

    private AzureDevOpsClientService dummyService = null; // null permitido para tests de validación de schema

    @Test
    void definitions() {
        var get = new RecycleBinGetTool(dummyService);
        assertEquals("azuredevops_wit_recyclebin_get", get.getName());
        assertTrue(get.getDescription().contains("Recycle Bin"));

        var list = new RecycleBinListTool(dummyService);
        assertEquals("azuredevops_wit_recyclebin_list", list.getName());

        var batch = new RecycleBinGetBatchTool(dummyService);
        assertEquals("azuredevops_wit_recyclebin_get_batch", batch.getName());

        var restore = new RecycleBinRestoreTool(dummyService);
        assertEquals("azuredevops_wit_recyclebin_restore", restore.getName());

        var destroy = new RecycleBinDestroyTool(dummyService);
        assertEquals("azuredevops_wit_recyclebin_destroy", destroy.getName());
    }

    @Test
    void schemaRequiresProject() {
        var tool = new RecycleBinGetTool(dummyService);
        var schema = tool.getInputSchema();
        @SuppressWarnings("unchecked") var required = (java.util.List<String>) schema.get("required");
        assertTrue(required.contains("project"));
        assertTrue(required.contains("id"));
    }

    @Test
    void validationMissingId() {
        var tool = new RecycleBinGetTool(dummyService);
        var res = tool.execute(Map.of("project","X"));
        assertTrue(Boolean.TRUE.equals(res.get("isError")));
    }
}
