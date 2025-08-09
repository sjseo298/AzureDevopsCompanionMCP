package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class WiqlToolsTest {

    @Test
    void schemaWiqlByQuery() {
        AbstractAzureDevOpsTool tool = new WiqlByQueryTool(null);
        assertEquals("azuredevops_wit_wiql_by_query", tool.getName());
        Map<String,Object> schema = tool.getInputSchema();
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String,Object>) schema.get("properties");
        assertTrue(props.containsKey("project"));
        assertTrue(props.containsKey("wiql"));
        @SuppressWarnings("unchecked") var req = (java.util.List<String>) schema.get("required");
        assertTrue(req.contains("project"));
        assertTrue(req.contains("wiql"));
    }

    @Test
    void schemaWiqlById() {
        AbstractAzureDevOpsTool tool = new WiqlByIdTool(null);
        assertEquals("azuredevops_wit_wiql_by_id", tool.getName());
        Map<String,Object> schema = tool.getInputSchema();
        @SuppressWarnings("unchecked") Map<String,Object> props = (Map<String,Object>) schema.get("properties");
        assertTrue(props.containsKey("project"));
        assertTrue(props.containsKey("id"));
        @SuppressWarnings("unchecked") var req = (java.util.List<String>) schema.get("required");
        assertTrue(req.contains("project"));
        assertTrue(req.contains("id"));
    }

    @Test
    void validationProjectRequired() {
        AbstractAzureDevOpsTool tool = new WiqlByQueryTool(null);
        Map<String,Object> resp = tool.execute(Map.of("wiql","SELECT 1"));
        assertTrue((Boolean) resp.get("isError"));
        assertTrue(resp.get("error").toString().contains("project"));
    }
}
