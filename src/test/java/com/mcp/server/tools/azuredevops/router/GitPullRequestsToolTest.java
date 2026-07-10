package com.mcp.server.tools.azuredevops.router;

import java.lang.reflect.Method;
import java.util.Map;

public class GitPullRequestsToolTest {

    private String invokeApiVersion(GitPullRequestsTool tool, Map<String, Object> args) throws Exception {
        Method method = GitPullRequestsTool.class.getDeclaredMethod("apiVersion", Map.class);
        method.setAccessible(true);
        return (String) method.invoke(tool, args);
    }

    public void testDefaultApiVersionForPrimaryEndpoints() {
        try {
            var tool = new GitPullRequestsTool(null);
            String v = invokeApiVersion(tool, Map.of("operation", "get"));
            assert "7.2-preview.2".equals(v) : "'get' debe usar 7.2-preview.2 por defecto";
            System.out.println("✓ testDefaultApiVersionForPrimaryEndpoints passed");
        } catch (Exception e) {
            System.err.println("✗ testDefaultApiVersionForPrimaryEndpoints failed: " + e.getMessage());
        }
    }

    public void testDefaultApiVersionForLegacyEndpoints() {
        try {
            var tool = new GitPullRequestsTool(null);
            String vThreads = invokeApiVersion(tool, Map.of("operation", "threads_list"));
            assert "7.2-preview.1".equals(vThreads) : "'threads_list' debe usar 7.2-preview.1 por defecto";

            String vWorkItems = invokeApiVersion(tool, Map.of("operation", "work_items_list"));
            assert "7.2-preview.1".equals(vWorkItems) : "'work_items_list' debe usar 7.2-preview.1 por defecto";

            System.out.println("✓ testDefaultApiVersionForLegacyEndpoints passed");
        } catch (Exception e) {
            System.err.println("✗ testDefaultApiVersionForLegacyEndpoints failed: " + e.getMessage());
        }
    }

    public void testVersionMappingIncludesDestructiveEndpoints() {
        try {
            var tool = new GitPullRequestsTool(null);

            String[] primary = {"create", "update", "status_add"};
            for (String op : primary) {
                String v = invokeApiVersion(tool, Map.of("operation", op));
                assert "7.2-preview.2".equals(v) : "'" + op + "' debe usar 7.2-preview.2 por defecto";
            }

            String[] legacy = {
                    "reviewer_add", "reviewer_update",
                    "thread_create", "thread_update",
                    "comments_add", "comment_update", "comment_delete",
                    "label_add", "label_delete",
                    "query", "share"
            };
            for (String op : legacy) {
                String v = invokeApiVersion(tool, Map.of("operation", op));
                assert "7.2-preview.1".equals(v) : "'" + op + "' debe usar 7.2-preview.1 por defecto";
            }

            System.out.println("✓ testVersionMappingIncludesDestructiveEndpoints passed");
        } catch (Exception e) {
            System.err.println("✗ testVersionMappingIncludesDestructiveEndpoints failed: " + e.getMessage());
        }
    }

    public void testApiVersionOverrideWins() {
        try {
            var tool = new GitPullRequestsTool(null);
            String v = invokeApiVersion(tool, Map.of("operation", "threads_list", "apiVersion", "7.1"));
            assert "7.1".equals(v) : "apiVersion explícito debe tener prioridad";
            System.out.println("✓ testApiVersionOverrideWins passed");
        } catch (Exception e) {
            System.err.println("✗ testApiVersionOverrideWins failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        GitPullRequestsToolTest test = new GitPullRequestsToolTest();
        test.testDefaultApiVersionForPrimaryEndpoints();
        test.testDefaultApiVersionForLegacyEndpoints();
        test.testVersionMappingIncludesDestructiveEndpoints();
        test.testApiVersionOverrideWins();
    }
}
