package com.mcp.server.tools.azuredevops.router;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
public class GitRepositoriesTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_git_repositories";
    private static final String DESC = "Operaciones Git Repositories/Code. operation: list|search|find|get_by_name|get|create|update|delete|items_get|items_get_safe|items_list|items_list_recursive|items_batch|search_files|find_files|search_content|explore_repo|commits_list|refs_list|refs_update|pushes_list|pushes_get|pushes_create|download_zip.";
    private static final String DEFAULT_API_VERSION = "7.2-preview.2";
    private static final String DEFAULT_ITEMS_API_VERSION = "7.2-preview.1";
    private static final String DEFAULT_PUSHES_API_VERSION = "7.2-preview.3";
    private static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper();
    private static final int DEFAULT_MAX_FILES = 200;
    private static final int DEFAULT_MAX_BYTES_PER_FILE = 262_144;
    private static final int RECOMMENDED_MAX_FILES = 200;
    private static final int RECOMMENDED_MAX_BYTES_PER_FILE = 262_144;
    private static final int HARD_MAX_FILES = 5_000;
    private static final int HARD_MAX_BYTES_PER_FILE = 2_097_152;

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
                        "items_get", "items_get_safe", "items_list", "items_list_recursive", "items_batch",
                        "search_files", "find_files", "search_content", "explore_repo",
                        "commits_list", "refs_list", "refs_update",
                        "pushes_list", "pushes_get", "pushes_create", "download_zip"
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

    private record LimitSettings(int maxFiles,
                                 int maxBytesPerFile,
                                 boolean customFiles,
                                 boolean customBytes,
                                 boolean clampedFiles,
                                 boolean clampedBytes) {}

    private record ContentRead(boolean readable, String text, String source, boolean truncated) {}

    private record TruncatedText(String text, boolean truncated) {}

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
