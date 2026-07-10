package com.mcp.server.tools.azuredevops.router;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
public class GitLocalTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_git_local";
    private static final String DESC = "Operaciones Git local en workspace gestionado. operation: workspace_info|workspace_list|clone_or_sync|status|checkout|create_branch|log|commit|push.";
    private static final String DEFAULT_WORKSPACE_ROOT = "/tmp/mcp-git";
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;
    private static final String META_FILE = ".mcp-repo.json";

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    public GitLocalTool(AzureDevOpsClientService svc) {
        super(svc);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESC;
    }

    @Override
    protected boolean isProjectRequired() {
        return false;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> base = new LinkedHashMap<>(createBaseSchema());
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) base.get("properties");

        props.put("operation", Map.of(
                "type", "string",
                "enum", List.of("workspace_info", "workspace_list", "clone_or_sync", "status", "checkout", "create_branch", "log", "commit", "push"),
                "description", "Operación a ejecutar"
        ));
        props.put("repository", Map.of("type", "string", "description", "Nombre del repositorio (clone_or_sync y resolución de repo local)"));
        props.put("repositoryId", Map.of("type", "string", "description", "GUID del repositorio (preferido para resolución exacta)"));
        props.put("workspacePath", Map.of("type", "string", "description", "Ruta del repo dentro de MCP_GIT_WORKSPACE_ROOT (o absoluta dentro de ese root)."));
        props.put("branch", Map.of("type", "string", "description", "Rama destino para clone_or_sync/checkout/push"));
        props.put("depth", Map.of("type", "integer", "description", "Profundidad de clone (opcional)"));
        props.put("sparsePaths", Map.of("type", "string", "description", "Lista CSV de rutas para sparse checkout (clone_or_sync)"));
        props.put("pull", Map.of("type", "boolean", "description", "En sync, además de fetch hace pull --ff-only de la rama indicada"));
        props.put("create", Map.of("type", "boolean", "description", "checkout: si true crea rama nueva (-b)"));
        props.put("startPoint", Map.of("type", "string", "description", "checkout/create_branch: rama o commit base opcional"));
        props.put("top", Map.of("type", "integer", "description", "Límite para log"));
        props.put("message", Map.of("type", "string", "description", "Mensaje de commit"));
        props.put("all", Map.of("type", "boolean", "description", "commit: ejecutar git add -A antes de commit"));
        props.put("paths", Map.of("type", "string", "description", "commit: rutas CSV para git add (si all=false)"));
        props.put("setUpstream", Map.of("type", "boolean", "description", "push: usar -u origin <branch>"));
        props.put("timeoutSeconds", Map.of("type", "integer", "description", "Timeout para comandos git (default 60)"));
        props.put("raw", Map.of("type", "boolean", "description", "Devuelve JSON crudo"));

        base.put("required", List.of("operation"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        String op = stringArg(arguments, "operation");
        if (op.isBlank()) return error("'operation' es requerido");

        try {
            return switch (op) {
                case "workspace_info" -> executeWorkspaceInfo(arguments);
                case "workspace_list" -> executeWorkspaceList(arguments);
                case "clone_or_sync" -> executeCloneOrSync(arguments);
                case "status" -> executeStatus(arguments);
                case "checkout" -> executeCheckout(arguments);
                case "create_branch" -> executeCreateBranch(arguments);
                case "log" -> executeLog(arguments);
                case "commit" -> executeCommit(arguments);
                case "push" -> executePush(arguments);
                default -> error("Operación no soportada: " + op);
            };
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        } catch (Exception e) {
            return error("Error en operación git local: " + e.getMessage());
        }
    }

    private Map<String, Object> executeWorkspaceInfo(Map<String, Object> arguments) throws Exception {
        Path root = ensureWorkspaceRoot();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workspaceRoot", root.toString());
        result.put("organization", resolveOrganization());
        result.put("exists", Files.exists(root));
        result.put("mcpGitPersistent", parseBooleanFlexible(System.getenv("MCP_GIT_PERSISTENT")));
        result.put("managedStructure", "{workspaceRoot}/{organization}/{project}/{repository}");
        result.put("metadataFile", META_FILE);
        result.put("note", "Si se monta volumen en MCP_GIT_WORKSPACE_ROOT, los repos persisten entre reinicios del contenedor.");
        return finish(arguments, result);
    }

    private Map<String, Object> executeWorkspaceList(Map<String, Object> arguments) throws Exception {
        Path root = ensureWorkspaceRoot();
        List<Map<String, Object>> repos = readAllMetadata(root);
        repos.sort(Comparator.comparing(m -> Objects.toString(m.getOrDefault("organization", ""))
                + "/" + Objects.toString(m.getOrDefault("project", ""))
                + "/" + Objects.toString(m.getOrDefault("repository", ""))));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workspaceRoot", root.toString());
        result.put("count", repos.size());
        result.put("repositories", repos);
        return finish(arguments, result);
    }

    private Map<String, Object> executeCloneOrSync(Map<String, Object> arguments) throws Exception {
        String project = stringArg(arguments, "project");
        if (project.isBlank()) throw new IllegalArgumentException("'project' es requerido para clone_or_sync");

        String repositoryId = stringArg(arguments, "repositoryId");
        String repositoryNameArg = stringArg(arguments, "repository");
        if (repositoryId.isBlank() && repositoryNameArg.isBlank()) {
            throw new IllegalArgumentException("Debe indicar 'repositoryId' o 'repository' para clone_or_sync");
        }

        Path root = ensureWorkspaceRoot();

        Map<String, Object> repoData = fetchRepository(project, repositoryId, repositoryNameArg);
        if (Boolean.TRUE.equals(repoData.get("isHttpError"))) {
            String remoteErr = tryFormatRemoteError(repoData);
            throw new IllegalArgumentException(remoteErr != null ? remoteErr : "No se pudo obtener el repositorio remoto");
        }

        String repositoryName = Objects.toString(repoData.get("name"), repositoryNameArg);
        String remoteUrl = Objects.toString(repoData.get("remoteUrl"), null);
        String resolvedRepoId = Objects.toString(repoData.get("id"), repositoryId);
        String defaultBranch = Objects.toString(repoData.get("defaultBranch"), "refs/heads/main");
        if (remoteUrl == null || remoteUrl.isBlank()) {
            throw new IllegalArgumentException("El repositorio no trae remoteUrl en la respuesta");
        }

        Path repoPath = resolveTargetRepoPath(root, resolveOrganization(), project, repositoryName, resolvedRepoId);
        int timeoutSec = intArg(arguments, "timeoutSeconds", DEFAULT_TIMEOUT_SECONDS);
        int depth = intArg(arguments, "depth", 0);
        boolean pull = parseBoolean(arguments.get("pull"));
        List<String> sparsePaths = parseCsv(arguments.get("sparsePaths"));

        String requestedBranch = normalizeBranchName(stringArg(arguments, "branch"));
        String fallbackBranch = normalizeBranchName(defaultBranch);
        String branchToUse = requestedBranch.isBlank() ? fallbackBranch : requestedBranch;

        String action;
        if (Files.exists(repoPath.resolve(".git"))) {
            action = "sync";
            runGit(repoPath, buildGitCommand(true, "fetch", "--prune", "origin"), timeoutSec, true);
            if (!branchToUse.isBlank()) {
                checkoutBranch(repoPath, branchToUse, timeoutSec);
                if (pull) {
                    runGit(repoPath, buildGitCommand(true, "pull", "--ff-only", "origin", branchToUse), timeoutSec, true);
                }
            }
        } else {
            action = "clone";
            Files.createDirectories(repoPath.getParent());
            if (!sparsePaths.isEmpty()) {
                List<String> cloneArgs = new ArrayList<>();
                cloneArgs.add("clone");
                cloneArgs.add("--filter=blob:none");
                cloneArgs.add("--no-checkout");
                if (depth > 0) {
                    cloneArgs.add("--depth");
                    cloneArgs.add(String.valueOf(depth));
                }
                if (!branchToUse.isBlank()) {
                    cloneArgs.add("--branch");
                    cloneArgs.add(branchToUse);
                }
                cloneArgs.add(remoteUrl);
                cloneArgs.add(repoPath.toString());
                runGit(root, buildGitCommand(true, cloneArgs.toArray(new String[0])), timeoutSec, true);
                runGit(repoPath, buildGitCommand(false, "sparse-checkout", "init", "--cone"), timeoutSec, true);
                List<String> sparseArgs = new ArrayList<>();
                sparseArgs.add("sparse-checkout");
                sparseArgs.add("set");
                sparseArgs.addAll(sparsePaths);
                runGit(repoPath, buildGitCommand(false, sparseArgs.toArray(new String[0])), timeoutSec, true);
                if (!branchToUse.isBlank()) {
                    checkoutBranch(repoPath, branchToUse, timeoutSec);
                }
            } else {
                List<String> cloneArgs = new ArrayList<>();
                cloneArgs.add("clone");
                if (depth > 0) {
                    cloneArgs.add("--depth");
                    cloneArgs.add(String.valueOf(depth));
                }
                if (!branchToUse.isBlank()) {
                    cloneArgs.add("--branch");
                    cloneArgs.add(branchToUse);
                }
                cloneArgs.add(remoteUrl);
                cloneArgs.add(repoPath.toString());
                runGit(root, buildGitCommand(true, cloneArgs.toArray(new String[0])), timeoutSec, true);
            }
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("organization", resolveOrganization());
        metadata.put("project", project);
        metadata.put("repository", repositoryName);
        metadata.put("repositoryId", resolvedRepoId);
        metadata.put("remoteUrl", remoteUrl);
        metadata.put("defaultBranch", defaultBranch);
        metadata.put("workspacePath", repoPath.toString());
        metadata.put("updatedAt", OffsetDateTime.now().toString());
        Map<String, Object> existing = readMetadata(repoPath);
        if (existing == null || existing.isEmpty()) {
            metadata.put("createdAt", OffsetDateTime.now().toString());
        } else {
            metadata.put("createdAt", existing.getOrDefault("createdAt", OffsetDateTime.now().toString()));
        }
        writeMetadata(repoPath, metadata);

        GitRunResult branchStatus = runGit(repoPath, buildGitCommand(false, "rev-parse", "--abbrev-ref", "HEAD"), timeoutSec, false);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", action);
        result.put("organization", resolveOrganization());
        result.put("project", project);
        result.put("repository", repositoryName);
        result.put("repositoryId", resolvedRepoId);
        result.put("workspacePath", repoPath.toString());
        result.put("remoteUrl", remoteUrl);
        result.put("defaultBranch", defaultBranch);
        result.put("currentBranch", branchStatus.success() ? branchStatus.outputTrimmed() : null);
        result.put("sparsePaths", sparsePaths);
        return finish(arguments, result);
    }

    private Map<String, Object> executeStatus(Map<String, Object> arguments) throws Exception {
        Path repo = resolveExistingRepo(arguments);
        int timeoutSec = intArg(arguments, "timeoutSeconds", DEFAULT_TIMEOUT_SECONDS);
        GitRunResult status = runGit(repo, buildGitCommand(false, "status", "--short", "--branch"), timeoutSec, true);
        GitRunResult remote = runGit(repo, buildGitCommand(false, "remote", "get-url", "origin"), timeoutSec, false);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workspacePath", repo.toString());
        result.put("status", status.outputTrimmed());
        result.put("origin", remote.success() ? remote.outputTrimmed() : null);
        return finish(arguments, result);
    }

    private Map<String, Object> executeCheckout(Map<String, Object> arguments) throws Exception {
        Path repo = resolveExistingRepo(arguments);
        String branch = normalizeBranchName(stringArg(arguments, "branch"));
        if (branch.isBlank()) throw new IllegalArgumentException("'branch' es requerido para checkout");
        boolean create = parseBoolean(arguments.get("create"));
        String startPoint = stringArg(arguments, "startPoint");
        int timeoutSec = intArg(arguments, "timeoutSeconds", DEFAULT_TIMEOUT_SECONDS);

        if (create) {
            if (startPoint.isBlank()) {
                runGit(repo, buildGitCommand(false, "checkout", "-b", branch), timeoutSec, true);
            } else {
                runGit(repo, buildGitCommand(false, "checkout", "-b", branch, startPoint), timeoutSec, true);
            }
        } else {
            checkoutBranch(repo, branch, timeoutSec);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workspacePath", repo.toString());
        result.put("branch", branch);
        result.put("created", create);
        return finish(arguments, result);
    }

    private Map<String, Object> executeCreateBranch(Map<String, Object> arguments) throws Exception {
        Path repo = resolveExistingRepo(arguments);
        String branch = normalizeBranchName(stringArg(arguments, "branch"));
        if (branch.isBlank()) throw new IllegalArgumentException("'branch' es requerido para create_branch");
        String startPoint = stringArg(arguments, "startPoint");
        int timeoutSec = intArg(arguments, "timeoutSeconds", DEFAULT_TIMEOUT_SECONDS);
        if (startPoint.isBlank()) {
            runGit(repo, buildGitCommand(false, "branch", branch), timeoutSec, true);
        } else {
            runGit(repo, buildGitCommand(false, "branch", branch, startPoint), timeoutSec, true);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workspacePath", repo.toString());
        result.put("branch", branch);
        result.put("startPoint", startPoint.isBlank() ? null : startPoint);
        return finish(arguments, result);
    }

    private Map<String, Object> executeLog(Map<String, Object> arguments) throws Exception {
        Path repo = resolveExistingRepo(arguments);
        int top = intArg(arguments, "top", 20);
        if (top <= 0) top = 20;
        int timeoutSec = intArg(arguments, "timeoutSeconds", DEFAULT_TIMEOUT_SECONDS);
        GitRunResult log = runGit(repo, buildGitCommand(false, "log", "--oneline", "--decorate", "-n", String.valueOf(top)), timeoutSec, true);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workspacePath", repo.toString());
        result.put("top", top);
        result.put("log", log.outputTrimmed());
        return finish(arguments, result);
    }

    private Map<String, Object> executeCommit(Map<String, Object> arguments) throws Exception {
        Path repo = resolveExistingRepo(arguments);
        String message = stringArg(arguments, "message");
        if (message.isBlank()) throw new IllegalArgumentException("'message' es requerido para commit");
        int timeoutSec = intArg(arguments, "timeoutSeconds", DEFAULT_TIMEOUT_SECONDS);
        boolean all = parseBoolean(arguments.get("all"));
        List<String> paths = parseCsv(arguments.get("paths"));

        if (all) {
            runGit(repo, buildGitCommand(false, "add", "-A"), timeoutSec, true);
        } else if (!paths.isEmpty()) {
            List<String> addArgs = new ArrayList<>();
            addArgs.add("add");
            addArgs.addAll(paths);
            runGit(repo, buildGitCommand(false, addArgs.toArray(new String[0])), timeoutSec, true);
        }

        GitRunResult commit = runGit(repo, buildGitCommand(false, "commit", "-m", message), timeoutSec, false);
        boolean nothingToCommit = !commit.success() && commit.outputLower().contains("nothing to commit");
        if (!commit.success() && !nothingToCommit) {
            throw new IllegalArgumentException("git commit falló: " + safeOutput(commit.outputTrimmed()));
        }

        GitRunResult head = runGit(repo, buildGitCommand(false, "rev-parse", "--short", "HEAD"), timeoutSec, false);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workspacePath", repo.toString());
        result.put("message", message);
        result.put("nothingToCommit", nothingToCommit);
        result.put("output", commit.outputTrimmed());
        result.put("head", head.success() ? head.outputTrimmed() : null);
        return finish(arguments, result);
    }

    private Map<String, Object> executePush(Map<String, Object> arguments) throws Exception {
        Path repo = resolveExistingRepo(arguments);
        int timeoutSec = intArg(arguments, "timeoutSeconds", DEFAULT_TIMEOUT_SECONDS);
        String branch = normalizeBranchName(stringArg(arguments, "branch"));
        if (branch.isBlank()) {
            GitRunResult current = runGit(repo, buildGitCommand(false, "rev-parse", "--abbrev-ref", "HEAD"), timeoutSec, true);
            branch = current.outputTrimmed();
        }
        boolean setUpstream = parseBoolean(arguments.get("setUpstream"));

        List<String> pushArgs = new ArrayList<>();
        pushArgs.add("push");
        if (setUpstream) pushArgs.add("-u");
        pushArgs.add("origin");
        pushArgs.add(branch);
        GitRunResult push = runGit(repo, buildGitCommand(true, pushArgs.toArray(new String[0])), timeoutSec, true);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workspacePath", repo.toString());
        result.put("branch", branch);
        result.put("setUpstream", setUpstream);
        result.put("output", push.outputTrimmed());
        return finish(arguments, result);
    }

    private Map<String, Object> finish(Map<String, Object> arguments, Map<String, Object> result) {
        if (parseBoolean(arguments.get("raw"))) return rawSuccess(result);
        return Map.of("isError", false, "result", result);
    }

    private Map<String, Object> fetchRepository(String project, String repositoryId, String repositoryName) {
        String key = !repositoryId.isBlank() ? repositoryId : repositoryName;
        return azureService.getGitApiWithQuery(project, "repositories/" + key, null, "7.2-preview.2");
    }

    private Path ensureWorkspaceRoot() throws Exception {
        String rootValue = System.getenv("MCP_GIT_WORKSPACE_ROOT");
        if (rootValue == null || rootValue.isBlank()) rootValue = DEFAULT_WORKSPACE_ROOT;
        Path root = Paths.get(rootValue).toAbsolutePath().normalize();
        Files.createDirectories(root);
        return root;
    }

    private Path resolveExistingRepo(Map<String, Object> arguments) throws Exception {
        Path root = ensureWorkspaceRoot();

        String workspacePathArg = stringArg(arguments, "workspacePath");
        if (!workspacePathArg.isBlank()) {
            Path p = resolveWorkspacePath(root, workspacePathArg);
            if (!Files.exists(p.resolve(".git"))) {
                throw new IllegalArgumentException("workspacePath no apunta a un repositorio git: " + p);
            }
            return p;
        }

        String repositoryId = stringArg(arguments, "repositoryId");
        if (!repositoryId.isBlank()) {
            Path byId = findRepoById(root, repositoryId);
            if (byId != null) return byId;
            throw new IllegalArgumentException("No se encontró repositorio local para repositoryId=" + repositoryId);
        }

        String project = stringArg(arguments, "project");
        String repository = stringArg(arguments, "repository");
        if (project.isBlank() || repository.isBlank()) {
            throw new IllegalArgumentException("Para resolver repo local indique workspacePath, repositoryId o bien project+repository");
        }

        Path candidate = buildStructuredPath(root, resolveOrganization(), project, repository);
        if (Files.exists(candidate.resolve(".git"))) return candidate;

        Path byNames = findRepoByProjectAndName(root, project, repository);
        if (byNames != null) return byNames;

        throw new IllegalArgumentException("No se encontró repositorio local para project=" + project + " repository=" + repository);
    }

    private Path resolveTargetRepoPath(Path root, String organization, String project, String repositoryName, String repositoryId) throws Exception {
        if (repositoryId != null && !repositoryId.isBlank()) {
            Path existingById = findRepoById(root, repositoryId);
            if (existingById != null) return existingById;
        }

        Path base = buildStructuredPath(root, organization, project, repositoryName);
        if (!Files.exists(base)) return base;

        Map<String, Object> existingMeta = readMetadata(base);
        String existingId = Objects.toString(existingMeta.get("repositoryId"), "");
        if (!existingId.isBlank() && repositoryId != null && !repositoryId.isBlank() && !existingId.equalsIgnoreCase(repositoryId)) {
            String suffix = repositoryId.substring(0, Math.min(8, repositoryId.length()));
            return base.resolveSibling(base.getFileName().toString() + "__" + suffix);
        }
        return base;
    }

    private Path buildStructuredPath(Path root, String organization, String project, String repository) {
        return root
                .resolve(sanitizeSegment(organization))
                .resolve(sanitizeSegment(project))
                .resolve(sanitizeSegment(repository))
                .normalize();
    }

    private Path resolveWorkspacePath(Path root, String workspacePathArg) {
        Path raw = Paths.get(workspacePathArg.trim());
        Path resolved = raw.isAbsolute() ? raw.normalize() : root.resolve(raw).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("workspacePath debe permanecer dentro de MCP_GIT_WORKSPACE_ROOT");
        }
        return resolved;
    }

    private Path findRepoById(Path root, String repositoryId) throws Exception {
        for (Map<String, Object> meta : readAllMetadata(root)) {
            String id = Objects.toString(meta.get("repositoryId"), "");
            if (id.equalsIgnoreCase(repositoryId)) {
                String path = Objects.toString(meta.get("workspacePath"), "");
                if (!path.isBlank()) {
                    Path p = Paths.get(path).normalize();
                    if (p.startsWith(root) && Files.exists(p.resolve(".git"))) return p;
                }
            }
        }
        return null;
    }

    private Path findRepoByProjectAndName(Path root, String project, String repository) throws Exception {
        for (Map<String, Object> meta : readAllMetadata(root)) {
            String p = Objects.toString(meta.get("project"), "");
            String r = Objects.toString(meta.get("repository"), "");
            if (p.equalsIgnoreCase(project) && r.equalsIgnoreCase(repository)) {
                String path = Objects.toString(meta.get("workspacePath"), "");
                if (!path.isBlank()) {
                    Path repoPath = Paths.get(path).normalize();
                    if (repoPath.startsWith(root) && Files.exists(repoPath.resolve(".git"))) return repoPath;
                }
            }
        }
        return null;
    }

    private List<Map<String, Object>> readAllMetadata(Path root) throws Exception {
        if (!Files.exists(root)) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        try (var stream = Files.walk(root, 5)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> META_FILE.equals(p.getFileName().toString()))
                    .toList();
            for (Path p : files) {
                try {
                    byte[] bytes = Files.readAllBytes(p);
                    Map<String, Object> meta = JSON.readValue(bytes, new TypeReference<Map<String, Object>>() {});
                    out.add(new LinkedHashMap<>(meta));
                } catch (Exception ignored) {
                    // Ignorar metadata corrupta sin romper la operación completa.
                }
            }
        }
        return out;
    }

    private Map<String, Object> readMetadata(Path repoPath) {
        try {
            Path p = repoPath.resolve(META_FILE);
            if (!Files.exists(p)) return Map.of();
            byte[] bytes = Files.readAllBytes(p);
            return JSON.readValue(bytes, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private void writeMetadata(Path repoPath, Map<String, Object> metadata) throws Exception {
        Files.createDirectories(repoPath);
        byte[] bytes = JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(metadata);
        Files.write(repoPath.resolve(META_FILE), bytes);
    }

    private void checkoutBranch(Path repo, String branch, int timeoutSec) throws Exception {
        GitRunResult checkout = runGit(repo, buildGitCommand(false, "checkout", branch), timeoutSec, false);
        if (checkout.success()) return;

        GitRunResult track = runGit(repo, buildGitCommand(false, "checkout", "-b", branch, "--track", "origin/" + branch), timeoutSec, false);
        if (track.success()) return;

        throw new IllegalArgumentException("No se pudo cambiar a rama '" + branch + "': " + safeOutput(checkout.outputTrimmed()));
    }

    private List<String> buildGitCommand(boolean withAuth, String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.add("git");
        if (withAuth) {
            String pat = System.getenv("AZURE_DEVOPS_PAT");
            if (pat != null && !pat.isBlank()) {
                String b64 = Base64.getEncoder().encodeToString((":" + pat).getBytes(StandardCharsets.UTF_8));
                cmd.add("-c");
                cmd.add("http.extraheader=Authorization: Basic " + b64);
            }
        }
        for (String arg : args) cmd.add(arg);
        return cmd;
    }

    private GitRunResult runGit(Path workdir, List<String> command, int timeoutSec, boolean failOnError) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workdir.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thread reader = new Thread(() -> {
            try (InputStream is = process.getInputStream()) {
                is.transferTo(out);
            } catch (Exception ignored) {
                // ignorar
            }
        });
        reader.start();

        boolean finished = process.waitFor(Math.max(1, timeoutSec), java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            reader.join(2000L);
            throw new IllegalArgumentException("Timeout ejecutando comando git");
        }
        reader.join(2000L);

        int code = process.exitValue();
        String output = out.toString(StandardCharsets.UTF_8);
        GitRunResult result = new GitRunResult(code, output);
        if (failOnError && !result.success()) {
            throw new IllegalArgumentException("Comando git falló: " + safeOutput(result.outputTrimmed()));
        }
        return result;
    }

    private String safeOutput(String output) {
        if (output == null) return "";
        String x = output.replaceAll("(?i)authorization:\\s*basic\\s+[a-zA-Z0-9+/=]+", "authorization: basic ***");
        String pat = System.getenv("AZURE_DEVOPS_PAT");
        if (pat != null && !pat.isBlank()) x = x.replace(pat, "***");
        if (x.length() > 2000) return x.substring(0, 2000) + "...";
        return x;
    }

    private String resolveOrganization() {
        String org = azureService.getOrganization();
        if (org == null || org.isBlank()) {
            org = System.getenv("AZURE_DEVOPS_ORGANIZATION");
        }
        if (org == null || org.isBlank()) {
            return "organization";
        }
        return org;
    }

    private String sanitizeSegment(String input) {
        String s = input == null ? "" : input.trim();
        if (s.isBlank()) return "unknown";
        s = s.replaceAll("[^a-zA-Z0-9._-]+", "-");
        s = s.replaceAll("-{2,}", "-");
        s = s.replaceAll("^[-.]+", "");
        s = s.replaceAll("[-.]+$", "");
        return s.isBlank() ? "unknown" : s;
    }

    private List<String> parseCsv(Object value) {
        String raw = value == null ? "" : value.toString().trim();
        if (raw.isBlank()) return List.of();
        String[] parts = raw.split(",");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String s = p.trim();
            if (!s.isBlank()) out.add(s);
        }
        return out;
    }

    private boolean parseBoolean(Object value) {
        if (value instanceof Boolean b) return b;
        if (value == null) return false;
        String s = value.toString().trim().toLowerCase(Locale.ROOT);
        return "true".equals(s) || "1".equals(s) || "yes".equals(s) || "si".equals(s) || "sí".equals(s);
    }

    private boolean parseBooleanFlexible(String value) {
        return parseBoolean(value);
    }

    private int intArg(Map<String, Object> args, String key, int defaultValue) {
        Object v = args.get(key);
        if (v == null) return defaultValue;
        try {
            return Integer.parseInt(v.toString().trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private String stringArg(Map<String, Object> args, String key) {
        Object v = args.get(key);
        return v == null ? "" : v.toString().trim();
    }

    private String normalizeBranchName(String branch) {
        String b = branch == null ? "" : branch.trim();
        if (b.startsWith("refs/heads/")) return b.substring("refs/heads/".length());
        return b;
    }

    private static final class GitRunResult {
        private final int exitCode;
        private final String output;

        private GitRunResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output == null ? "" : output;
        }

        private boolean success() {
            return exitCode == 0;
        }

        private String outputTrimmed() {
            return output.trim();
        }

        private String outputLower() {
            return output.toLowerCase(Locale.ROOT);
        }
    }
}
