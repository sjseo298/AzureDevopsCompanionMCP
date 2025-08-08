package com.mcp.server.tools.azuredevops.wit;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CommentsToolsTest {

    @Test
    void testCommentsListDefinition() {
        CommentsListTool tool = new CommentsListTool(null);
        var def = tool.getToolDefinition();
        assertEquals("azuredevops_wit_comments_list", def.getName());
        assertNotNull(def.getDescription());
        assertNotNull(def.getInputSchema());
    }

    @Test
    void testCommentsAddDefinition() {
        CommentsAddTool tool = new CommentsAddTool(null);
        var def = tool.getToolDefinition();
        assertEquals("azuredevops_wit_comments_add", def.getName());
        assertNotNull(def.getDescription());
        assertNotNull(def.getInputSchema());
    }

    @Test
    void testCommentsUpdateDefinition() {
        CommentsUpdateTool tool = new CommentsUpdateTool(null);
        var def = tool.getToolDefinition();
        assertEquals("azuredevops_wit_comments_update", def.getName());
        assertNotNull(def.getDescription());
        assertNotNull(def.getInputSchema());
    }

    @Test
    void testCommentsDeleteDefinition() {
        CommentsDeleteTool tool = new CommentsDeleteTool(null);
        var def = tool.getToolDefinition();
        assertEquals("azuredevops_wit_comments_delete", def.getName());
        assertNotNull(def.getDescription());
        assertNotNull(def.getInputSchema());
    }

    @Test
    void testCommentsReactionsListDefinition() {
        CommentsReactionsListTool tool = new CommentsReactionsListTool(null);
        var def = tool.getToolDefinition();
        assertEquals("azuredevops_wit_comments_reactions_list", def.getName());
        assertNotNull(def.getDescription());
        assertNotNull(def.getInputSchema());
    }

    @Test
    void testCommentsReactionsAddDefinition() {
        CommentsReactionsAddTool tool = new CommentsReactionsAddTool(null);
        var def = tool.getToolDefinition();
        assertEquals("azuredevops_wit_comments_reactions_add", def.getName());
        assertNotNull(def.getDescription());
        assertNotNull(def.getInputSchema());
    }

    @Test
    void testCommentsReactionsDeleteDefinition() {
        CommentsReactionsDeleteTool tool = new CommentsReactionsDeleteTool(null);
        var def = tool.getToolDefinition();
        assertEquals("azuredevops_wit_comments_reactions_delete", def.getName());
        assertNotNull(def.getDescription());
        assertNotNull(def.getInputSchema());
    }
}
