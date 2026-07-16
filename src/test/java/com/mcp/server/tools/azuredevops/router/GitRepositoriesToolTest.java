package com.mcp.server.tools.azuredevops.router;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class GitRepositoriesToolTest {

    public void testToolDefinition() {
        try {
            var tool = new GitRepositoriesTool(null);
            var def = tool.getToolDefinition();
            assert def != null : "ToolDefinition no puede ser null";
            assert "azuredevops_git_repositories".equals(def.getName()) : "Nombre incorrecto";
            assert def.getDescription() != null : "Descripción no puede ser null";
            assert def.getInputSchema() != null : "InputSchema no puede ser null";
            System.out.println("✓ testToolDefinition passed");
        } catch (Exception e) {
            System.err.println("✗ testToolDefinition failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public void testSchemaIncludesNewOperationsAndFilters() {
        try {
            var tool = new GitRepositoriesTool(null);
            var schema = tool.getInputSchema();
            assert "object".equals(schema.get("type")) : "Tipo de schema incorrecto";
            var props = (Map<String, Object>) schema.get("properties");
            assert props.containsKey("nameContains") : "Falta propiedad 'nameContains'";
            assert props.containsKey("nameSearch") : "Falta propiedad 'nameSearch'";

            var operation = (Map<String, Object>) props.get("operation");
            var values = (List<String>) operation.get("enum");
            assert values.contains("search") : "Falta operación 'search'";
            assert values.contains("find") : "Falta operación 'find'";
            assert values.contains("get_by_name") : "Falta operación 'get_by_name'";
            assert values.contains("items_list_recursive") : "Falta operación 'items_list_recursive'";
            assert values.contains("items_get_safe") : "Falta operación 'items_get_safe'";
            assert values.contains("items_read_window") : "Falta operación 'items_read_window'";
            assert values.contains("search_files") : "Falta operación 'search_files'";
            assert values.contains("search_content") : "Falta operación 'search_content'";
            assert values.contains("explore_repo") : "Falta operación 'explore_repo'";
            assert values.contains("repo_to_pipelines") : "Falta operación 'repo_to_pipelines'";
            assert values.contains("pipeline_to_repo") : "Falta operación 'pipeline_to_repo'";

            assert props.containsKey("filePattern") : "Falta propiedad 'filePattern'";
            assert props.containsKey("textPattern") : "Falta propiedad 'textPattern'";
            assert props.containsKey("maxFiles") : "Falta propiedad 'maxFiles'";
            assert props.containsKey("maxBytesPerFile") : "Falta propiedad 'maxBytesPerFile'";
            assert props.containsKey("maxResults") : "Falta propiedad 'maxResults'";
            assert props.containsKey("maxSnippetChars") : "Falta propiedad 'maxSnippetChars'";
            assert props.containsKey("scanBudgetMs") : "Falta propiedad 'scanBudgetMs'";
            assert props.containsKey("offset") : "Falta propiedad 'offset'";
            assert props.containsKey("limit") : "Falta propiedad 'limit'";
            assert props.containsKey("maxWaitMs") : "Falta propiedad 'maxWaitMs'";
            assert props.containsKey("pipelineId") : "Falta propiedad 'pipelineId'";
            System.out.println("✓ testSchemaIncludesNewOperationsAndFilters passed");
        } catch (Exception e) {
            System.err.println("✗ testSchemaIncludesNewOperationsAndFilters failed: " + e.getMessage());
        }
    }

    public void testRepoToPipelinesRequiresProject() {
        try {
            var tool = new GitRepositoriesTool(null);
            var resp = tool.execute(Map.of("operation", "repo_to_pipelines", "repositoryId", "abc"));
            assert Boolean.TRUE.equals(resp.get("isError")) : "Debe fallar repo_to_pipelines sin project";
            System.out.println("✓ testRepoToPipelinesRequiresProject passed");
        } catch (Exception e) {
            System.err.println("✗ testRepoToPipelinesRequiresProject failed: " + e.getMessage());
        }
    }

    public void testPipelineToRepoRequiresPipelineId() {
        try {
            var tool = new GitRepositoriesTool(null);
            var resp = tool.execute(Map.of("operation", "pipeline_to_repo", "project", "Demo"));
            assert Boolean.TRUE.equals(resp.get("isError")) : "Debe fallar pipeline_to_repo sin pipelineId";
            System.out.println("✓ testPipelineToRepoRequiresPipelineId passed");
        } catch (Exception e) {
            System.err.println("✗ testPipelineToRepoRequiresPipelineId failed: " + e.getMessage());
        }
    }

    public void testSearchWithoutPatternReturnsError() {
        try {
            var tool = new GitRepositoriesTool(null);
            var resp = tool.execute(Map.of("operation", "search"));
            assert Boolean.TRUE.equals(resp.get("isError")) : "Debería fallar search sin patrón";
            System.out.println("✓ testSearchWithoutPatternReturnsError passed");
        } catch (Exception e) {
            System.err.println("✗ testSearchWithoutPatternReturnsError failed: " + e.getMessage());
        }
    }

    private String invokeVersionMethod(GitRepositoriesTool tool, String methodName, Map<String, Object> args) throws Exception {
        Method method = GitRepositoriesTool.class.getDeclaredMethod(methodName, Map.class);
        method.setAccessible(true);
        return (String) method.invoke(tool, args);
    }

    public void testVersionDefaultsByFamily() {
        try {
            var tool = new GitRepositoriesTool(null);

            String api = invokeVersionMethod(tool, "apiVersion", Map.of());
            assert "7.2-preview.2".equals(api) : "apiVersion default debe ser 7.2-preview.2";

            String items = invokeVersionMethod(tool, "itemsApiVersion", Map.of());
            assert "7.2-preview.1".equals(items) : "itemsApiVersion default debe ser 7.2-preview.1";

            String pushes = invokeVersionMethod(tool, "pushesApiVersion", Map.of());
            assert "7.2-preview.3".equals(pushes) : "pushesApiVersion default debe ser 7.2-preview.3";

            String build = invokeVersionMethod(tool, "buildApiVersion", Map.of());
            assert "7.2-preview.7".equals(build) : "buildApiVersion default debe ser 7.2-preview.7";

            System.out.println("✓ testVersionDefaultsByFamily passed");
        } catch (Exception e) {
            System.err.println("✗ testVersionDefaultsByFamily failed: " + e.getMessage());
        }
    }

    public void testVersionOverrideWinsAcrossFamilies() {
        try {
            var tool = new GitRepositoriesTool(null);
            Map<String, Object> args = Map.of("apiVersion", "7.1");

            String api = invokeVersionMethod(tool, "apiVersion", args);
            String items = invokeVersionMethod(tool, "itemsApiVersion", args);
            String pushes = invokeVersionMethod(tool, "pushesApiVersion", args);
            String build = invokeVersionMethod(tool, "buildApiVersion", args);

            assert "7.1".equals(api) : "apiVersion override debe aplicar";
            assert "7.1".equals(items) : "itemsApiVersion override debe aplicar";
            assert "7.1".equals(pushes) : "pushesApiVersion override debe aplicar";
            assert "7.1".equals(build) : "buildApiVersion override debe aplicar";

            System.out.println("✓ testVersionOverrideWinsAcrossFamilies passed");
        } catch (Exception e) {
            System.err.println("✗ testVersionOverrideWinsAcrossFamilies failed: " + e.getMessage());
        }
    }

    public void testScopePathRequiredErrorDetection() {
        try {
            var tool = new GitRepositoriesTool(null);
            Method method = GitRepositoriesTool.class.getDeclaredMethod("isScopePathRequiredError", Map.class, String.class);
            method.setAccessible(true);

            Map<String, Object> resp = Map.of("message", "A valid scopePath is required for this request");
            boolean detected = (Boolean) method.invoke(tool, resp, "Error remoto (HTTP 400)");
            assert detected : "Debe detectar errores de scopePath requerido";

            boolean notDetected = (Boolean) method.invoke(tool, Map.of("message", "Some unrelated error"), "Error remoto (HTTP 404)");
            assert !notDetected : "No debe marcar errores no relacionados";

            System.out.println("✓ testScopePathRequiredErrorDetection passed");
        } catch (Exception e) {
            System.err.println("✗ testScopePathRequiredErrorDetection failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public void testItemsReadWindowRejectsBinaryMetadata() {
        try {
            var tool = new GitRepositoriesTool(null);
            Method method = GitRepositoriesTool.class.getDeclaredMethod("evaluateTextEligibility", Map.class);
            method.setAccessible(true);

            Map<String, Object> item = Map.of("contentMetadata", Map.of("contentType", "application/pdf"));
            Object result = method.invoke(tool, item);

            Method allowedMethod = result.getClass().getDeclaredMethod("allowed");
            Method reasonCodeMethod = result.getClass().getDeclaredMethod("reasonCode");
            allowedMethod.setAccessible(true);
            reasonCodeMethod.setAccessible(true);

            boolean allowed = (Boolean) allowedMethod.invoke(result);
            String code = (String) reasonCodeMethod.invoke(result);

            assert !allowed : "Content-Type binario debe rechazarse para lectura por líneas";
            assert "BINARY_NOT_SUPPORTED".equals(code) : "Código esperado BINARY_NOT_SUPPORTED";

            System.out.println("✓ testItemsReadWindowRejectsBinaryMetadata passed");
        } catch (Exception e) {
            System.err.println("✗ testItemsReadWindowRejectsBinaryMetadata failed: " + e.getMessage());
        }
    }

    public void testItemsReadWindowRequiresPath() {
        try {
            var tool = new GitRepositoriesTool(null);
            var resp = tool.execute(Map.of("operation", "items_read_window", "project", "Demo", "repositoryId", "r1"));
            assert Boolean.TRUE.equals(resp.get("isError")) : "Debe fallar items_read_window sin path";
            System.out.println("✓ testItemsReadWindowRequiresPath passed");
        } catch (Exception e) {
            System.err.println("✗ testItemsReadWindowRequiresPath failed: " + e.getMessage());
        }
    }

    public void testSearchRuntimeSettingsRaiseMaxResultsForPagination() {
        try {
            var tool = new GitRepositoriesTool(null);
            Method method = GitRepositoriesTool.class.getDeclaredMethod("resolveSearchRuntimeSettings", Map.class, int.class, Integer.class);
            method.setAccessible(true);

            Object settings = method.invoke(tool, Map.of("maxResults", "5"), 10, Integer.valueOf(10));

            Method maxResultsMethod = settings.getClass().getDeclaredMethod("maxResults");
            Method raisedForPaginationMethod = settings.getClass().getDeclaredMethod("raisedForPagination");
            maxResultsMethod.setAccessible(true);
            raisedForPaginationMethod.setAccessible(true);

            int maxResults = (Integer) maxResultsMethod.invoke(settings);
            boolean raised = (Boolean) raisedForPaginationMethod.invoke(settings);

            assert maxResults == 20 : "maxResults debe elevarse para cubrir skip+top";
            assert raised : "Debe marcar ajuste por paginación";

            System.out.println("✓ testSearchRuntimeSettingsRaiseMaxResultsForPagination passed");
        } catch (Exception e) {
            System.err.println("✗ testSearchRuntimeSettingsRaiseMaxResultsForPagination failed: " + e.getMessage());
        }
    }

    public void testSearchRuntimeSettingsClampHardLimits() {
        try {
            var tool = new GitRepositoriesTool(null);
            Method method = GitRepositoriesTool.class.getDeclaredMethod("resolveSearchRuntimeSettings", Map.class, int.class, Integer.class);
            method.setAccessible(true);

            Object settings = method.invoke(tool,
                    Map.of(
                            "maxResults", "999999",
                            "maxSnippetChars", "999999",
                            "scanBudgetMs", "999999"
                    ),
                    0,
                    Integer.valueOf(10));

            Method maxResultsMethod = settings.getClass().getDeclaredMethod("maxResults");
            Method maxSnippetMethod = settings.getClass().getDeclaredMethod("maxSnippetChars");
            Method scanBudgetMethod = settings.getClass().getDeclaredMethod("scanBudgetMs");
            maxResultsMethod.setAccessible(true);
            maxSnippetMethod.setAccessible(true);
            scanBudgetMethod.setAccessible(true);

            int maxResults = (Integer) maxResultsMethod.invoke(settings);
            int maxSnippetChars = (Integer) maxSnippetMethod.invoke(settings);
            long scanBudgetMs = (Long) scanBudgetMethod.invoke(settings);

            assert maxResults == 10000 : "maxResults debe respetar límite duro";
            assert maxSnippetChars == 8000 : "maxSnippetChars debe respetar límite duro";
            assert scanBudgetMs == 300000L : "scanBudgetMs debe respetar límite duro";

            System.out.println("✓ testSearchRuntimeSettingsClampHardLimits passed");
        } catch (Exception e) {
            System.err.println("✗ testSearchRuntimeSettingsClampHardLimits failed: " + e.getMessage());
        }
    }

    public void testSnippetHonorsRequestedMaxChars() {
        try {
            var tool = new GitRepositoriesTool(null);
            Method method = GitRepositoriesTool.class.getDeclaredMethod("snippet", String.class, int.class, int.class, int.class);
            method.setAccessible(true);

            String text = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".repeat(10);
            String snippet = (String) method.invoke(tool, text, 120, 130, 50);

            assert snippet.length() <= 50 : "Snippet debe respetar maxSnippetChars";

            System.out.println("✓ testSnippetHonorsRequestedMaxChars passed");
        } catch (Exception e) {
            System.err.println("✗ testSnippetHonorsRequestedMaxChars failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        GitRepositoriesToolTest test = new GitRepositoriesToolTest();
        test.testToolDefinition();
        test.testSchemaIncludesNewOperationsAndFilters();
        test.testSearchWithoutPatternReturnsError();
        test.testRepoToPipelinesRequiresProject();
        test.testPipelineToRepoRequiresPipelineId();
        test.testVersionDefaultsByFamily();
        test.testVersionOverrideWinsAcrossFamilies();
        test.testScopePathRequiredErrorDetection();
        test.testItemsReadWindowRejectsBinaryMetadata();
        test.testItemsReadWindowRequiresPath();
        test.testSearchRuntimeSettingsRaiseMaxResultsForPagination();
        test.testSearchRuntimeSettingsClampHardLimits();
        test.testSnippetHonorsRequestedMaxChars();
    }
}
