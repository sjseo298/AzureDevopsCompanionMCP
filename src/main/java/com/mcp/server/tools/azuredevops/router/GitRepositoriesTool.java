package com.mcp.server.tools.azuredevops.router;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
public class GitRepositoriesTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_git_repositories";
    private static final String DESC = "Operaciones Git Repositories/Code (API-first, evita clone local). operation: list|search|find|get_by_name|get|create|update|delete|items_get|items_get_safe|items_read_window|items_list|items_list_recursive|items_batch|search_files|find_files|search_content|explore_repo|commits_list|refs_list|refs_update|pushes_list|pushes_get|pushes_create|download_zip|repo_to_pipelines|pipeline_to_repo.";
    private static final String DEFAULT_API_VERSION = "7.2-preview.2";
    private static final String DEFAULT_ITEMS_API_VERSION = "7.2-preview.1";
    private static final String DEFAULT_PUSHES_API_VERSION = "7.2-preview.3";
    private static final String DEFAULT_BUILD_API_VERSION = "7.2-preview.7";
    private static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper();
    private static final int DEFAULT_MAX_FILES = 200;
    private static final int DEFAULT_MAX_BYTES_PER_FILE = 262_144;
    private static final int RECOMMENDED_MAX_FILES = 200;
    private static final int RECOMMENDED_MAX_BYTES_PER_FILE = 262_144;
    private static final int HARD_MAX_FILES = 5_000;
    private static final int HARD_MAX_BYTES_PER_FILE = 2_097_152;
    private static final int DEFAULT_WINDOW_OFFSET = 1;
    private static final int DEFAULT_WINDOW_LIMIT = 200;
    private static final int HARD_WINDOW_LIMIT = 5_000;
    private static final long DEFAULT_WINDOW_MAX_WAIT_MS = 3_500L;
    private static final long HARD_WINDOW_MAX_WAIT_MS = 30_000L;
    private static final String WINDOW_STATUS_READY = "ready";
    private static final String WINDOW_STATUS_WARMING_UP = "warming_up";
    private static final String WINDOW_STATUS_REJECTED = "rejected";
    private static final String WINDOW_STATUS_FAILED = "failed";
    private static final String CODE_BINARY_NOT_SUPPORTED = "BINARY_NOT_SUPPORTED";
    private static final String CODE_CACHE_QUOTA_EXCEEDED = "CACHE_QUOTA_EXCEEDED";
    private static final String CODE_FILE_TOO_LARGE = "FILE_TOO_LARGE";
    private static final String CODE_PREPARING_TIMEOUT = "PREPARING_TIMEOUT";
    private static final String CODE_DOWNLOAD_FAILED = "DOWNLOAD_FAILED";
    private static final String CODE_TOO_MANY_IN_PROGRESS = "TOO_MANY_DOWNLOADS_IN_PROGRESS";
    private static final String CODE_DISK_SPACE_LOW = "DISK_SPACE_LOW";
    private static final int DEFAULT_RETRY_AFTER_SECONDS = 6;
    private static final WindowFileCache WINDOW_CACHE = new WindowFileCache();

    @Autowired
    public GitRepositoriesTool(AzureDevOpsClientService svc) {
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
                "enum", List.of(
                        "list", "search", "find", "get_by_name", "get", "create", "update", "delete",
                        "items_get", "items_get_safe", "items_read_window", "items_list", "items_list_recursive", "items_batch",
                        "search_files", "find_files", "search_content", "explore_repo",
                        "commits_list", "refs_list", "refs_update",
                        "pushes_list", "pushes_get", "pushes_create", "download_zip",
                        "repo_to_pipelines", "pipeline_to_repo"
                ),
                "description", "Operación a ejecutar"
        ));

        props.put("repositoryId", Map.of("type", "string", "description", "ID del repositorio"));
        props.put("repositoryName", Map.of("type", "string", "description", "Nombre del repositorio (get/get_by_name alternativo)"));
        props.put("nameContains", Map.of("type", "string", "description", "Filtro por nombre (contiene, case-insensitive) para list/search/find"));
        props.put("nameSearch", Map.of("type", "string", "description", "Alias de nameContains para list/search/find"));
        props.put("includeLinks", Map.of("type", "boolean", "description", "Incluir _links cuando aplique"));
        props.put("includeAllUrls", Map.of("type", "boolean", "description", "List repos: includeAllUrls"));
        props.put("includeHidden", Map.of("type", "boolean", "description", "List repos: includeHidden"));

        props.put("name", Map.of("type", "string", "description", "Nombre para create/update"));
        props.put("defaultBranch", Map.of("type", "string", "description", "Rama por defecto (refs/heads/main)"));
        props.put("isDisabled", Map.of("type", "boolean", "description", "Deshabilitar repositorio (update)"));

        props.put("path", Map.of("type", "string", "description", "Ruta de item (items_get/items_list/download_zip)"));
        props.put("sha1", Map.of("type", "string", "description", "SHA1 de árbol para fallback trees_get en items_list_recursive"));
        props.put("scopePath", Map.of("type", "string", "description", "Ruta base para listados"));
        props.put("recursionLevel", Map.of("type", "string", "description", "none|oneLevel|oneLevelPlusNestedEmptyFolders|full"));
        props.put("version", Map.of("type", "string", "description", "Version descriptor version (branch/tag/commit)"));
        props.put("versionType", Map.of("type", "string", "description", "branch|tag|commit"));
        props.put("versionOptions", Map.of("type", "string", "description", "none|previousChange|firstParent"));
        props.put("includeContent", Map.of("type", "boolean", "description", "items_get: incluir contenido"));
        props.put("includeContentMetadata", Map.of("type", "boolean", "description", "items_get/items_list"));
        props.put("latestProcessedChange", Map.of("type", "boolean", "description", "items_get/items_list"));
        props.put("download", Map.of("type", "boolean", "description", "items_get/items_list: download"));
        props.put("resolveLfs", Map.of("type", "boolean", "description", "items_get: resolveLfs"));
        props.put("sanitize", Map.of("type", "boolean", "description", "items_get: sanitize"));
        props.put("offset", Map.of("type", "integer", "description", "items_read_window: línea inicial (1-based)"));
        props.put("limit", Map.of("type", "integer", "description", "items_read_window: cantidad de líneas a devolver"));
        props.put("maxWaitMs", Map.of("type", "integer", "description", "items_read_window: espera máxima en ms para preparar cache local"));
        props.put("zipForUnix", Map.of("type", "boolean", "description", "items_list/download_zip: zipForUnix"));
        props.put("filePattern", Map.of("type", "string", "description", "search_files/search_content: patrón glob de ruta/nombre (ej: **/*.conf)"));
        props.put("pathRegex", Map.of("type", "string", "description", "search_files/search_content: regex sobre path"));
        props.put("extensions", Map.of("type", "string", "description", "search_files/search_content: extensiones CSV (ej: conf,yml,properties)"));
        props.put("textPattern", Map.of("type", "string", "description", "search_content: texto o regex a buscar"));
        props.put("regex", Map.of("type", "boolean", "description", "search_content: tratar textPattern como regex"));
        props.put("caseSensitive", Map.of("type", "boolean", "description", "search_content: búsqueda sensible a mayúsculas"));
        props.put("maxFiles", Map.of("type", "integer", "description", "search_content/explore_repo: máximo de archivos a escanear (default 200, configurable)"));
        props.put("maxBytesPerFile", Map.of("type", "integer", "description", "search_content/explore_repo: máximo bytes por archivo (default 262144, configurable)"));
        props.put("includeContentPreview", Map.of("type", "boolean", "description", "search_content/explore_repo: incluir preview de contenido en resultados"));
        props.put("recursive", Map.of("type", "boolean", "description", "items_list_recursive: forzar estrategia recursiva fallback trees_get"));

        props.put("itemDescriptors", Map.of("type", "string", "description", "items_batch: JSON array con item descriptors"));
        props.put("ids", Map.of("type", "string", "description", "IDs CSV para operaciones batch"));

        props.put("top", Map.of("type", "integer", "description", "Límite de resultados. En list/search/find/search_files/search_content/explore_repo aplica paginación local"));
        props.put("skip", Map.of("type", "integer", "description", "Offset/paginación. En list/search/find/search_files/search_content/explore_repo aplica paginación local"));
        props.put("fromDate", Map.of("type", "string", "description", "commits_list searchCriteria.fromDate"));
        props.put("toDate", Map.of("type", "string", "description", "commits_list searchCriteria.toDate"));
        props.put("author", Map.of("type", "string", "description", "commits_list searchCriteria.author"));
        props.put("itemPath", Map.of("type", "string", "description", "commits_list searchCriteria.itemPath"));
        props.put("branch", Map.of("type", "string", "description", "refs_list/pushes filters y pushes_create target ref"));

        props.put("oldObjectId", Map.of("type", "string", "description", "refs_update/pushes_create: oldObjectId"));
        props.put("newObjectId", Map.of("type", "string", "description", "refs_update: newObjectId"));
        props.put("isLocked", Map.of("type", "boolean", "description", "refs_update: bloqueo de ref"));

        props.put("pushId", Map.of("type", "integer", "description", "pushes_get: ID del push"));
        props.put("pipelineId", Map.of("type", "integer", "description", "pipeline_to_repo: ID del pipeline"));
        props.put("commitComment", Map.of("type", "string", "description", "pushes_create: comentario del commit"));
        props.put("changeType", Map.of("type", "string", "description", "pushes_create: add|edit|delete|rename"));
        props.put("sourcePath", Map.of("type", "string", "description", "pushes_create rename/move: ruta origen"));
        props.put("content", Map.of("type", "string", "description", "pushes_create: contenido texto (rawText)"));
        props.put("contentBase64", Map.of("type", "string", "description", "pushes_create: contenido base64Encoded"));
        props.put("contentType", Map.of("type", "string", "description", "pushes_create: rawText|base64Encoded"));
        props.put("changesJson", Map.of("type", "string", "description", "pushes_create: JSON array de changes completo"));
        props.put("commitsJson", Map.of("type", "string", "description", "pushes_create: JSON array de commits completo"));
        props.put("bodyJson", Map.of("type", "string", "description", "pushes_create: body completo opcional (máxima flexibilidad)"));
        props.put("base64", Map.of("type", "boolean", "description", "download_zip: incluir base64 en respuesta"));
        props.put("outputPath", Map.of("type", "string", "description", "download_zip: guardar zip local"));
        props.put("maxBase64Chars", Map.of("type", "integer", "description", "download_zip: truncar base64"));

        props.put("apiVersion", Map.of("type", "string", "description", "Override api-version"));
        props.put("raw", Map.of("type", "boolean", "description", "Devuelve JSON crudo"));

        base.put("required", List.of("operation"));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> arguments) {
        String op = str(arguments, "operation");
        if (op.isBlank()) return error("'operation' es requerido");

        try {
            return switch (op) {
                case "list" -> opList(arguments);
                case "search" -> opSearch(arguments);
                case "find" -> opFind(arguments);
                case "get_by_name" -> opGetByName(arguments);
                case "get" -> opGet(arguments);
                case "create" -> opCreate(arguments);
                case "update" -> opUpdate(arguments);
                case "delete" -> opDelete(arguments);
                case "items_get" -> opItemsGet(arguments);
                case "items_get_safe" -> opItemsGetSafe(arguments);
                case "items_read_window" -> opItemsReadWindow(arguments);
                case "items_list" -> opItemsList(arguments);
                case "items_list_recursive" -> opItemsListRecursive(arguments);
                case "items_batch" -> opItemsBatch(arguments);
                case "search_files" -> opSearchFiles(arguments);
                case "find_files" -> opFindFiles(arguments);
                case "search_content" -> opSearchContent(arguments);
                case "explore_repo" -> opExploreRepo(arguments);
                case "commits_list" -> opCommitsList(arguments);
                case "refs_list" -> opRefsList(arguments);
                case "refs_update" -> opRefsUpdate(arguments);
                case "pushes_list" -> opPushesList(arguments);
                case "pushes_get" -> opPushesGet(arguments);
                case "pushes_create" -> opPushesCreate(arguments);
                case "download_zip" -> opDownloadZip(arguments);
                case "repo_to_pipelines" -> opRepoToPipelines(arguments);
                case "pipeline_to_repo" -> opPipelineToRepo(arguments);
                default -> error("Operación no soportada: " + op);
            };
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        } catch (Exception e) {
            return error("Error ejecutando operación git repos: " + e.getMessage());
        }
    }

    private Map<String, Object> opList(Map<String, Object> args) {
        String project = str(args, "project");
        validateListLikeInputs(args, false);
        Map<String, String> q = baseQuery(args);
        putBool(q, "includeLinks", args.get("includeLinks"));
        putBool(q, "includeAllUrls", args.get("includeAllUrls"));
        putBool(q, "includeHidden", args.get("includeHidden"));
        Map<String, Object> resp = azureService.getGitApiWithQuery(projectOrNull(project), "repositories", q, apiVersion(args));
        String err = tryFormatRemoteError(resp);
        if (err != null) return error(err);

        Map<String, Object> result = buildRepositoriesListResult(args, resp, false, null, true);
        return doneResult(args, result);
    }

    private Map<String, Object> opSearch(Map<String, Object> args) {
        String project = str(args, "project");
        validateListLikeInputs(args, true);
        Map<String, String> q = baseQuery(args);
        putBool(q, "includeLinks", args.get("includeLinks"));
        putBool(q, "includeAllUrls", args.get("includeAllUrls"));
        putBool(q, "includeHidden", args.get("includeHidden"));
        Map<String, Object> resp = azureService.getGitApiWithQuery(projectOrNull(project), "repositories", q, apiVersion(args));
        String err = tryFormatRemoteError(resp);
        if (err != null) return error(err);

        Map<String, Object> result = buildRepositoriesListResult(args, resp, true, null, true);
        return doneResult(args, result);
    }

    private Map<String, Object> opFind(Map<String, Object> args) {
        return opSearch(args);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> opGetByName(Map<String, Object> args) {
        String exactName = exactRepositoryName(args);
        String project = str(args, "project");
        Map<String, String> q = baseQuery(args);
        putBool(q, "includeLinks", args.get("includeLinks"));
        putBool(q, "includeAllUrls", args.get("includeAllUrls"));
        putBool(q, "includeHidden", args.get("includeHidden"));

        Map<String, Object> resp = azureService.getGitApiWithQuery(projectOrNull(project), "repositories", q, apiVersion(args));
        String err = tryFormatRemoteError(resp);
        if (err != null) return error(err);

        Map<String, Object> lookup = buildRepositoriesListResult(args, resp, false, exactName, false);
        int total = asInt(lookup.get("totalCount"), 0);
        List<Map<String, Object>> matches = (List<Map<String, Object>>) lookup.getOrDefault("value", List.of());

        if (total <= 0 || matches.isEmpty()) {
            return error("No se encontró repositorio con nombre exacto: '" + exactName + "'");
        }

        if (total > 1) {
            List<Map<String, Object>> candidates = new ArrayList<>();
            for (Map<String, Object> repo : matches) {
                candidates.add(repositorySummary(repo));
            }
            String msg = "Nombre de repositorio ambiguo: '" + exactName + "' coincide en " + total + " repositorios. Use 'project' para acotar o 'repositoryId'.";

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("isError", true);
            out.put("error", msg);
            out.put("lookupName", exactName);
            out.put("count", total);
            out.put("candidates", candidates);
            out.put("content", List.of(Map.of("type", "text", "text", msg)));
            return out;
        }

        Map<String, Object> repository = matches.get(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("lookupName", exactName);
        result.put("projectScope", project.isBlank() ? "organization" : project);
        result.put("repository", repository);
        result.put("summary", repositorySummary(repository));
        return doneResult(args, result);
    }

    private Map<String, Object> opGet(Map<String, Object> args) {
        String project = str(args, "project");
        if (project.isBlank()) throw new IllegalArgumentException("'project' es requerido para get");
        String repo = repoKey(args);
        Map<String, String> q = baseQuery(args);
        putBool(q, "includeLinks", args.get("includeLinks"));
        Map<String, Object> resp = azureService.getGitApiWithQuery(project, "repositories/" + repo, q, apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opCreate(Map<String, Object> args) {
        String project = str(args, "project");
        if (project.isBlank()) throw new IllegalArgumentException("'project' es requerido para create");
        String name = str(args, "name");
        if (name.isBlank()) throw new IllegalArgumentException("'name' es requerido para create");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        Map<String, Object> projectObj = new LinkedHashMap<>();
        projectObj.put("id", project);
        body.put("project", projectObj);

        Map<String, Object> resp = azureService.postGitApiWithQuery(project, "repositories", baseQuery(args), body, apiVersion(args), MediaType.APPLICATION_JSON);
        return done(args, resp);
    }

    private Map<String, Object> opUpdate(Map<String, Object> args) {
        String project = str(args, "project");
        if (project.isBlank()) throw new IllegalArgumentException("'project' es requerido para update");
        String repo = repoKey(args);

        Map<String, Object> body = new LinkedHashMap<>();
        if (!str(args, "name").isBlank()) body.put("name", str(args, "name"));
        if (!str(args, "defaultBranch").isBlank()) body.put("defaultBranch", normalizeRef(str(args, "defaultBranch")));
        if (args.get("isDisabled") != null) body.put("isDisabled", parseBool(args.get("isDisabled")));
        if (body.isEmpty()) throw new IllegalArgumentException("Debe indicar al menos un campo para update (name/defaultBranch/isDisabled)");

        Map<String, Object> resp = azureService.patchGitApiWithQuery(project, "repositories/" + repo, baseQuery(args), body, apiVersion(args), MediaType.APPLICATION_JSON);
        return done(args, resp);
    }

    private Map<String, Object> opDelete(Map<String, Object> args) {
        String project = str(args, "project");
        if (project.isBlank()) throw new IllegalArgumentException("'project' es requerido para delete");
        String repo = repoKey(args);
        Map<String, Object> resp = azureService.deleteGitApiWithQuery(project, "repositories/" + repo, baseQuery(args), apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opItemsGet(Map<String, Object> args) {
        String project = requireProject(args, "items_get");
        String repo = resolveRepositoryId(project, args, "items_get");
        String path = str(args, "path");
        if (path.isBlank()) throw new IllegalArgumentException("'path' es requerido para items_get");

        Map<String, String> q = baseQuery(args);
        q.put("path", path);
        putBool(q, "download", args.get("download"));
        putBool(q, "includeContent", args.get("includeContent"));
        putBool(q, "includeContentMetadata", args.get("includeContentMetadata"));
        putBool(q, "latestProcessedChange", args.get("latestProcessedChange"));
        putBool(q, "resolveLfs", args.get("resolveLfs"));
        putBool(q, "sanitize", args.get("sanitize"));
        putIfNotBlank(q, "scopePath", str(args, "scopePath"));
        putVersionDescriptor(q, args);
        putIfNotBlank(q, "recursionLevel", str(args, "recursionLevel"));

        Map<String, Object> resp = azureService.getGitApiWithQuery(project, "repositories/" + repo + "/items", q, itemsApiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opItemsGetSafe(Map<String, Object> args) {
        String project = requireProject(args, "items_get_safe");
        String repo = resolveRepositoryId(project, args, "items_get_safe");
        String path = str(args, "path");
        if (path.isBlank()) throw new IllegalArgumentException("'path' es requerido para items_get_safe");

        Map<String, Object> safeArgs = new LinkedHashMap<>(args);
        safeArgs.put("repositoryId", repo);
        safeArgs.put("includeContent", true);

        Map<String, Object> first = opItemsGetRaw(safeArgs, project, repo);
        String err = tryFormatRemoteError(first);
        if (err != null) return error(err);

        String content = Objects.toString(first.get("content"), "");
        if (!content.isBlank()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("contentSource", "items_get");
            result.put("repositoryId", repo);
            result.put("path", path);
            result.put("item", first);
            result.put("content", content);
            return doneResult(args, result);
        }

        String objectId = extractObjectId(first);
        if (objectId.isBlank()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("contentSource", "items_get_metadata_only");
            result.put("repositoryId", repo);
            result.put("path", path);
            result.put("item", first);
            result.put("warning", "items_get no devolvió contenido y no se pudo resolver objectId para fallback blob");
            return doneResult(args, result);
        }

        Map<String, String> q = new LinkedHashMap<>();
        q.put("$format", "text");
        putBool(q, "resolveLfs", args.get("resolveLfs"));
        Map<String, Object> blobResp = azureService.exchangeGitApi(
                project,
                HttpMethod.GET,
                "repositories/" + repo + "/blobs/" + objectId,
                q,
                null,
                itemsApiVersion(args),
                null,
                MediaType.TEXT_PLAIN,
                false
        );

        String blobErr = tryFormatRemoteError(blobResp);
        if (blobErr != null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("contentSource", "items_get_metadata_only");
            result.put("repositoryId", repo);
            result.put("path", path);
            result.put("item", first);
            result.put("warning", "items_get sin contenido y fallback blob falló: " + blobErr);
            return doneResult(args, result);
        }

        String text = Objects.toString(blobResp.get("text"), "");
        if (text.isBlank()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("contentSource", "items_get_metadata_only");
            result.put("repositoryId", repo);
            result.put("path", path);
            result.put("item", first);
            result.put("warning", "fallback blob no devolvió texto");
            return doneResult(args, result);
        }

        Map<String, Object> item = new LinkedHashMap<>(first);
        item.put("content", text);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contentSource", "blob_fallback");
        result.put("repositoryId", repo);
        result.put("path", path);
        result.put("item", item);
        result.put("content", text);
        return doneResult(args, result);
    }

    private Map<String, Object> opItemsReadWindow(Map<String, Object> args) {
        String project = requireProject(args, "items_read_window");
        String repo = resolveRepositoryId(project, args, "items_read_window");
        String path = normalizePath(str(args, "path"));
        if (path.isBlank()) throw new IllegalArgumentException("'path' es requerido para items_read_window");

        WindowRequest req = resolveWindowRequest(args);

        Map<String, Object> itemArgs = new LinkedHashMap<>(args);
        itemArgs.put("repositoryId", repo);
        itemArgs.put("path", path);
        itemArgs.put("includeContent", false);
        itemArgs.put("includeContentMetadata", true);
        itemArgs.put("latestProcessedChange", false);
        Map<String, Object> itemResp = opItemsGetRaw(itemArgs, project, repo);
        String itemErr = tryFormatRemoteError(itemResp);
        if (itemErr != null) return error(itemErr);

        Map<String, Object> item = firstItem(itemResp);
        if (item.isEmpty()) {
            Map<String, Object> failed = buildWindowFailed(project, repo, path, req, "No se pudo obtener metadata del archivo", CODE_DOWNLOAD_FAILED, null);
            return doneResult(args, failed);
        }

        TextEligibility eligibility = evaluateTextEligibility(item);
        if (!eligibility.allowed()) {
            Map<String, Object> rejected = buildWindowRejected(project, repo, path, req, eligibility.reasonCode(), eligibility.reason(), eligibility.guidance());
            return doneResult(args, rejected);
        }

        String cacheKey = buildWindowCacheKey(project, repo, path, args);
        Map<String, String> downloadQuery = buildItemsReadWindowDownloadQuery(args, path);

        WindowFetchResult fetch = WINDOW_CACHE.fetch(
                cacheKey,
                path,
                req.offset(),
                req.limit(),
                req.maxWaitMs(),
                (targetPath, maxFileBytes) -> mapDownloadResult(
                        azureService.downloadGitTextToFile(
                                project,
                                "repositories/" + repo + "/items",
                                downloadQuery,
                                itemsApiVersion(args),
                                targetPath,
                                maxFileBytes
                        )
                )
        );

        if (fetch.status().equals(WINDOW_STATUS_READY)) {
            Map<String, Object> ready = new LinkedHashMap<>();
            ready.put("operation", "items_read_window");
            ready.put("status", WINDOW_STATUS_READY);
            ready.put("project", project);
            ready.put("repositoryId", repo);
            ready.put("path", path);
            ready.put("cacheKey", cacheKey);
            ready.put("source", "local_temp_cache");
            ready.put("offset", req.offset());
            ready.put("limit", req.limit());
            ready.put("lineEnd", fetch.lineEnd());
            ready.put("returnedLines", fetch.lines().size());
            ready.put("hasMore", fetch.hasMore());
            ready.put("totalLines", fetch.totalLines());
            ready.put("lines", fetch.lines());
            ready.put("cacheInfo", Map.of(
                    "cacheRoot", WINDOW_CACHE.rootDir().toString(),
                    "cacheBytes", fetch.cacheBytes(),
                    "fileBytes", fetch.fileBytes(),
                    "lastPreparedAt", fetch.lastPreparedAt().toString()
            ));
            return doneResult(args, ready);
        }

        if (fetch.status().equals(WINDOW_STATUS_WARMING_UP)) {
            Map<String, Object> warming = new LinkedHashMap<>();
            warming.put("operation", "items_read_window");
            warming.put("status", WINDOW_STATUS_WARMING_UP);
            warming.put("project", project);
            warming.put("repositoryId", repo);
            warming.put("path", path);
            warming.put("cacheKey", cacheKey);
            warming.put("offset", req.offset());
            warming.put("limit", req.limit());
            warming.put("errorCode", CODE_PREPARING_TIMEOUT);
            warming.put("message", "El archivo se está preparando en cache temporal porque es una lectura pesada. Espere unos segundos y vuelva a consultar.");
            warming.put("retryAfterSeconds", DEFAULT_RETRY_AFTER_SECONDS);
            warming.put("guidance", "Reintente la misma operación items_read_window con el mismo path/offset/limit.");
            if (fetch.downloadStartedAt() != null) {
                warming.put("download", Map.of(
                        "startedAt", fetch.downloadStartedAt().toString(),
                        "inProgress", true,
                        "elapsedMs", fetch.elapsedMs()
                ));
            }
            return doneResult(args, warming);
        }

        Map<String, Object> failed = buildWindowFailed(project, repo, path, req, fetch.message(), fetch.errorCode(), cacheKey);
        return doneResult(args, failed);
    }

    private WindowRequest resolveWindowRequest(Map<String, Object> args) {
        int offset = DEFAULT_WINDOW_OFFSET;
        if (args.containsKey("offset") && !str(args, "offset").isBlank()) {
            Integer v = parseInt(args.get("offset"));
            if (v == null || v < 1) throw new IllegalArgumentException("'offset' debe ser entero >= 1");
            offset = v;
        }

        int limit = DEFAULT_WINDOW_LIMIT;
        if (args.containsKey("limit") && !str(args, "limit").isBlank()) {
            Integer v = parseInt(args.get("limit"));
            if (v == null || v < 1) throw new IllegalArgumentException("'limit' debe ser entero >= 1");
            limit = Math.min(v, HARD_WINDOW_LIMIT);
        }

        long maxWaitMs = DEFAULT_WINDOW_MAX_WAIT_MS;
        if (args.containsKey("maxWaitMs") && !str(args, "maxWaitMs").isBlank()) {
            Integer v = parseInt(args.get("maxWaitMs"));
            if (v == null || v < 250) throw new IllegalArgumentException("'maxWaitMs' debe ser entero >= 250");
            maxWaitMs = Math.min(v.longValue(), HARD_WINDOW_MAX_WAIT_MS);
        }

        return new WindowRequest(offset, limit, maxWaitMs);
    }

    private Map<String, String> buildItemsReadWindowDownloadQuery(Map<String, Object> args, String path) {
        Map<String, String> q = new LinkedHashMap<>();
        q.put("path", path);
        q.put("$format", "text");
        putBool(q, "resolveLfs", args.get("resolveLfs"));
        putVersionDescriptor(q, args);
        return q;
    }

    private String buildWindowCacheKey(String project, String repo, String path, Map<String, Object> args) {
        String version = str(args, "version");
        String versionType = str(args, "versionType");
        String versionOptions = str(args, "versionOptions");
        return project + "|" + repo + "|" + normalizePath(path) + "|" + version + "|" + versionType + "|" + versionOptions;
    }

    private Map<String, Object> firstItem(Map<String, Object> response) {
        if (response == null || response.isEmpty()) return Map.of();
        if (response.containsKey("path")) return response;

        List<Map<String, Object>> list = extractItemsFromItemsResponse(response);
        if (list.isEmpty()) return Map.of();
        return list.get(0);
    }

    private TextEligibility evaluateTextEligibility(Map<String, Object> item) {
        Map<String, Object> metadata = toObjectMap(item.get("contentMetadata"));
        Object isBinaryObj = metadata.get("isBinary");
        if (isBinaryObj != null && parseBool(isBinaryObj)) {
            return new TextEligibility(false, CODE_BINARY_NOT_SUPPORTED,
                    "El archivo es binario y no puede leerse por líneas.",
                    "Use operaciones de descarga binaria si necesita ese contenido.");
        }

        String contentType = Objects.toString(metadata.get("contentType"), "").toLowerCase(Locale.ROOT);
        if (!contentType.isBlank() && isClearlyBinaryContentType(contentType)) {
            return new TextEligibility(false, CODE_BINARY_NOT_SUPPORTED,
                    "contentMetadata indica contenido binario ('" + contentType + "').",
                    "Solo se admiten archivos de texto en items_read_window.");
        }

        return new TextEligibility(true, "", "", "");
    }

    private boolean isClearlyBinaryContentType(String contentType) {
        String ct = contentType.toLowerCase(Locale.ROOT);
        if (ct.isBlank()) return false;
        if (ct.startsWith("text/")) return false;
        if (ct.contains("json") || ct.contains("xml") || ct.contains("yaml") || ct.contains("x-sh") || ct.contains("x-python")) {
            return false;
        }
        return ct.startsWith("image/")
                || ct.startsWith("audio/")
                || ct.startsWith("video/")
                || ct.startsWith("font/")
                || ct.contains("application/pdf")
                || ct.contains("application/zip")
                || ct.contains("application/x-zip")
                || ct.contains("application/gzip")
                || ct.contains("application/x-gzip")
                || ct.contains("application/java-archive")
                || ct.contains("application/x-rar")
                || ct.contains("application/x-7z")
                || ct.contains("application/msword")
                || ct.contains("application/vnd.ms-")
                || ct.contains("application/vnd.openxmlformats-officedocument")
                || ct.contains("application/x-executable");
    }

    private DownloadRunResult mapDownloadResult(Map<String, Object> response) {
        if (response == null) {
            return new DownloadRunResult(false, CODE_DOWNLOAD_FAILED, "Respuesta vacía al descargar archivo", 0L);
        }

        String errorRaw = Objects.toString(response.get("error"), "").trim();
        if (!errorRaw.isBlank()) {
            if ("MAX_BYTES_EXCEEDED".equalsIgnoreCase(errorRaw)) {
                long bytesRead = asLong(response.get("bytesRead"), -1L);
                long maxBytes = asLong(response.get("maxBytes"), -1L);
                String msg = "El archivo excede el límite máximo permitido para cache temporal";
                if (bytesRead > 0 && maxBytes > 0) {
                    msg += " (bytesRead=" + bytesRead + ", maxBytes=" + maxBytes + ")";
                }
                return new DownloadRunResult(false, CODE_FILE_TOO_LARGE, msg, Math.max(bytesRead, 0L));
            }
            return new DownloadRunResult(false, CODE_DOWNLOAD_FAILED, errorRaw, 0L);
        }

        String err = tryFormatRemoteError(response);
        if (err != null) {
            return new DownloadRunResult(false, CODE_DOWNLOAD_FAILED, err, 0L);
        }

        long bytes = asLong(response.get("bytesWritten"), 0L);
        return new DownloadRunResult(true, "", "", bytes);
    }

    private long asLong(Object value, long defaultValue) {
        if (value instanceof Number n) return n.longValue();
        if (value == null) return defaultValue;
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Map<String, Object> buildWindowRejected(String project,
                                                    String repo,
                                                    String path,
                                                    WindowRequest req,
                                                    String errorCode,
                                                    String reason,
                                                    String guidance) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("operation", "items_read_window");
        out.put("status", WINDOW_STATUS_REJECTED);
        out.put("project", project);
        out.put("repositoryId", repo);
        out.put("path", path);
        out.put("offset", req.offset());
        out.put("limit", req.limit());
        out.put("errorCode", errorCode);
        out.put("message", reason);
        out.put("guidance", guidance);
        return out;
    }

    private Map<String, Object> buildWindowFailed(String project,
                                                  String repo,
                                                  String path,
                                                  WindowRequest req,
                                                  String reason,
                                                  String errorCode,
                                                  String cacheKey) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("operation", "items_read_window");
        out.put("status", WINDOW_STATUS_FAILED);
        out.put("project", project);
        out.put("repositoryId", repo);
        out.put("path", path);
        out.put("offset", req.offset());
        out.put("limit", req.limit());
        if (cacheKey != null && !cacheKey.isBlank()) out.put("cacheKey", cacheKey);
        out.put("errorCode", errorCode == null || errorCode.isBlank() ? CODE_DOWNLOAD_FAILED : errorCode);
        out.put("message", reason == null || reason.isBlank() ? "No se pudo preparar el archivo para lectura por líneas" : reason);
        out.put("guidance", "Reduzca el alcance de la consulta o espere unos segundos y reintente.");
        return out;
    }

    private Map<String, Object> opItemsList(Map<String, Object> args) {
        String project = requireProject(args, "items_list");
        String repo = resolveRepositoryId(project, args, "items_list");
        String recursionLevel = str(args, "recursionLevel");
        String scopePathArg = str(args, "scopePath");
        String pathArg = str(args, "path");
        boolean missingScopeAndPath = scopePathArg.isBlank() && pathArg.isBlank();

        List<String> warnings = new ArrayList<>();
        Map<String, String> q = baseQuery(args);
        if (missingScopeAndPath && !recursionLevel.isBlank()) {
            q.put("scopePath", "/");
            warnings.add("items_list: 'scopePath/path' no informado con recursionLevel; se aplica scopePath='/' automáticamente.");
        } else {
            putIfNotBlank(q, "scopePath", scopePathArg);
            putIfNotBlank(q, "path", pathArg);
        }
        putIfNotBlank(q, "recursionLevel", recursionLevel);
        putBool(q, "includeContentMetadata", args.get("includeContentMetadata"));
        putBool(q, "latestProcessedChange", args.get("latestProcessedChange"));
        putBool(q, "download", args.get("download"));
        putBool(q, "zipForUnix", args.get("zipForUnix"));
        putVersionDescriptor(q, args);

        Map<String, Object> resp = azureService.getGitApiWithQuery(project, "repositories/" + repo + "/items", q, itemsApiVersion(args));
        String err = tryFormatRemoteError(resp);
        if (err != null && isScopePathRequiredError(resp, err)) {
            Map<String, Object> fallbackArgs = new LinkedHashMap<>(args);
            fallbackArgs.put("repositoryId", repo);
            if (missingScopeAndPath) fallbackArgs.put("scopePath", "/");
            fallbackArgs.put("_itemsListFallbackWarning", "items_list fallback automático a items_list_recursive por requerimiento de scopePath.");
            return opItemsListRecursive(fallbackArgs);
        }
        if (err != null) return error(err);

        if (!warnings.isEmpty()) {
            Map<String, Object> out = new LinkedHashMap<>(resp);
            out.put("warnings", warnings);
            return done(args, out);
        }
        return done(args, resp);
    }

    private Map<String, Object> opItemsListRecursive(Map<String, Object> args) {
        String project = requireProject(args, "items_list_recursive");
        String repo = resolveRepositoryId(project, args, "items_list_recursive");

        Map<String, Object> collected = collectRepositoryItems(project, repo, args);
        String err = tryFormatRemoteError(collected);
        if (err != null) return error(err);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) collected.getOrDefault("items", List.of());
        int skip = parseSkip(args.get("skip"));
        Integer top = parseTop(args.get("top"));

        int total = items.size();
        int from = Math.min(skip, total);
        int to = top == null ? total : Math.min(total, from + top);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("project", project);
        result.put("repositoryId", repo);
        result.put("strategy", collected.get("strategy"));
        result.put("strategyFallback", collected.get("strategyFallback"));
        result.put("count", to - from);
        result.put("totalCount", total);
        result.put("skip", skip);
        if (top != null) result.put("top", top);
        result.put("hasMore", to < total);
        result.put("value", new ArrayList<>(items.subList(from, to)));
        copyWarnings(collected, result);
        appendWarning(result, str(args, "_itemsListFallbackWarning"));
        return doneResult(args, result);
    }

    private Map<String, Object> opSearchFiles(Map<String, Object> args) {
        String project = requireProject(args, "search_files");
        String repo = resolveRepositoryId(project, args, "search_files");
        FileFilters filters = resolveFileFilters(args);

        Map<String, Object> collected = collectRepositoryItems(project, repo, args);
        String err = tryFormatRemoteError(collected);
        if (err != null) return error(err);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) collected.getOrDefault("items", List.of());
        List<Map<String, Object>> files = new ArrayList<>();
        for (Map<String, Object> item : items) {
            if (isDirectory(item)) continue;
            if (!matchesFileFilters(item, filters)) continue;
            files.add(fileResultRow(item));
        }

        return doneResult(args, paginateRows(args, project, repo, files, collected));
    }

    private Map<String, Object> opFindFiles(Map<String, Object> args) {
        return opSearchFiles(args);
    }

    private Map<String, Object> opSearchContent(Map<String, Object> args) {
        String project = requireProject(args, "search_content");
        String repo = resolveRepositoryId(project, args, "search_content");
        String textPattern = str(args, "textPattern");
        if (textPattern.isBlank()) throw new IllegalArgumentException("'textPattern' es requerido para search_content");

        FileFilters filters = resolveFileFilters(args);
        LimitSettings limits = resolveLimits(args);
        Pattern compiled = compileSearchPattern(textPattern, parseBool(args.get("regex")), parseBool(args.get("caseSensitive")));

        Map<String, Object> collected = collectRepositoryItems(project, repo, args);
        String err = tryFormatRemoteError(collected);
        if (err != null) return error(err);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) collected.getOrDefault("items", List.of());
        List<Map<String, Object>> candidateFiles = new ArrayList<>();
        for (Map<String, Object> item : items) {
            if (isDirectory(item)) continue;
            if (!matchesFileFilters(item, filters)) continue;
            candidateFiles.add(item);
        }

        List<Map<String, Object>> matches = new ArrayList<>();
        int scanned = 0;
        boolean stoppedByLimit = false;
        boolean includePreview = parseBool(args.get("includeContentPreview"));
        for (Map<String, Object> file : candidateFiles) {
            if (scanned >= limits.maxFiles()) {
                stoppedByLimit = true;
                break;
            }
            scanned++;

            String path = itemPath(file);
            ContentRead read = readFileText(project, repo, path, limits.maxBytesPerFile(), args);
            if (!read.readable()) continue;

            Matcher matcher = compiled.matcher(read.text());
            if (!matcher.find()) continue;

            Map<String, Object> row = fileResultRow(file);
            row.put("contentSource", read.source());
            row.put("contentTruncated", read.truncated());
            row.put("matchIndex", matcher.start());
            row.put("matchLength", matcher.end() - matcher.start());
            row.put("snippet", snippet(read.text(), matcher.start(), matcher.end()));
            if (includePreview) {
                row.put("contentPreview", truncate(read.text(), 1200));
            }
            matches.add(row);
        }

        Map<String, Object> result = paginateRows(args, project, repo, matches, collected);
        result.put("textPattern", textPattern);
        result.put("regex", parseBool(args.get("regex")));
        result.put("caseSensitive", parseBool(args.get("caseSensitive")));
        result.put("candidateFiles", candidateFiles.size());
        result.put("scannedFiles", scanned);
        result.put("scanLimited", stoppedByLimit);
        addLimitWarnings(result, limits, stoppedByLimit);
        return doneResult(args, result);
    }

    private Map<String, Object> opExploreRepo(Map<String, Object> args) {
        String project = requireProject(args, "explore_repo");
        String repo = resolveRepositoryId(project, args, "explore_repo");
        FileFilters filters = resolveFileFilters(args);
        LimitSettings limits = resolveLimits(args);
        boolean includePreview = parseBool(args.get("includeContentPreview"));

        Map<String, Object> collected = collectRepositoryItems(project, repo, args);
        String err = tryFormatRemoteError(collected);
        if (err != null) return error(err);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) collected.getOrDefault("items", List.of());
        List<Map<String, Object>> files = new ArrayList<>();
        int directories = 0;
        for (Map<String, Object> item : items) {
            if (isDirectory(item)) {
                directories++;
            } else {
                if (!matchesFileFilters(item, filters)) continue;
                files.add(item);
            }
        }

        List<Map<String, Object>> keyFiles = new ArrayList<>();
        int previewReads = 0;
        boolean previewLimited = false;
        for (Map<String, Object> file : files) {
            String path = itemPath(file).toLowerCase(Locale.ROOT);
            if (isKeyFile(path)) {
                Map<String, Object> row = fileResultRow(file);
                if (includePreview) {
                    if (previewReads >= limits.maxFiles()) {
                        previewLimited = true;
                    } else {
                        previewReads++;
                        ContentRead read = readFileText(project, repo, itemPath(file), limits.maxBytesPerFile(), args);
                        if (read.readable()) {
                            row.put("contentSource", read.source());
                            row.put("contentPreview", truncate(read.text(), 1200));
                            row.put("contentTruncated", read.truncated());
                        }
                    }
                }
                keyFiles.add(row);
            }
        }

        Map<String, Object> paged = paginateRows(args, project, repo, summarizeItems(items), collected);
        paged.put("directories", directories);
        paged.put("files", files.size());
        paged.put("keyFilesCount", keyFiles.size());
        paged.put("keyFiles", keyFiles);
        if (includePreview) {
            paged.put("previewReads", previewReads);
            paged.put("previewLimited", previewLimited);
            addLimitWarnings(paged, limits, previewLimited);
        }
        return doneResult(args, paged);
    }

    private Map<String, Object> opItemsBatch(Map<String, Object> args) {
        String project = requireProject(args, "items_batch");
        String repo = resolveRepositoryId(project, args, "items_batch");
        String descriptorsRaw = str(args, "itemDescriptors");
        if (descriptorsRaw.isBlank()) throw new IllegalArgumentException("'itemDescriptors' (JSON array) es requerido para items_batch");

        Object itemDescriptors;
        try {
            itemDescriptors = JSON.readValue(descriptorsRaw, Object.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("'itemDescriptors' debe ser JSON válido: " + e.getMessage());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("itemDescriptors", itemDescriptors);
        if (!str(args, "scopePath").isBlank()) body.put("scopePath", str(args, "scopePath"));
        if (!str(args, "latestProcessedChange").isBlank()) body.put("latestProcessedChange", parseBool(args.get("latestProcessedChange")));
        if (!str(args, "includeContentMetadata").isBlank()) body.put("includeContentMetadata", parseBool(args.get("includeContentMetadata")));
        if (!str(args, "includeContent").isBlank()) body.put("includeContent", parseBool(args.get("includeContent")));

        Map<String, Object> resp = azureService.postGitApiWithQuery(project, "repositories/" + repo + "/itemsbatch", baseQuery(args), body, itemsApiVersion(args), MediaType.APPLICATION_JSON);
        return done(args, resp);
    }

    private Map<String, Object> opCommitsList(Map<String, Object> args) {
        String project = requireProject(args, "commits_list");
        String repo = resolveRepositoryId(project, args, "commits_list");
        Map<String, String> q = baseQuery(args);
        putInt(q, "searchCriteria.$top", args.get("top"));
        putInt(q, "searchCriteria.$skip", args.get("skip"));
        putIfNotBlank(q, "searchCriteria.author", str(args, "author"));
        putIfNotBlank(q, "searchCriteria.fromDate", str(args, "fromDate"));
        putIfNotBlank(q, "searchCriteria.toDate", str(args, "toDate"));
        putIfNotBlank(q, "searchCriteria.itemPath", str(args, "itemPath"));
        String branch = str(args, "branch");
        if (!branch.isBlank()) {
            q.put("searchCriteria.itemVersion.version", normalizeBranch(branch));
            q.put("searchCriteria.itemVersion.versionType", "branch");
        }
        Map<String, Object> resp = azureService.getGitApiWithQuery(project, "repositories/" + repo + "/commits", q, apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opRefsList(Map<String, Object> args) {
        String project = requireProject(args, "refs_list");
        String repo = resolveRepositoryId(project, args, "refs_list");
        Map<String, String> q = baseQuery(args);
        String branch = str(args, "branch");
        if (!branch.isBlank()) q.put("filter", normalizeRef(branch));
        putInt(q, "$top", args.get("top"));
        putInt(q, "$skip", args.get("skip"));
        putBool(q, "includeLinks", args.get("includeLinks"));
        Map<String, Object> resp = azureService.getGitApiWithQuery(project, "repositories/" + repo + "/refs", q, apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opRefsUpdate(Map<String, Object> args) {
        String project = requireProject(args, "refs_update");
        String repo = resolveRepositoryId(project, args, "refs_update");
        String branch = str(args, "branch");
        String oldObjectId = str(args, "oldObjectId");
        String newObjectId = str(args, "newObjectId");
        if (branch.isBlank() || oldObjectId.isBlank() || newObjectId.isBlank()) {
            throw new IllegalArgumentException("'branch', 'oldObjectId' y 'newObjectId' son requeridos para refs_update");
        }

        Map<String, Object> update = new LinkedHashMap<>();
        update.put("name", normalizeRef(branch));
        update.put("oldObjectId", oldObjectId);
        update.put("newObjectId", newObjectId);
        if (args.get("isLocked") != null) update.put("isLocked", parseBool(args.get("isLocked")));

        Map<String, Object> resp = azureService.postGitApiWithQuery(
                project,
                "repositories/" + repo + "/refs",
                baseQuery(args),
                List.of(update),
                apiVersion(args),
                MediaType.APPLICATION_JSON
        );
        return done(args, resp);
    }

    private Map<String, Object> opPushesList(Map<String, Object> args) {
        String project = requireProject(args, "pushes_list");
        String repo = resolveRepositoryId(project, args, "pushes_list");
        Map<String, String> q = baseQuery(args);
        putInt(q, "$top", args.get("top"));
        putInt(q, "$skip", args.get("skip"));
        putIfNotBlank(q, "searchCriteria.refName", normalizeRef(str(args, "branch")));
        Map<String, Object> resp = azureService.getGitApiWithQuery(project, "repositories/" + repo + "/pushes", q, pushesApiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opPushesGet(Map<String, Object> args) {
        String project = requireProject(args, "pushes_get");
        String repo = resolveRepositoryId(project, args, "pushes_get");
        Object pushId = args.get("pushId");
        if (pushId == null) throw new IllegalArgumentException("'pushId' es requerido para pushes_get");
        Map<String, Object> resp = azureService.getGitApiWithQuery(project, "repositories/" + repo + "/pushes/" + pushId, baseQuery(args), pushesApiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opPushesCreate(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "pushes_create");
        String repo = resolveRepositoryId(project, args, "pushes_create");

        Map<String, Object> body = parseJsonObject(str(args, "bodyJson"));
        if (body.isEmpty()) {
            body = buildPushCreateBody(args);
        }

        if (!body.containsKey("refUpdates") || !body.containsKey("commits")) {
            throw new IllegalArgumentException("pushes_create requiere body con 'refUpdates' y 'commits' (o use bodyJson completo)");
        }

        Map<String, Object> resp = azureService.postGitApiWithQuery(
                project,
                "repositories/" + repo + "/pushes",
                baseQuery(args),
                body,
                pushesApiVersion(args),
                MediaType.APPLICATION_JSON
        );
        return done(args, resp);
    }

    private Map<String, Object> buildPushCreateBody(Map<String, Object> args) throws Exception {
        String branch = normalizeRef(str(args, "branch"));
        String oldObjectId = str(args, "oldObjectId");

        if (branch.isBlank()) {
            throw new IllegalArgumentException("'branch' es requerido para pushes_create cuando no se usa bodyJson");
        }
        if (oldObjectId.isBlank()) {
            throw new IllegalArgumentException("'oldObjectId' es requerido para pushes_create cuando no se usa bodyJson");
        }

        List<Map<String, Object>> commits;
        String commitsJson = str(args, "commitsJson");
        if (!commitsJson.isBlank()) {
            commits = parseJsonArrayOfObjects(commitsJson, "commitsJson");
        } else {
            Map<String, Object> commit = new LinkedHashMap<>();
            String comment = str(args, "commitComment");
            if (comment.isBlank()) comment = "API-first change via azuredevops_git_repositories";
            commit.put("comment", comment);

            List<Map<String, Object>> changes;
            String changesJson = str(args, "changesJson");
            if (!changesJson.isBlank()) {
                changes = parseJsonArrayOfObjects(changesJson, "changesJson");
            } else {
                changes = List.of(buildSingleChange(args));
            }

            commit.put("changes", changes);
            commits = List.of(commit);
        }

        Map<String, Object> refUpdate = new LinkedHashMap<>();
        refUpdate.put("name", branch);
        refUpdate.put("oldObjectId", oldObjectId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("refUpdates", List.of(refUpdate));
        body.put("commits", commits);
        return body;
    }

    private Map<String, Object> buildSingleChange(Map<String, Object> args) {
        String changeType = str(args, "changeType");
        if (changeType.isBlank()) changeType = "edit";

        String path = str(args, "path");
        if (path.isBlank()) {
            throw new IllegalArgumentException("'path' es requerido para pushes_create cuando no se usa changesJson/commitsJson/bodyJson");
        }

        Map<String, Object> change = new LinkedHashMap<>();
        change.put("changeType", changeType);
        change.put("item", Map.of("path", path));

        String sourcePath = str(args, "sourcePath");
        if (!sourcePath.isBlank()) {
            change.put("sourceServerItem", sourcePath);
        }

        if (!"delete".equalsIgnoreCase(changeType)) {
            String content = str(args, "content");
            String contentBase64 = str(args, "contentBase64");
            if (content.isBlank() && contentBase64.isBlank()) {
                throw new IllegalArgumentException("pushes_create requiere 'content' o 'contentBase64' para changeType distinto de delete");
            }

            Map<String, Object> newContent = new LinkedHashMap<>();
            if (!contentBase64.isBlank()) {
                newContent.put("content", contentBase64);
                newContent.put("contentType", "base64Encoded");
            } else {
                newContent.put("content", content);
                String ct = str(args, "contentType");
                newContent.put("contentType", ct.isBlank() ? "rawText" : ct);
            }
            change.put("newContent", newContent);
        }

        return change;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonObject(String json) throws Exception {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        Object parsed = JSON.readValue(json, Object.class);
        if (!(parsed instanceof Map<?, ?> m)) {
            throw new IllegalArgumentException("bodyJson debe ser un objeto JSON");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : m.entrySet()) {
            if (e.getKey() != null) out.put(e.getKey().toString(), e.getValue());
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseJsonArrayOfObjects(String json, String fieldName) throws Exception {
        Object parsed = JSON.readValue(json, Object.class);
        if (!(parsed instanceof List<?> list)) {
            throw new IllegalArgumentException("'" + fieldName + "' debe ser un array JSON");
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> m)) {
                throw new IllegalArgumentException("'" + fieldName + "' debe contener solo objetos JSON");
            }
            Map<String, Object> row = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (e.getKey() != null) row.put(e.getKey().toString(), e.getValue());
            }
            out.add(row);
        }
        return out;
    }

    private Map<String, Object> opDownloadZip(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "download_zip");
        String repo = resolveRepositoryId(project, args, "download_zip");
        String path = str(args, "path");
        if (path.isBlank()) path = "/";

        Map<String, String> q = baseQuery(args);
        q.put("scopePath", path);
        q.put("download", "true");
        q.put("$format", "zip");
        putBool(q, "zipForUnix", args.get("zipForUnix"));
        putVersionDescriptor(q, args);

        Map<String, Object> binary = azureService.getGitBinary(project, "repositories/" + repo + "/items", q, itemsApiVersion(args));
        String err = tryFormatRemoteError(binary);
        if (err != null) return error(err);

        String b64 = Objects.toString(binary.get("data"), "");
        if (b64.isBlank()) return error("No se recibió contenido ZIP");

        byte[] bytes = Base64.getDecoder().decode(b64);
        String outputPath = str(args, "outputPath");
        String savedTo = null;
        if (!outputPath.isBlank()) {
            Path out = Path.of(outputPath).toAbsolutePath().normalize();
            Path parent = out.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.write(out, bytes);
            savedTo = out.toString();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("project", project);
        result.put("repositoryId", repo);
        result.put("scopePath", path);
        result.put("contentType", Objects.toString(binary.get("contentType"), "application/zip"));
        result.put("bytes", bytes.length);
        result.put("downloadedAt", OffsetDateTime.now().toString());
        if (savedTo != null) result.put("savedToPath", savedTo);

        boolean includeB64 = parseBool(args.get("base64"));
        if (includeB64) {
            Integer max = parseInt(args.get("maxBase64Chars"));
            if (max != null && max > 0 && b64.length() > max) {
                result.put("dataBase64", b64.substring(0, max));
                result.put("base64Truncated", true);
                result.put("base64ReturnedChars", max);
                result.put("base64TotalChars", b64.length());
            } else {
                result.put("dataBase64", b64);
                result.put("base64Truncated", false);
            }
        }

        return doneResult(args, result);
    }

    private Map<String, Object> opRepoToPipelines(Map<String, Object> args) {
        String project = requireProject(args, "repo_to_pipelines");
        String repoId = resolveRepositoryId(project, args, "repo_to_pipelines");

        Map<String, Object> repoResp = azureService.getGitApiWithQuery(project, "repositories/" + repoId, baseQuery(args), apiVersion(args));
        String repoErr = tryFormatRemoteError(repoResp);
        if (repoErr != null) return error(repoErr);

        String repoName = Objects.toString(repoResp.get("name"), "").trim();
        List<String> warnings = new ArrayList<>();
        Set<String> repoPipelineFiles = collectRepositoryPipelineYamlPaths(project, repoId, args, warnings);

        Map<String, String> q = new LinkedHashMap<>();
        q.put("name", repoName);
        q.put("includeAllProperties", "true");
        Map<String, Object> defsResp = azureService.exchangeDevAreaApi(
                project,
                "build",
                HttpMethod.GET,
                "definitions",
                q,
                null,
                buildApiVersion(args),
                null,
                null,
                false
        );
        String defsErr = tryFormatRemoteError(defsResp);
        if (defsErr != null) return error(defsErr);

        List<Map<String, Object>> definitions = toObjectList(defsResp.get("value"));
        List<Map<String, Object>> found = new ArrayList<>();
        for (Map<String, Object> def : definitions) {
            String defRepoId = extractBuildRepositoryId(def);
            String yamlPath = extractBuildYamlPath(def);

            boolean byRepoId = !defRepoId.isBlank() && defRepoId.equalsIgnoreCase(repoId);
            boolean byCiPath = !yamlPath.isBlank() && repoPipelineFiles.contains(yamlPath);
            if (!byRepoId && !byCiPath) continue;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("pipelineId", def.get("id"));
            row.put("pipelineName", def.get("name"));
            row.put("folder", def.get("path"));
            row.put("configurationPath", yamlPath);
            row.put("confirmedByRepositoryId", byRepoId);
            row.put("confirmedByCiPath", byCiPath);
            row.put("confidence", byRepoId ? "high" : "medium");
            found.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("project", project);
        result.put("repository", repositorySummary(repoResp));
        result.put("repositoryPipelineFiles", new ArrayList<>(repoPipelineFiles));
        result.put("scannedPipelines", definitions.size());
        result.put("count", found.size());
        result.put("pipelines", found);
        if (!warnings.isEmpty()) result.put("warnings", warnings);
        return doneResult(args, result);
    }

    private Map<String, Object> opPipelineToRepo(Map<String, Object> args) {
        String project = requireProject(args, "pipeline_to_repo");
        Integer pipelineId = requireIntArg(args, "pipelineId", "pipeline_to_repo");

        List<String> warnings = new ArrayList<>();
        List<String> evidence = new ArrayList<>();

        Map<String, Object> defResp = azureService.exchangeDevAreaApi(
                project,
                "build",
                HttpMethod.GET,
                "definitions/" + pipelineId,
                Map.of("includeAllProperties", "true"),
                null,
                buildApiVersion(args),
                null,
                null,
                false
        );

        String defErr = tryFormatRemoteError(defResp);
        if (defErr != null) return error(defErr);

        String repoId = extractBuildRepositoryId(defResp);
        String repoName = extractBuildRepositoryName(defResp);
        String yamlPath = extractBuildYamlPath(defResp);

        Map<String, Object> parentRepository = null;
        if (!repoId.isBlank()) {
            Map<String, Object> repoResp = azureService.getGitApiWithQuery(project, "repositories/" + repoId, baseQuery(args), apiVersion(args));
            String repoErr = tryFormatRemoteError(repoResp);
            if (repoErr == null) {
                parentRepository = repoResp;
                evidence.add("build.definition.repository.id apunta al repositorio padre");
            } else {
                warnings.add("No se pudo obtener repositorio por repository.id: " + repoErr);
            }
        }

        if (parentRepository == null && !repoName.isBlank()) {
            try {
                String resolvedId = resolveRepositoryId(project, Map.of("repositoryName", repoName), "pipeline_to_repo");
                Map<String, Object> repoResp = azureService.getGitApiWithQuery(project, "repositories/" + resolvedId, baseQuery(args), apiVersion(args));
                String repoErr = tryFormatRemoteError(repoResp);
                if (repoErr == null) {
                    parentRepository = repoResp;
                    evidence.add("fallback por nombre de repositorio en definición");
                } else {
                    warnings.add("No se pudo resolver repo por nombre en definición: " + repoErr);
                }
            } catch (Exception ex) {
                warnings.add("No se pudo resolver repo por nombre en definición: " + ex.getMessage());
            }
        }

        if (parentRepository == null) {
            RepoCandidate fallback = resolveParentRepositoryByHeuristics(project, defResp, args, warnings);
            if (fallback.repository() != null) {
                parentRepository = fallback.repository();
                evidence.addAll(fallback.evidence());
            }
            if (!fallback.candidates().isEmpty()) {
                warnings.add("Se usó heurística de nombre para resolver repositorio padre");
            }
        }

        if (parentRepository == null) {
            return error("No se pudo resolver el repositorio padre del pipeline");
        }

        boolean pathExists = false;
        if (!yamlPath.isBlank()) {
            String parentRepoId = Objects.toString(parentRepository.get("id"), "");
            pathExists = pathExistsInRepository(project, parentRepoId, yamlPath, args);
            if (pathExists) evidence.add("yamlFilename existe en el repositorio padre");
        }

        Map<String, Object> pipeline = new LinkedHashMap<>();
        pipeline.put("id", defResp.get("id"));
        pipeline.put("name", defResp.get("name"));
        pipeline.put("folder", defResp.get("path"));
        pipeline.put("configurationPath", yamlPath);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("project", project);
        result.put("pipeline", pipeline);
        result.put("parentRepository", repositorySummary(parentRepository));
        result.put("pipelinePathExistsInParentRepository", pathExists);
        result.put("confidence", !repoId.isBlank() ? "high" : "medium");
        result.put("evidence", evidence);
        if (!warnings.isEmpty()) result.put("warnings", warnings);
        return doneResult(args, result);
    }

    private String extractBuildRepositoryId(Map<String, Object> definition) {
        Map<String, Object> repository = toObjectMap(definition.get("repository"));
        String id = Objects.toString(repository.get("id"), "").trim();
        if (!id.isBlank()) return id;

        Map<String, Object> props = toObjectMap(repository.get("properties"));
        String safe = Objects.toString(props.get("safeRepository"), "").trim();
        return safe;
    }

    private String extractBuildRepositoryName(Map<String, Object> definition) {
        Map<String, Object> repository = toObjectMap(definition.get("repository"));
        return Objects.toString(repository.get("name"), "").trim();
    }

    private String extractBuildYamlPath(Map<String, Object> definition) {
        Map<String, Object> process = toObjectMap(definition.get("process"));
        return normalizePath(Objects.toString(process.get("yamlFilename"), "").trim());
    }

    private String resolveRepositoryId(String project, Map<String, Object> args, String op) {
        String id = str(args, "repositoryId");
        if (!id.isBlank()) return id;

        String name = str(args, "repositoryName");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Debe indicar 'repositoryId' o 'repositoryName' para " + op);
        }

        Map<String, Object> resp = azureService.getGitApiWithQuery(project, "repositories/" + name, baseQuery(args), apiVersion(args));
        String err = tryFormatRemoteError(resp);
        if (err != null) {
            throw new IllegalArgumentException("No se pudo resolver repositoryName='" + name + "': " + err);
        }

        String resolved = Objects.toString(resp.get("id"), "").trim();
        if (resolved.isBlank()) {
            throw new IllegalArgumentException("No se encontró ID para repositoryName='" + name + "'");
        }
        return resolved;
    }

    private Set<String> collectRepositoryPipelineYamlPaths(String project,
                                                           String repo,
                                                           Map<String, Object> args,
                                                           List<String> warnings) {
        Map<String, String> q = baseQuery(args);
        q.put("scopePath", "/CI");
        q.put("recursionLevel", "full");
        q.put("includeContentMetadata", "false");
        Map<String, Object> resp = azureService.getGitApiWithQuery(project, "repositories/" + repo + "/items", q, itemsApiVersion(args));
        String err = tryFormatRemoteError(resp);
        if (err != null) {
            warnings.add("No se encontró carpeta CI o no fue posible listarla: " + err);
            return Set.of();
        }

        List<Map<String, Object>> items = extractItemsFromItemsResponse(resp);
        Set<String> paths = new LinkedHashSet<>();
        for (Map<String, Object> item : items) {
            if (isDirectory(item)) continue;
            String path = normalizePath(itemPath(item));
            String lower = path.toLowerCase(Locale.ROOT);
            if (!(lower.endsWith(".yml") || lower.endsWith(".yaml"))) continue;
            paths.add(path);
        }
        return paths;
    }

    private List<Map<String, Object>> prefilterPipelinesForRepository(List<Map<String, Object>> stubs, String repositoryName) {
        if (stubs == null || stubs.isEmpty()) return List.of();

        String repoNorm = normalizeName(repositoryName);
        Set<String> repoTokens = tokenSet(repoNorm);
        String strongToken = strongestToken(repoTokens);

        List<Map<String, Object>> exact = new ArrayList<>();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> stub : stubs) {
            String pipelineName = Objects.toString(stub.get("name"), "");
            String nameNorm = normalizeName(pipelineName);

            if (!repoNorm.isBlank() && nameNorm.equals(repoNorm)) {
                exact.add(stub);
                continue;
            }

            if (!repoNorm.isBlank() && (nameNorm.contains(repoNorm) || repoNorm.contains(nameNorm))) {
                out.add(stub);
                continue;
            }

            int similarity = nameSimilarityScore(repositoryName, pipelineName);
            if (similarity >= 10) {
                out.add(stub);
                continue;
            }

            if (strongToken != null && !strongToken.isBlank()) {
                Set<String> pipelineTokens = tokenSet(nameNorm);
                if (pipelineTokens.contains(strongToken)) {
                    out.add(stub);
                }
            }
        }
        if (!exact.isEmpty()) return exact;
        return out;
    }

    private String strongestToken(Set<String> tokens) {
        String best = null;
        int bestLen = -1;
        for (String token : tokens) {
            if (token == null || token.isBlank()) continue;
            if (token.length() > bestLen) {
                bestLen = token.length();
                best = token;
            }
        }
        return best;
    }

    private RepoCandidate resolveParentRepositoryByHeuristics(String project,
                                                              Map<String, Object> pipeline,
                                                              Map<String, Object> args,
                                                              List<String> warnings) {
        Map<String, Object> reposResp = azureService.getGitApiWithQuery(project, "repositories", baseQuery(args), apiVersion(args));
        String reposErr = tryFormatRemoteError(reposResp);
        if (reposErr != null) {
            warnings.add("No se pudieron listar repos para fallback pipeline_to_repo: " + reposErr);
            return new RepoCandidate(null, 0, List.of(), List.of());
        }

        String pipelineName = Objects.toString(pipeline.get("name"), "").trim();
        List<Map<String, Object>> repos = toObjectList(reposResp.get("value"));
        List<Map<String, Object>> candidates = new ArrayList<>();

        Map<String, Object> best = null;
        int bestScore = -1;
        List<String> bestEvidence = List.of();

        for (Map<String, Object> repo : repos) {
            String repoName = Objects.toString(repo.get("name"), "").trim();
            String repoId = Objects.toString(repo.get("id"), "").trim();
            if (repoId.isBlank()) continue;

            int score = 0;
            List<String> evidence = new ArrayList<>();

            int nameScore = nameSimilarityScore(repoName, pipelineName);
            if (nameScore > 0) {
                score += nameScore;
                evidence.add("similitud nombre pipeline/repositorio");
            }

            if (score <= 0) continue;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("repositoryId", repoId);
            row.put("repositoryName", repoName);
            row.put("score", score);
            row.put("confidence", confidenceFromScore(score));
            row.put("evidence", evidence);
            candidates.add(row);

            if (score > bestScore) {
                best = repo;
                bestScore = score;
                bestEvidence = new ArrayList<>(evidence);
            }
        }

        candidates.sort((a, b) -> Integer.compare(asInt(b.get("score"), 0), asInt(a.get("score"), 0)));
        return new RepoCandidate(best, Math.max(bestScore, 0), bestEvidence, candidates);
    }

    private String confidenceFromScore(int score) {
        if (score >= 80) return "high";
        if (score >= 55) return "medium";
        return "low";
    }

    private boolean pathExistsInRepository(String project,
                                           String repositoryId,
                                           String path,
                                           Map<String, Object> args) {
        String normalized = normalizePath(path);
        if (normalized.isBlank()) return false;

        Map<String, Object> itemArgs = new LinkedHashMap<>(args);
        itemArgs.put("repositoryId", repositoryId);
        itemArgs.put("path", normalized);
        itemArgs.put("includeContent", false);
        itemArgs.put("includeContentMetadata", true);

        Map<String, Object> item = opItemsGetRaw(itemArgs, project, repositoryId);
        String err = tryFormatRemoteError(item);
        return err == null;
    }

    private int nameSimilarityScore(String left, String right) {
        String a = normalizeName(left);
        String b = normalizeName(right);
        if (a.isBlank() || b.isBlank()) return 0;
        if (a.equals(b)) return 30;
        if (a.contains(b) || b.contains(a)) return 18;

        Set<String> at = tokenSet(a);
        Set<String> bt = tokenSet(b);
        int overlap = 0;
        for (String t : at) {
            if (bt.contains(t)) overlap++;
        }
        if (overlap >= 4) return 16;
        if (overlap >= 2) return 10;
        if (overlap == 1) return 4;
        return 0;
    }

    private String normalizeName(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replace('_', '-')
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "")
                .trim();
    }

    private Set<String> tokenSet(String value) {
        Set<String> out = new LinkedHashSet<>();
        if (value == null || value.isBlank()) return out;
        for (String part : value.split("-")) {
            String token = part.trim();
            if (token.length() < 2) continue;
            out.add(token);
        }
        return out;
    }

    private Map<String, Object> toObjectMap(Object value) {
        if (!(value instanceof Map<?, ?> row)) return Map.of();
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : row.entrySet()) {
            if (e.getKey() != null) out.put(e.getKey().toString(), e.getValue());
        }
        return out;
    }

    private Map<String, Object> pipelineSummary(Map<String, Object> pipeline) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", pipeline.get("id"));
        out.put("name", pipeline.get("name"));
        out.put("folder", pipeline.get("folder"));

        Map<String, Object> config = toObjectMap(pipeline.get("configuration"));
        if (!config.isEmpty()) {
            out.put("configurationType", config.get("type"));
            out.put("configurationPath", normalizePath(Objects.toString(config.get("path"), "")));
            Map<String, Object> repo = toObjectMap(config.get("repository"));
            if (!repo.isEmpty()) out.put("configurationRepositoryId", repo.get("id"));
        }
        return out;
    }

    private Integer requireIntArg(Map<String, Object> args, String key, String op) {
        Integer value = parseInt(args.get(key));
        if (value == null) {
            throw new IllegalArgumentException("'" + key + "' es requerido para " + op);
        }
        return value;
    }

    private Map<String, Object> opItemsGetRaw(Map<String, Object> args, String project, String repo) {
        String path = str(args, "path");
        if (path.isBlank()) throw new IllegalArgumentException("'path' es requerido");

        Map<String, String> q = baseQuery(args);
        q.put("path", path);
        putBool(q, "download", args.get("download"));
        putBool(q, "includeContent", args.get("includeContent"));
        putBool(q, "includeContentMetadata", args.get("includeContentMetadata"));
        putBool(q, "latestProcessedChange", args.get("latestProcessedChange"));
        putBool(q, "resolveLfs", args.get("resolveLfs"));
        putBool(q, "sanitize", args.get("sanitize"));
        putIfNotBlank(q, "scopePath", str(args, "scopePath"));
        putVersionDescriptor(q, args);
        putIfNotBlank(q, "recursionLevel", str(args, "recursionLevel"));

        return azureService.getGitApiWithQuery(project, "repositories/" + repo + "/items", q, itemsApiVersion(args));
    }

    private String extractObjectId(Map<String, Object> item) {
        if (item == null || item.isEmpty()) return "";
        String direct = Objects.toString(item.get("objectId"), "").trim();
        if (!direct.isBlank()) return direct;

        Object value = item.get("value");
        if (value instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            Object nested = first.get("objectId");
            if (nested != null) return nested.toString().trim();
        }
        return "";
    }

    private Map<String, Object> collectRepositoryItems(String project, String repo, Map<String, Object> args) {
        String scopePath = effectiveScopePath(args);
        String recursion = str(args, "recursionLevel");
        if (recursion.isBlank()) recursion = "full";

        List<String> warnings = new ArrayList<>();
        boolean forceTrees = parseBool(args.get("recursive"));

        if (!forceTrees) {
            Map<String, Object> listResp = listItemsRaw(project, repo, args, recursion, scopePath);
            String listErr = tryFormatRemoteError(listResp);
            if (listErr == null) {
                List<Map<String, Object>> items = extractItemsFromItemsResponse(listResp);
                items = filterByScopePath(items, scopePath);
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("strategy", "items_list");
                out.put("strategyFallback", false);
                out.put("items", dedupeByPath(items));
                return out;
            }
            warnings.add("items_list fallback a trees_get: " + listErr);
        } else {
            warnings.add("Estrategia trees_get forzada por parámetro 'recursive=true'");
        }

        String sha = str(args, "sha1");
        if (sha.isBlank()) {
            sha = resolveRootTreeSha(project, repo, args);
        }
        if (sha.isBlank()) {
            throw new IllegalArgumentException("No se pudo resolver SHA de árbol raíz para fallback trees_get. Indique 'sha1'.");
        }

        Map<String, String> q = new LinkedHashMap<>();
        q.put("recursive", "true");
        Map<String, Object> treeResp = azureService.getGitApiWithQuery(project, "repositories/" + repo + "/trees/" + sha, q, itemsApiVersion(args));
        String treeErr = tryFormatRemoteError(treeResp);
        if (treeErr != null) {
            throw new IllegalArgumentException("items_list_recursive fallback trees_get falló: " + treeErr);
        }

        List<Map<String, Object>> items = normalizeTreeEntries(treeResp.get("treeEntries"));
        items = filterByScopePath(items, scopePath);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("strategy", "trees_get");
        out.put("strategyFallback", true);
        out.put("items", dedupeByPath(items));
        if (!warnings.isEmpty()) out.put("warnings", warnings);
        return out;
    }

    private Map<String, String> buildItemsListQuery(Map<String, Object> args, String recursionLevel, String scopePath) {
        Map<String, String> q = baseQuery(args);
        if (!scopePath.isBlank()) q.put("scopePath", scopePath);
        q.put("recursionLevel", recursionLevel);
        if (args.containsKey("includeContentMetadata")) {
            putBool(q, "includeContentMetadata", args.get("includeContentMetadata"));
        } else {
            q.put("includeContentMetadata", "true");
        }
        putBool(q, "latestProcessedChange", args.get("latestProcessedChange"));
        putBool(q, "download", args.get("download"));
        putBool(q, "zipForUnix", args.get("zipForUnix"));
        putVersionDescriptor(q, args);
        return q;
    }

    private Map<String, Object> listItemsRaw(String project,
                                             String repo,
                                             Map<String, Object> args,
                                             String recursionLevel,
                                             String scopePath) {
        Map<String, String> q = buildItemsListQuery(args, recursionLevel, scopePath);
        return azureService.getGitApiWithQuery(project, "repositories/" + repo + "/items", q, itemsApiVersion(args));
    }

    private String resolveRootTreeSha(String project, String repo, Map<String, Object> args) {
        Map<String, Object> rootList = listItemsRaw(project, repo, args, "oneLevel", "/");
        String rootListErr = tryFormatRemoteError(rootList);
        if (rootListErr == null) {
            String rootFromList = findRootTreeObjectId(rootList);
            if (!rootFromList.isBlank()) return rootFromList;
        }

        Map<String, Object> rootArgs = new LinkedHashMap<>(args);
        rootArgs.put("path", "/");
        rootArgs.put("includeContent", false);
        rootArgs.put("includeContentMetadata", true);
        rootArgs.put("latestProcessedChange", true);
        rootArgs.put("repositoryId", repo);

        Map<String, Object> rootItem = opItemsGetRaw(rootArgs, project, repo);
        String err = tryFormatRemoteError(rootItem);
        if (err == null) {
            String objectId = extractObjectId(rootItem);
            if (!objectId.isBlank()) return objectId;
        }

        Map<String, Object> repoResp = azureService.getGitApiWithQuery(project, "repositories/" + repo, baseQuery(args), apiVersion(args));
        String repoErr = tryFormatRemoteError(repoResp);
        if (repoErr != null) return "";

        String defaultBranch = normalizeBranch(Objects.toString(repoResp.get("defaultBranch"), ""));
        if (defaultBranch.isBlank()) defaultBranch = "main";

        Map<String, String> q = new LinkedHashMap<>();
        q.put("searchCriteria.$top", "1");
        q.put("searchCriteria.itemVersion.version", defaultBranch);
        q.put("searchCriteria.itemVersion.versionType", "branch");
        Map<String, Object> commits = azureService.getGitApiWithQuery(project, "repositories/" + repo + "/commits", q, apiVersion(args));
        String commitErr = tryFormatRemoteError(commits);
        if (commitErr != null) return "";

        Object value = commits.get("value");
        if (value instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            Object treeId = first.get("treeId");
            if (treeId != null) return treeId.toString().trim();
        }
        return "";
    }

    private String findRootTreeObjectId(Map<String, Object> itemsResp) {
        List<Map<String, Object>> rows = extractItemsFromItemsResponse(itemsResp);
        for (Map<String, Object> row : rows) {
            String path = normalizePath(itemPath(row));
            if (!"/".equals(path)) continue;
            if (!isDirectory(row)) continue;
            String objectId = Objects.toString(row.get("objectId"), "").trim();
            if (!objectId.isBlank()) return objectId;
        }
        return "";
    }

    private List<Map<String, Object>> extractItemsFromItemsResponse(Map<String, Object> resp) {
        if (resp == null || resp.isEmpty()) return List.of();
        Object value = resp.get("value");
        if (value instanceof List<?>) {
            return toObjectList(value);
        }
        if (resp.containsKey("path")) {
            return List.of(new LinkedHashMap<>(resp));
        }
        return List.of();
    }

    private List<Map<String, Object>> normalizeTreeEntries(Object entriesObj) {
        if (!(entriesObj instanceof List<?> entries)) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object e : entries) {
            if (!(e instanceof Map<?, ?> row)) continue;
            String relative = Objects.toString(row.get("relativePath"), "").trim();
            if (relative.isBlank()) continue;

            String path = relative.startsWith("/") ? relative : "/" + relative;
            String gitObjectType = Objects.toString(row.get("gitObjectType"), "");
            boolean folder = "tree".equalsIgnoreCase(gitObjectType);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("path", path);
            if (row.get("objectId") != null) item.put("objectId", row.get("objectId"));
            if (!gitObjectType.isBlank()) item.put("gitObjectType", gitObjectType);
            item.put("isFolder", folder);
            if (row.get("url") != null) item.put("url", row.get("url"));
            out.add(item);
        }
        return out;
    }

    private List<Map<String, Object>> filterByScopePath(List<Map<String, Object>> items, String scopePath) {
        String scope = normalizePath(scopePath);
        if (scope.isBlank() || "/".equals(scope)) return items;

        List<Map<String, Object>> out = new ArrayList<>();
        String prefix = scope.endsWith("/") ? scope : scope + "/";
        for (Map<String, Object> item : items) {
            String path = normalizePath(itemPath(item));
            if (path.equals(scope) || path.startsWith(prefix)) out.add(item);
        }
        return out;
    }

    private List<Map<String, Object>> dedupeByPath(List<Map<String, Object>> rows) {
        Set<String> seen = new LinkedHashSet<>();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String path = normalizePath(itemPath(row));
            if (path.isBlank()) continue;
            if (!seen.add(path.toLowerCase(Locale.ROOT))) continue;
            out.add(row);
        }
        return out;
    }

    private String effectiveScopePath(Map<String, Object> args) {
        String scopePath = str(args, "scopePath");
        if (!scopePath.isBlank()) return normalizePath(scopePath);

        String path = str(args, "path");
        if (!path.isBlank()) return normalizePath(path);
        return "/";
    }

    private boolean isDirectory(Map<String, Object> item) {
        Object isFolderObj = item.get("isFolder");
        if (isFolderObj != null && parseBool(isFolderObj)) return true;
        String type = Objects.toString(item.get("gitObjectType"), "");
        return "tree".equalsIgnoreCase(type);
    }

    private String itemPath(Map<String, Object> item) {
        String path = Objects.toString(item.get("path"), "").trim();
        if (!path.isBlank()) return path;
        return Objects.toString(item.get("relativePath"), "").trim();
    }

    private Map<String, Object> fileResultRow(Map<String, Object> item) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("path", normalizePath(itemPath(item)));
        if (item.get("objectId") != null) row.put("objectId", item.get("objectId"));
        if (item.get("url") != null) row.put("url", item.get("url"));
        String type = isDirectory(item) ? "tree" : "blob";
        row.put("type", type);
        return row;
    }

    private List<Map<String, Object>> summarizeItems(List<Map<String, Object>> items) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> item : items) {
            rows.add(fileResultRow(item));
        }
        return rows;
    }

    private Map<String, Object> paginateRows(Map<String, Object> args,
                                             String project,
                                             String repo,
                                             List<Map<String, Object>> rows,
                                             Map<String, Object> source) {
        int skip = parseSkip(args.get("skip"));
        Integer top = parseTop(args.get("top"));
        int total = rows.size();
        int from = Math.min(skip, total);
        int to = top == null ? total : Math.min(total, from + top);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("project", project);
        out.put("repositoryId", repo);
        out.put("count", to - from);
        out.put("totalCount", total);
        out.put("skip", skip);
        if (top != null) out.put("top", top);
        out.put("hasMore", to < total);
        out.put("value", new ArrayList<>(rows.subList(from, to)));
        copyWarnings(source, out);
        return out;
    }

    private void copyWarnings(Map<String, Object> source, Map<String, Object> target) {
        if (source == null || source.isEmpty()) return;
        Object warningsObj = source.get("warnings");
        if (!(warningsObj instanceof List<?> list) || list.isEmpty()) return;

        List<String> warnings = toStringList(target.get("warnings"));
        for (Object w : list) {
            if (w == null) continue;
            String msg = w.toString().trim();
            if (msg.isBlank()) continue;
            warnings.add(msg);
        }
        if (!warnings.isEmpty()) target.put("warnings", warnings);
    }

    private void appendWarning(Map<String, Object> target, String warning) {
        if (warning == null || warning.isBlank()) return;
        List<String> warnings = toStringList(target.get("warnings"));
        warnings.add(warning.trim());
        target.put("warnings", warnings);
    }

    private boolean isScopePathRequiredError(Map<String, Object> response, String formattedError) {
        StringBuilder joined = new StringBuilder();
        if (formattedError != null) joined.append(formattedError).append(' ');
        if (response != null) {
            Object message = response.get("message");
            if (message != null) joined.append(message).append(' ');
            Object bodyRaw = response.get("bodyRaw");
            if (bodyRaw != null) joined.append(bodyRaw).append(' ');
            Object typeName = response.get("typeName");
            if (typeName != null) joined.append(typeName).append(' ');
            Object typeKey = response.get("typeKey");
            if (typeKey != null) joined.append(typeKey);
        }
        String lower = joined.toString().toLowerCase(Locale.ROOT);
        return lower.contains("valid scopepath") || (lower.contains("scopepath") && lower.contains("required"));
    }

    private List<String> toStringList(Object value) {
        List<String> out = new ArrayList<>();
        if (!(value instanceof List<?> list)) return out;
        for (Object item : list) {
            if (item == null) continue;
            String s = item.toString().trim();
            if (!s.isBlank()) out.add(s);
        }
        return out;
    }

    private FileFilters resolveFileFilters(Map<String, Object> args) {
        String filePattern = str(args, "filePattern");
        String pathRegex = str(args, "pathRegex");
        String extensions = str(args, "extensions");

        Pattern globPattern = null;
        if (!filePattern.isBlank()) {
            globPattern = compileGlobPattern(filePattern);
        }

        Pattern regexPattern = null;
        if (!pathRegex.isBlank()) {
            try {
                regexPattern = Pattern.compile(pathRegex);
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException("'pathRegex' inválido: " + e.getMessage());
            }
        }

        Set<String> extSet = new LinkedHashSet<>();
        if (!extensions.isBlank()) {
            for (String part : extensions.split(",")) {
                if (part == null) continue;
                String ext = part.trim().toLowerCase(Locale.ROOT);
                if (ext.startsWith(".")) ext = ext.substring(1);
                if (!ext.isBlank()) extSet.add(ext);
            }
        }
        return new FileFilters(globPattern, regexPattern, extSet);
    }

    private boolean matchesFileFilters(Map<String, Object> item, Map<String, Object> args) {
        FileFilters filters = resolveFileFilters(args);
        return matchesFileFilters(item, filters);
    }

    private boolean matchesFileFilters(Map<String, Object> item, FileFilters filters) {
        String path = normalizePath(itemPath(item));
        if (path.isBlank()) return false;

        if (filters.globPattern() != null && !filters.globPattern().matcher(path).matches()) return false;
        if (filters.regexPattern() != null && !filters.regexPattern().matcher(path).find()) return false;

        if (!filters.extensions().isEmpty()) {
            String ext = extensionOf(path);
            if (ext.isBlank() || !filters.extensions().contains(ext.toLowerCase(Locale.ROOT))) return false;
        }
        return true;
    }

    private Pattern compileGlobPattern(String glob) {
        String g = glob.trim();
        if (g.isBlank()) throw new IllegalArgumentException("'filePattern' no puede estar vacío");

        StringBuilder regex = new StringBuilder();
        regex.append("^");
        for (int i = 0; i < g.length(); i++) {
            char c = g.charAt(i);
            if (c == '*') {
                boolean dbl = (i + 1) < g.length() && g.charAt(i + 1) == '*';
                if (dbl) {
                    regex.append(".*");
                    i++;
                } else {
                    regex.append("[^/]*");
                }
                continue;
            }
            if (c == '?') {
                regex.append(".");
                continue;
            }
            if ("\\.[]{}()+-^$|".indexOf(c) >= 0) regex.append('\\');
            regex.append(c);
        }
        regex.append("$");
        return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
    }

    private Pattern compileSearchPattern(String pattern, boolean regex, boolean caseSensitive) {
        int flags = Pattern.MULTILINE;
        if (!caseSensitive) {
            flags |= Pattern.CASE_INSENSITIVE;
            flags |= Pattern.UNICODE_CASE;
        }
        String expression = regex ? pattern : Pattern.quote(pattern);
        try {
            return Pattern.compile(expression, flags);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("'textPattern' inválido: " + e.getMessage());
        }
    }

    private LimitSettings resolveLimits(Map<String, Object> args) {
        int maxFiles = DEFAULT_MAX_FILES;
        int maxBytes = DEFAULT_MAX_BYTES_PER_FILE;
        boolean customFiles = false;
        boolean customBytes = false;
        boolean clampedFiles = false;
        boolean clampedBytes = false;

        if (args.containsKey("maxFiles") && !str(args, "maxFiles").isBlank()) {
            Integer v = parseInt(args.get("maxFiles"));
            if (v == null || v < 1) throw new IllegalArgumentException("'maxFiles' debe ser entero >= 1");
            customFiles = true;
            maxFiles = v;
        }

        if (args.containsKey("maxBytesPerFile") && !str(args, "maxBytesPerFile").isBlank()) {
            Integer v = parseInt(args.get("maxBytesPerFile"));
            if (v == null || v < 1) throw new IllegalArgumentException("'maxBytesPerFile' debe ser entero >= 1");
            customBytes = true;
            maxBytes = v;
        }

        if (maxFiles > HARD_MAX_FILES) {
            maxFiles = HARD_MAX_FILES;
            clampedFiles = true;
        }
        if (maxBytes > HARD_MAX_BYTES_PER_FILE) {
            maxBytes = HARD_MAX_BYTES_PER_FILE;
            clampedBytes = true;
        }

        return new LimitSettings(maxFiles, maxBytes, customFiles, customBytes, clampedFiles, clampedBytes);
    }

    private void addLimitWarnings(Map<String, Object> result, LimitSettings limits, boolean stoppedByLimit) {
        List<String> warnings = toStringList(result.get("warnings"));
        if (limits.customFiles() && limits.maxFiles() > RECOMMENDED_MAX_FILES) {
            warnings.add("Advertencia: maxFiles=" + limits.maxFiles() + " supera recomendado=" + RECOMMENDED_MAX_FILES + ". Puede aumentar latencia/costo.");
        }
        if (limits.customBytes() && limits.maxBytesPerFile() > RECOMMENDED_MAX_BYTES_PER_FILE) {
            warnings.add("Advertencia: maxBytesPerFile=" + limits.maxBytesPerFile() + " supera recomendado=" + RECOMMENDED_MAX_BYTES_PER_FILE + ". Puede aumentar latencia/costo.");
        }
        if (limits.clampedFiles()) {
            warnings.add("'maxFiles' excedía límite duro; se ajustó a " + HARD_MAX_FILES + ".");
        }
        if (limits.clampedBytes()) {
            warnings.add("'maxBytesPerFile' excedía límite duro; se ajustó a " + HARD_MAX_BYTES_PER_FILE + ".");
        }
        if (stoppedByLimit) {
            warnings.add("Escaneo detenido por límite de maxFiles=" + limits.maxFiles() + ". Ajuste el parámetro si necesita mayor cobertura.");
        }
        if (!warnings.isEmpty()) result.put("warnings", warnings);
    }

    private ContentRead readFileText(String project,
                                     String repo,
                                     String path,
                                     int maxBytes,
                                     Map<String, Object> args) {
        Map<String, Object> itemArgs = new LinkedHashMap<>(args);
        itemArgs.put("path", path);
        itemArgs.put("repositoryId", repo);
        itemArgs.put("includeContent", true);
        itemArgs.put("includeContentMetadata", true);

        Map<String, Object> itemResp = opItemsGetRaw(itemArgs, project, repo);
        String itemErr = tryFormatRemoteError(itemResp);
        if (itemErr == null) {
            String content = Objects.toString(itemResp.get("content"), "");
            if (!content.isBlank()) {
                TruncatedText tt = truncateUtf8(content, maxBytes);
                return new ContentRead(true, tt.text(), "items_get", tt.truncated());
            }
        }

        String objectId = extractObjectId(itemResp);
        if (objectId.isBlank()) return new ContentRead(false, "", "none", false);

        Map<String, String> q = new LinkedHashMap<>();
        q.put("$format", "text");
        putBool(q, "resolveLfs", args.get("resolveLfs"));
        Map<String, Object> blobResp = azureService.exchangeGitApi(
                project,
                HttpMethod.GET,
                "repositories/" + repo + "/blobs/" + objectId,
                q,
                null,
                itemsApiVersion(args),
                null,
                MediaType.TEXT_PLAIN,
                false
        );
        String blobErr = tryFormatRemoteError(blobResp);
        if (blobErr != null) return new ContentRead(false, "", "none", false);

        String text = Objects.toString(blobResp.get("text"), "");
        if (text.isBlank()) return new ContentRead(false, "", "none", false);
        TruncatedText tt = truncateUtf8(text, maxBytes);
        return new ContentRead(true, tt.text(), "blob_fallback", tt.truncated());
    }

    private TruncatedText truncateUtf8(String text, int maxBytes) {
        if (text == null) return new TruncatedText("", false);
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) return new TruncatedText(text, false);

        int len = maxBytes;
        while (len > 0 && (bytes[len - 1] & 0b1100_0000) == 0b1000_0000) {
            len--;
        }
        if (len <= 0) len = Math.min(maxBytes, bytes.length);
        return new TruncatedText(new String(bytes, 0, len, StandardCharsets.UTF_8), true);
    }

    private String snippet(String text, int start, int end) {
        int from = Math.max(0, start - 140);
        int to = Math.min(text.length(), end + 140);
        return text.substring(from, to);
    }

    private String truncate(String text, int maxChars) {
        if (text == null) return "";
        if (text.length() <= maxChars) return text;
        return text.substring(0, maxChars);
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) return "";
        String p = path.replace('\\', '/').trim();
        if (!p.startsWith("/")) p = "/" + p;
        while (p.contains("//")) p = p.replace("//", "/");
        return p;
    }

    private String extensionOf(String path) {
        if (path == null || path.isBlank()) return "";
        int slash = path.lastIndexOf('/');
        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot < slash) return "";
        if (dot == path.length() - 1) return "";
        return path.substring(dot + 1);
    }

    private boolean isKeyFile(String normalizedLowerPath) {
        return normalizedLowerPath.endsWith("/application.conf")
                || normalizedLowerPath.endsWith("/application.properties")
                || normalizedLowerPath.endsWith("/application.yml")
                || normalizedLowerPath.endsWith("/application.yaml")
                || normalizedLowerPath.endsWith("/dockerfile")
                || normalizedLowerPath.endsWith("/pom.xml")
                || normalizedLowerPath.endsWith("/build.gradle")
                || normalizedLowerPath.endsWith("/build.gradle.kts")
                || normalizedLowerPath.endsWith("/package.json")
                || normalizedLowerPath.endsWith("/service.conf")
                || normalizedLowerPath.endsWith("/readme.md");
    }

    private record FileFilters(Pattern globPattern, Pattern regexPattern, Set<String> extensions) {}

    private record WindowRequest(int offset, int limit, long maxWaitMs) {}

    private record TextEligibility(boolean allowed, String reasonCode, String reason, String guidance) {}

    private record DownloadRunResult(boolean success, String errorCode, String message, long bytesWritten) {}

    private record WindowFetchResult(String status,
                                     String message,
                                     String errorCode,
                                     List<String> lines,
                                     int lineEnd,
                                     boolean hasMore,
                                     int totalLines,
                                     long cacheBytes,
                                     long fileBytes,
                                     Instant lastPreparedAt,
                                     Instant downloadStartedAt,
                                     long elapsedMs) {}

    private record LimitSettings(int maxFiles,
                                 int maxBytesPerFile,
                                 boolean customFiles,
                                 boolean customBytes,
                                 boolean clampedFiles,
                                 boolean clampedBytes) {}

    private record ContentRead(boolean readable, String text, String source, boolean truncated) {}

    private record TruncatedText(String text, boolean truncated) {}

    private record RepoCandidate(Map<String, Object> repository,
                                 int score,
                                 List<String> evidence,
                                 List<Map<String, Object>> candidates) {}

    @FunctionalInterface
    private interface DownloadTask {
        DownloadRunResult run(Path outputPath, long maxFileBytes);
    }

    private static final class WindowFileCache {
        private final Path rootDir;
        private final long maxCacheBytes;
        private final long maxFileBytes;
        private final long minFreeDiskBytes;
        private final int maxConcurrentDownloads;
        private final long ttlMillis;
        private final long maxInProgressBytes;
        private final AtomicBoolean startupCleanupDone = new AtomicBoolean(false);
        private final ExecutorService executor;
        private final ConcurrentHashMap<String, CacheEntry> entries = new ConcurrentHashMap<>();

        private WindowFileCache() {
            this.rootDir = Path.of(System.getenv().getOrDefault("MCP_TEXT_CACHE_ROOT", "/tmp/mcp-text-window-cache"))
                    .toAbsolutePath().normalize();
            this.maxCacheBytes = envLong("MCP_TEXT_CACHE_MAX_BYTES", 20L * 1024L * 1024L * 1024L);
            this.maxFileBytes = envLong("MCP_TEXT_CACHE_MAX_FILE_BYTES", 512L * 1024L * 1024L);
            this.minFreeDiskBytes = envLong("MCP_TEXT_CACHE_MIN_FREE_BYTES", 5L * 1024L * 1024L * 1024L);
            this.maxConcurrentDownloads = (int) Math.max(1L, envLong("MCP_TEXT_CACHE_MAX_CONCURRENT", 2L));
            this.ttlMillis = envLong("MCP_TEXT_CACHE_TTL_MS", Duration.ofMinutes(30).toMillis());
            this.maxInProgressBytes = envLong("MCP_TEXT_CACHE_MAX_IN_PROGRESS_BYTES", 4L * 1024L * 1024L * 1024L);
            this.executor = Executors.newFixedThreadPool(this.maxConcurrentDownloads, r -> {
                Thread t = new Thread(r);
                t.setName("git-text-window-cache");
                t.setDaemon(true);
                return t;
            });
        }

        private Path rootDir() {
            ensureInitialized();
            return rootDir;
        }

        private WindowFetchResult fetch(String cacheKey,
                                        String path,
                                        int offset,
                                        int limit,
                                        long maxWaitMs,
                                        DownloadTask downloadTask) {
            ensureInitialized();
            evictExpired();

            CacheEntry entry = entries.computeIfAbsent(cacheKey, key -> new CacheEntry(key, filePathForKey(key)));

            synchronized (entry) {
                entry.lastAccessAt = Instant.now();
                if (entry.status == EntryStatus.READY && Files.exists(entry.filePath)) {
                    return readReady(entry, offset, limit);
                }

                if (entry.status == EntryStatus.DOWNLOADING && entry.downloadFuture != null && !entry.downloadFuture.isDone()) {
                    return awaitOrWarmup(entry, offset, limit, maxWaitMs);
                }

                if (entry.status == EntryStatus.FAILED && entry.lastError != null && !entry.lastError.isBlank()) {
                    return failed(entry.lastErrorCode, entry.lastError, entry.downloadStartedAt, entry.lastDownloadBytes);
                }

                if (!hasDownloadSlot()) {
                    return failed(CODE_TOO_MANY_IN_PROGRESS,
                            "Hay demasiadas descargas en curso para preparar lecturas por líneas. Intente de nuevo en unos segundos.",
                            entry.downloadStartedAt,
                            entry.lastDownloadBytes);
                }

                String quotaError = validateQuotaBeforeDownload();
                if (quotaError != null) {
                    String code = quotaError.startsWith(CODE_DISK_SPACE_LOW + ":") ? CODE_DISK_SPACE_LOW : CODE_CACHE_QUOTA_EXCEEDED;
                    String message = quotaError;
                    if (code.equals(CODE_DISK_SPACE_LOW)) {
                        message = quotaError.substring((CODE_DISK_SPACE_LOW + ":").length()).trim();
                    }
                    return failed(code, message, entry.downloadStartedAt, entry.lastDownloadBytes);
                }

                entry.status = EntryStatus.DOWNLOADING;
                entry.lastError = "";
                entry.lastErrorCode = "";
                entry.downloadStartedAt = Instant.now();
                entry.lastDownloadBytes = 0L;
                entry.downloadFuture = executor.submit(() -> runDownload(entry, downloadTask));
            }

            return awaitOrWarmup(entry, offset, limit, maxWaitMs);
        }

        private void runDownload(CacheEntry entry, DownloadTask downloadTask) {
            Path tmp = entry.filePath.resolveSibling(entry.filePath.getFileName().toString() + ".part");
            try {
                Files.createDirectories(entry.filePath.getParent());
                Files.deleteIfExists(tmp);

                DownloadRunResult result = downloadTask.run(tmp, maxFileBytes);
                synchronized (entry) {
                    if (!result.success()) {
                        entry.status = EntryStatus.FAILED;
                        entry.lastErrorCode = result.errorCode();
                        entry.lastError = result.message();
                        entry.lastDownloadBytes = result.bytesWritten();
                        try {
                            Files.deleteIfExists(tmp);
                        } catch (Exception ignored) {
                            // best-effort
                        }
                        return;
                    }

                    long fileBytes = safeFileSize(tmp);
                    if (fileBytes <= 0 && result.bytesWritten() > 0) fileBytes = result.bytesWritten();
                    if (fileBytes > maxFileBytes) {
                        entry.status = EntryStatus.FAILED;
                        entry.lastErrorCode = CODE_FILE_TOO_LARGE;
                        entry.lastError = "El archivo descargado excede el límite permitido para cache temporal.";
                        entry.lastDownloadBytes = fileBytes;
                        try {
                            Files.deleteIfExists(tmp);
                        } catch (Exception ignored) {
                            // best-effort
                        }
                        return;
                    }
                    if (looksBinary(tmp)) {
                        entry.status = EntryStatus.FAILED;
                        entry.lastErrorCode = CODE_BINARY_NOT_SUPPORTED;
                        entry.lastError = "El contenido descargado parece binario; este endpoint solo admite texto.";
                        entry.lastDownloadBytes = fileBytes;
                        try {
                            Files.deleteIfExists(tmp);
                        } catch (Exception ignored) {
                            // best-effort
                        }
                        return;
                    }

                    enforceQuotaBeforeCommit(fileBytes, entry.key);
                    Files.move(tmp, entry.filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    entry.fileBytes = fileBytes;
                    entry.status = EntryStatus.READY;
                    entry.readyAt = Instant.now();
                    entry.lastPreparedAt = entry.readyAt;
                    entry.lastDownloadBytes = fileBytes;
                    entry.lastError = "";
                    entry.lastErrorCode = "";
                }
            } catch (Exception e) {
                synchronized (entry) {
                    entry.status = EntryStatus.FAILED;
                    entry.lastErrorCode = CODE_DOWNLOAD_FAILED;
                    entry.lastError = e.getMessage();
                }
            } finally {
                try {
                    Files.deleteIfExists(tmp);
                } catch (Exception ignored) {
                    // best-effort
                }
            }
        }

        private WindowFetchResult awaitOrWarmup(CacheEntry entry, int offset, int limit, long maxWaitMs) {
            Future<?> f;
            Instant startedAt;
            synchronized (entry) {
                f = entry.downloadFuture;
                startedAt = entry.downloadStartedAt;
            }

            if (f == null) {
                return failed(CODE_DOWNLOAD_FAILED, "No se pudo iniciar la descarga en background", startedAt, entry.lastDownloadBytes);
            }

            long wait = Math.max(250L, Math.min(maxWaitMs, HARD_WINDOW_MAX_WAIT_MS));
            try {
                f.get(wait, TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException te) {
                return warmingUp(startedAt);
            } catch (Exception e) {
                synchronized (entry) {
                    entry.status = EntryStatus.FAILED;
                    entry.lastErrorCode = CODE_DOWNLOAD_FAILED;
                    entry.lastError = e.getMessage();
                }
            }

            synchronized (entry) {
                if (entry.status == EntryStatus.READY && Files.exists(entry.filePath)) {
                    return readReady(entry, offset, limit);
                }
                return failed(entry.lastErrorCode, entry.lastError, entry.downloadStartedAt, entry.lastDownloadBytes);
            }
        }

        private WindowFetchResult readReady(CacheEntry entry, int offset, int limit) {
            try {
                WindowSlice slice = readLinesWindow(entry.filePath, offset, limit);
                long cacheBytes = computeTotalCacheBytes();
                entry.lastAccessAt = Instant.now();
                entry.fileBytes = safeFileSize(entry.filePath);
                return new WindowFetchResult(
                        WINDOW_STATUS_READY,
                        "",
                        "",
                        slice.lines(),
                        slice.lineEnd(),
                        slice.hasMore(),
                        slice.totalLines(),
                        cacheBytes,
                        entry.fileBytes,
                        entry.lastPreparedAt == null ? Instant.now() : entry.lastPreparedAt,
                        entry.downloadStartedAt,
                        elapsedMs(entry.downloadStartedAt)
                );
            } catch (Exception e) {
                entry.status = EntryStatus.FAILED;
                entry.lastErrorCode = CODE_DOWNLOAD_FAILED;
                entry.lastError = e.getMessage();
                return failed(entry.lastErrorCode, entry.lastError, entry.downloadStartedAt, entry.lastDownloadBytes);
            }
        }

        private WindowSlice readLinesWindow(Path file, int offset, int limit) throws IOException {
            int safeOffset = Math.max(1, offset);
            int safeLimit = Math.max(1, Math.min(limit, HARD_WINDOW_LIMIT));

            List<String> lines = new ArrayList<>(safeLimit);
            int lineNo = 0;
            int lineEnd = safeOffset - 1;
            boolean hasMore = false;

            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lineNo++;
                    if (lineNo < safeOffset) continue;
                    if (lines.size() < safeLimit) {
                        lines.add(line);
                        lineEnd = lineNo;
                        continue;
                    }
                    hasMore = true;
                    break;
                }
            }

            if (!hasMore) {
                hasMore = lineNo > lineEnd;
            }
            int totalLines = hasMore ? -1 : lineNo;
            return new WindowSlice(lines, lineEnd, hasMore, totalLines);
        }

        private long computeTotalCacheBytes() {
            long total = 0L;
            for (CacheEntry e : entries.values()) {
                if (e.status != EntryStatus.READY) continue;
                if (!Files.exists(e.filePath)) continue;
                total += safeFileSize(e.filePath);
            }
            return total;
        }

        private void enforceQuotaBeforeCommit(long incomingBytes, String protectedKey) {
            long current = computeTotalCacheBytes();
            if (current + incomingBytes <= maxCacheBytes) return;

            List<CacheEntry> candidates = new ArrayList<>();
            for (CacheEntry e : entries.values()) {
                if (e.key.equals(protectedKey)) continue;
                if (e.status != EntryStatus.READY) continue;
                if (!Files.exists(e.filePath)) continue;
                candidates.add(e);
            }
            candidates.sort(Comparator.comparing(a -> a.lastAccessAt == null ? Instant.EPOCH : a.lastAccessAt));

            for (CacheEntry candidate : candidates) {
                long bytes = safeFileSize(candidate.filePath);
                try {
                    Files.deleteIfExists(candidate.filePath);
                } catch (Exception ignored) {
                    // best-effort
                }
                candidate.status = EntryStatus.MISSING;
                candidate.fileBytes = 0L;
                current = Math.max(0L, current - bytes);
                if (current + incomingBytes <= maxCacheBytes) break;
            }
        }

        private String validateQuotaBeforeDownload() {
            long cacheBytes = computeTotalCacheBytes();
            if (cacheBytes >= maxCacheBytes) {
                return "Cache temporal lleno (cacheBytes=" + cacheBytes + ", maxCacheBytes=" + maxCacheBytes + ")";
            }

            long inProgressBytes = 0L;
            int activeDownloads = 0;
            for (CacheEntry e : entries.values()) {
                if (e.status != EntryStatus.DOWNLOADING) continue;
                activeDownloads++;
                inProgressBytes += Math.max(0L, e.lastDownloadBytes);
            }
            if (activeDownloads >= maxConcurrentDownloads) {
                return "Límite de descargas en curso alcanzado (" + activeDownloads + "/" + maxConcurrentDownloads + ")";
            }
            if (inProgressBytes >= maxInProgressBytes) {
                return "Bytes en preparación exceden el presupuesto actual (inProgressBytes=" + inProgressBytes + ", maxInProgressBytes=" + maxInProgressBytes + ")";
            }

            long free = safeFreeSpace(rootDir);
            if (free > 0 && free < minFreeDiskBytes) {
                return CODE_DISK_SPACE_LOW + ": Espacio libre insuficiente para descargas temporales (freeBytes=" + free + ", minFreeDiskBytes=" + minFreeDiskBytes + ")";
            }
            return null;
        }

        private boolean hasDownloadSlot() {
            int running = 0;
            for (CacheEntry e : entries.values()) {
                if (e.status == EntryStatus.DOWNLOADING) running++;
            }
            return running < maxConcurrentDownloads;
        }

        private void evictExpired() {
            Instant now = Instant.now();
            for (CacheEntry e : entries.values()) {
                if (e.status != EntryStatus.READY) continue;
                Instant at = e.lastAccessAt == null ? e.lastPreparedAt : e.lastAccessAt;
                if (at == null) continue;
                long age = Duration.between(at, now).toMillis();
                if (age < ttlMillis) continue;
                try {
                    Files.deleteIfExists(e.filePath);
                } catch (Exception ignored) {
                    // best-effort
                }
                e.status = EntryStatus.MISSING;
                e.fileBytes = 0L;
            }
        }

        private Path filePathForKey(String key) {
            String hash = sha256Hex(key);
            String folder = hash.substring(0, 2);
            String file = hash.substring(2) + ".txt";
            return rootDir.resolve(folder).resolve(file);
        }

        private String sha256Hex(String value) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (Exception e) {
                return Integer.toHexString(value.hashCode());
            }
        }

        private void ensureInitialized() {
            if (!startupCleanupDone.compareAndSet(false, true)) return;
            try {
                if (Files.exists(rootDir)) {
                    deleteRecursively(rootDir);
                }
                Files.createDirectories(rootDir);
            } catch (Exception ignored) {
                // best-effort
            }
        }

        private void deleteRecursively(Path root) throws IOException {
            if (!Files.exists(root)) return;
            try (var walk = Files.walk(root)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {
                        // best-effort
                    }
                });
            }
        }

        private long safeFileSize(Path file) {
            try {
                return Files.exists(file) ? Files.size(file) : 0L;
            } catch (Exception e) {
                return 0L;
            }
        }

        private long safeFreeSpace(Path path) {
            try {
                Path p = Files.exists(path) ? path : path.getParent();
                if (p == null) return -1L;
                FileStore store = Files.getFileStore(p);
                return store.getUsableSpace();
            } catch (Exception e) {
                return -1L;
            }
        }

        private boolean looksBinary(Path file) {
            byte[] sample = new byte[8192];
            int read = 0;
            try (var in = Files.newInputStream(file)) {
                read = in.read(sample);
            } catch (Exception e) {
                return false;
            }
            if (read <= 0) return false;

            int control = 0;
            for (int i = 0; i < read; i++) {
                int b = sample[i] & 0xff;
                if (b == 0) return true;
                if (b < 0x09) control++;
                if (b > 0x0D && b < 0x20) control++;
            }
            double ratio = (double) control / (double) read;
            return ratio > 0.18;
        }

        private long elapsedMs(Instant since) {
            if (since == null) return 0L;
            return Math.max(0L, Duration.between(since, Instant.now()).toMillis());
        }

        private WindowFetchResult warmingUp(Instant startedAt) {
            return new WindowFetchResult(
                    WINDOW_STATUS_WARMING_UP,
                    "Preparando archivo en cache temporal",
                    CODE_PREPARING_TIMEOUT,
                    List.of(),
                    0,
                    false,
                    0,
                    computeTotalCacheBytes(),
                    0L,
                    Instant.now(),
                    startedAt,
                    elapsedMs(startedAt)
            );
        }

        private WindowFetchResult failed(String errorCode, String message, Instant startedAt, long bytes) {
            String safeCode = (errorCode == null || errorCode.isBlank()) ? CODE_DOWNLOAD_FAILED : errorCode;
            String safeMessage = (message == null || message.isBlank()) ? "No se pudo preparar archivo temporal" : message;
            return new WindowFetchResult(
                    WINDOW_STATUS_FAILED,
                    safeMessage,
                    safeCode,
                    List.of(),
                    0,
                    false,
                    0,
                    computeTotalCacheBytes(),
                    Math.max(0L, bytes),
                    Instant.now(),
                    startedAt,
                    elapsedMs(startedAt)
            );
        }

        private static long envLong(String key, long defaultValue) {
            String raw = System.getenv(key);
            if (raw == null || raw.isBlank()) return defaultValue;
            try {
                long v = Long.parseLong(raw.trim());
                return v > 0 ? v : defaultValue;
            } catch (Exception e) {
                return defaultValue;
            }
        }

        private static final class CacheEntry {
            private final String key;
            private final Path filePath;
            private volatile EntryStatus status = EntryStatus.MISSING;
            private volatile Future<?> downloadFuture;
            private volatile Instant downloadStartedAt;
            private volatile Instant readyAt;
            private volatile Instant lastPreparedAt;
            private volatile Instant lastAccessAt;
            private volatile long fileBytes;
            private volatile long lastDownloadBytes;
            private volatile String lastError = "";
            private volatile String lastErrorCode = "";

            private CacheEntry(String key, Path filePath) {
                this.key = key;
                this.filePath = filePath;
            }
        }

        private enum EntryStatus {
            MISSING,
            DOWNLOADING,
            READY,
            FAILED
        }

        private record WindowSlice(List<String> lines, int lineEnd, boolean hasMore, int totalLines) {}
    }

    private Map<String, Object> done(Map<String, Object> args, Map<String, Object> resp) {
        String err = tryFormatRemoteError(resp);
        if (err != null) return error(err);
        if (parseBool(args.get("raw"))) return rawSuccess(resp);
        return Map.of("isError", false, "result", resp);
    }

    private Map<String, Object> doneResult(Map<String, Object> args, Map<String, Object> result) {
        result.remove("_itemsListFallbackWarning");
        if (parseBool(args.get("raw"))) return rawSuccess(result);
        return Map.of("isError", false, "result", result);
    }

    private String requireProject(Map<String, Object> args, String op) {
        String p = str(args, "project");
        if (p.isBlank()) throw new IllegalArgumentException("'project' es requerido para " + op);
        return p;
    }

    private String repoId(Map<String, Object> args) {
        String id = str(args, "repositoryId");
        if (id.isBlank()) throw new IllegalArgumentException("'repositoryId' es requerido");
        return id;
    }

    private String repoKey(Map<String, Object> args) {
        String id = str(args, "repositoryId");
        if (!id.isBlank()) return id;
        String name = str(args, "repositoryName");
        if (!name.isBlank()) return name;
        throw new IllegalArgumentException("Debe indicar 'repositoryId' o 'repositoryName'");
    }

    private Map<String, String> baseQuery(Map<String, Object> args) {
        return new LinkedHashMap<>();
    }

    private Map<String, Object> buildRepositoriesListResult(Map<String, Object> args,
                                                             Map<String, Object> resp,
                                                             boolean requirePattern,
                                                             String exactName,
                                                             boolean applyPagination) {
        String project = str(args, "project");
        String pattern = resolveNamePattern(args, requirePattern);
        int skip = applyPagination ? parseSkip(args.get("skip")) : 0;
        Integer top = applyPagination ? parseTop(args.get("top")) : null;

        List<Map<String, Object>> repos = toObjectList(resp.get("value"));
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> repo : repos) {
            if (!matchesContains(repo, pattern)) continue;
            if (!matchesExact(repo, exactName)) continue;
            filtered.add(repo);
        }

        int total = filtered.size();
        int from = Math.min(skip, total);
        int to = top == null ? total : Math.min(total, from + top);
        List<Map<String, Object>> paged = new ArrayList<>(filtered.subList(from, to));

        Integer sourceCount = asIntOrNull(resp.get("count"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("projectScope", project.isBlank() ? "organization" : project);
        result.put("count", paged.size());
        result.put("totalCount", total);
        if (sourceCount != null) result.put("sourceCount", sourceCount);
        result.put("skip", skip);
        if (top != null) result.put("top", top);
        result.put("hasMore", to < total);
        if (!pattern.isBlank()) result.put("nameSearch", pattern);
        if (exactName != null && !exactName.isBlank()) result.put("exactName", exactName);
        result.put("value", paged);
        return result;
    }

    private String projectOrNull(String project) {
        if (project == null || project.isBlank()) return null;
        return project;
    }

    private void validateListLikeInputs(Map<String, Object> args, boolean requirePattern) {
        resolveNamePattern(args, requirePattern);
        parseSkip(args.get("skip"));
        parseTop(args.get("top"));
    }

    private String resolveNamePattern(Map<String, Object> args, boolean required) {
        String contains = str(args, "nameContains");
        String search = str(args, "nameSearch");
        if (!contains.isBlank() && !search.isBlank() && !contains.equalsIgnoreCase(search)) {
            throw new IllegalArgumentException("'nameContains' y 'nameSearch' no deben diferir. Use solo uno o el mismo valor");
        }

        String pattern = search.isBlank() ? contains : search;
        if (required && pattern.isBlank()) {
            throw new IllegalArgumentException("'nameContains' o 'nameSearch' es requerido para search/find");
        }
        return pattern;
    }

    private String exactRepositoryName(Map<String, Object> args) {
        String repositoryName = str(args, "repositoryName");
        String name = str(args, "name");
        String exact = repositoryName.isBlank() ? name : repositoryName;
        if (exact.isBlank()) {
            throw new IllegalArgumentException("'repositoryName' (o 'name') es requerido para get_by_name");
        }
        return exact;
    }

    private boolean matchesContains(Map<String, Object> repo, String pattern) {
        if (pattern == null || pattern.isBlank()) return true;
        String name = Objects.toString(repo.get("name"), "");
        return name.toLowerCase(Locale.ROOT).contains(pattern.toLowerCase(Locale.ROOT));
    }

    private boolean matchesExact(Map<String, Object> repo, String exactName) {
        if (exactName == null || exactName.isBlank()) return true;
        String name = Objects.toString(repo.get("name"), "");
        return name.equalsIgnoreCase(exactName);
    }

    private int parseSkip(Object value) {
        if (value == null || value.toString().isBlank()) return 0;
        Integer n = parseInt(value);
        if (n == null || n < 0) throw new IllegalArgumentException("'skip' debe ser entero >= 0");
        return n;
    }

    private Integer parseTop(Object value) {
        if (value == null || value.toString().isBlank()) return null;
        Integer n = parseInt(value);
        if (n == null || n < 1) throw new IllegalArgumentException("'top' debe ser entero >= 1");
        return n;
    }

    private Integer asIntOrNull(Object value) {
        if (value instanceof Number n) return n.intValue();
        return parseInt(value);
    }

    private int asInt(Object value, int defaultValue) {
        Integer n = asIntOrNull(value);
        return n == null ? defaultValue : n;
    }

    private List<Map<String, Object>> toObjectList(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> row)) continue;
            Map<String, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : row.entrySet()) {
                if (e.getKey() != null) normalized.put(e.getKey().toString(), e.getValue());
            }
            out.add(normalized);
        }
        return out;
    }

    private Map<String, Object> repositorySummary(Map<String, Object> repository) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", repository.get("id"));
        summary.put("name", repository.get("name"));
        summary.put("url", repository.get("url"));
        summary.put("remoteUrl", repository.get("remoteUrl"));
        summary.put("webUrl", repository.get("webUrl"));
        summary.put("defaultBranch", repository.get("defaultBranch"));
        if (repository.containsKey("isDisabled")) summary.put("isDisabled", repository.get("isDisabled"));

        Object projectObj = repository.get("project");
        if (projectObj instanceof Map<?, ?> p) {
            Object projectId = p.get("id");
            Object projectName = p.get("name");
            if (projectId != null) summary.put("projectId", projectId);
            if (projectName != null) summary.put("projectName", projectName);
        }

        return summary;
    }

    private void putVersionDescriptor(Map<String, String> q, Map<String, Object> args) {
        String version = str(args, "version");
        String versionType = str(args, "versionType");
        String versionOptions = str(args, "versionOptions");
        if (!version.isBlank()) q.put("versionDescriptor.version", normalizeBranch(version));
        if (!versionType.isBlank()) q.put("versionDescriptor.versionType", versionType);
        if (!versionOptions.isBlank()) q.put("versionDescriptor.versionOptions", versionOptions);
    }

    private void putIfNotBlank(Map<String, String> q, String key, String value) {
        if (value != null && !value.isBlank()) q.put(key, value);
    }

    private void putBool(Map<String, String> q, String key, Object value) {
        if (value == null) return;
        q.put(key, String.valueOf(parseBool(value)));
    }

    private void putInt(Map<String, String> q, String key, Object value) {
        Integer n = parseInt(value);
        if (n != null) q.put(key, String.valueOf(n));
    }

    private String apiVersion(Map<String, Object> args) {
        String v = str(args, "apiVersion");
        return v.isBlank() ? DEFAULT_API_VERSION : v;
    }

    private String itemsApiVersion(Map<String, Object> args) {
        String v = str(args, "apiVersion");
        return v.isBlank() ? DEFAULT_ITEMS_API_VERSION : v;
    }

    private String pushesApiVersion(Map<String, Object> args) {
        String v = str(args, "apiVersion");
        return v.isBlank() ? DEFAULT_PUSHES_API_VERSION : v;
    }

    private String buildApiVersion(Map<String, Object> args) {
        String v = str(args, "apiVersion");
        return v.isBlank() ? DEFAULT_BUILD_API_VERSION : v;
    }

    private Integer parseInt(Object value) {
        if (value == null) return null;
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String str(Map<String, Object> args, String key) {
        Object v = args.get(key);
        return v == null ? "" : v.toString().trim();
    }

    private boolean parseBool(Object value) {
        if (value instanceof Boolean b) return b;
        if (value == null) return false;
        String s = value.toString().trim().toLowerCase(Locale.ROOT);
        return "true".equals(s) || "1".equals(s) || "yes".equals(s) || "si".equals(s) || "sí".equals(s);
    }

    private String normalizeBranch(String value) {
        if (value == null || value.isBlank()) return value;
        if (value.startsWith("refs/heads/")) return value.substring("refs/heads/".length());
        return value;
    }

    private String normalizeRef(String value) {
        if (value == null || value.isBlank()) return value;
        if (value.startsWith("refs/")) return value;
        if (value.startsWith("heads/")) return "refs/" + value;
        return "refs/heads/" + value;
    }
}
