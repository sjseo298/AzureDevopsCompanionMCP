package com.mcp.server.tools.azuredevops.router;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GitApiToolTest {

    public void testItemsListCompatibilityAddsScopePathSlash() {
        try {
            var tool = new GitApiTool(null);
            Method method = GitApiTool.class.getDeclaredMethod("applyItemsListCompatibility", Map.class, List.class);
            method.setAccessible(true);

            Map<String, String> query = new LinkedHashMap<>();
            query.put("recursionLevel", "full");
            List<String> warnings = new ArrayList<>();

            method.invoke(tool, query, warnings);

            assert "/".equals(query.get("scopePath")) : "Debe autocompletar scopePath='/'";
            assert !warnings.isEmpty() : "Debe incluir warning de auto-scopePath";
            System.out.println("✓ testItemsListCompatibilityAddsScopePathSlash passed");
        } catch (Exception e) {
            System.err.println("✗ testItemsListCompatibilityAddsScopePathSlash failed: " + e.getMessage());
        }
    }

    public void testScopePathRequiredErrorDetection() {
        try {
            var tool = new GitApiTool(null);
            Method method = GitApiTool.class.getDeclaredMethod("isScopePathRequiredError", Map.class, String.class);
            method.setAccessible(true);

            Map<String, Object> resp = Map.of("message", "A valid scopePath is required for this request");
            boolean detected = (Boolean) method.invoke(tool, resp, "Error remoto (HTTP 400)");
            assert detected : "Debe detectar error de scopePath requerido";

            boolean notDetected = (Boolean) method.invoke(tool, Map.of("message", "Some unrelated error"), "Error remoto (HTTP 404)");
            assert !notDetected : "No debe detectar falsos positivos";

            System.out.println("✓ testScopePathRequiredErrorDetection passed");
        } catch (Exception e) {
            System.err.println("✗ testScopePathRequiredErrorDetection failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        GitApiToolTest test = new GitApiToolTest();
        test.testItemsListCompatibilityAddsScopePathSlash();
        test.testScopePathRequiredErrorDetection();
    }
}
