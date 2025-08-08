package com.mcp.server.tools.azuredevops.wit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArtifactLinkTypesToolTest {

    @Test
    void testDefinition() {
        ArtifactLinkTypesTool tool = new ArtifactLinkTypesTool(null);
        var def = tool.getToolDefinition();
        assertEquals("azuredevops_wit_get_artifact_link_types", def.getName());
        assertNotNull(def.getDescription());
        assertNotNull(def.getInputSchema());
    }
}
