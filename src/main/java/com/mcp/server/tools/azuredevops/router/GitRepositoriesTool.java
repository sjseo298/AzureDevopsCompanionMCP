package com.mcp.server.tools.azuredevops.router;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
public class GitRepositoriesTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_git_repositories";
    private static final String DESC = "Operaciones Git Repositories/Code. operation: list|get|create|update|delete|items_get|items_list|items_batch|commits_list|refs_list|refs_update|pushes_list|pushes_get|pushes_create|download_zip.";
    private static final String DEFAULT_API_VERSION = "7.2-preview.2";
    private static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper();

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
                        "list", "get", "create", "update", "delete",
                        "items_get", "items_list", "items_batch",
                        "commits_list", "refs_list", "refs_update",
                        "pushes_list", "pushes_get", "pushes_create", "download_zip"
                ),
                "description", "Operación a ejecutar"
        ));

        props.put("repositoryId", Map.of("type", "string", "description", "ID del repositorio"));
        props.put("repositoryName", Map.of("type", "string", "description", "Nombre del repositorio (create/get alternativo)"));
        props.put("includeLinks", Map.of("type", "boolean", "description", "Incluir _links cuando aplique"));
        props.put("includeAllUrls", Map.of("type", "boolean", "description", "List repos: includeAllUrls"));
        props.put("includeHidden", Map.of("type", "boolean", "description", "List repos: includeHidden"));

        props.put("name", Map.of("type", "string", "description", "Nombre para create/update"));
        props.put("defaultBranch", Map.of("type", "string", "description", "Rama por defecto (refs/heads/main)"));
        props.put("isDisabled", Map.of("type", "boolean", "description", "Deshabilitar repositorio (update)"));

        props.put("path", Map.of("type", "string", "description", "Ruta de item (items_get/items_list/download_zip)"));
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

        props.put("itemDescriptors", Map.of("type", "string", "description", "items_batch: JSON array con item descriptors"));
        props.put("ids", Map.of("type", "string", "description", "IDs CSV para operaciones batch"));

        props.put("top", Map.of("type", "integer", "description", "Límite de resultados"));
        props.put("skip", Map.of("type", "integer", "description", "Offset/paginación"));
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
                case "get" -> opGet(arguments);
                case "create" -> opCreate(arguments);
                case "update" -> opUpdate(arguments);
                case "delete" -> opDelete(arguments);
                case "items_get" -> opItemsGet(arguments);
                case "items_list" -> opItemsList(arguments);
                case "items_batch" -> opItemsBatch(arguments);
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
        if (project.isBlank()) throw new IllegalArgumentException("'project' es requerido para list");
        Map<String, String> q = baseQuery(args);
        putBool(q, "includeLinks", args.get("includeLinks"));
        putBool(q, "includeAllUrls", args.get("includeAllUrls"));
        putBool(q, "includeHidden", args.get("includeHidden"));
        Map<String, Object> resp = azureService.getGitApiWithQuery(project, "repositories", q, apiVersion(args));
        return done(args, resp);
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
        String repo = repoId(args);
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

        Map<String, Object> resp = azureService.getGitApiWithQuery(project, "repositories/" + repo + "/items", q, apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opItemsList(Map<String, Object> args) {
        String project = requireProject(args, "items_list");
        String repo = repoId(args);
        Map<String, String> q = baseQuery(args);
        putIfNotBlank(q, "scopePath", str(args, "scopePath"));
        putIfNotBlank(q, "path", str(args, "path"));
        putIfNotBlank(q, "recursionLevel", str(args, "recursionLevel"));
        putBool(q, "includeContentMetadata", args.get("includeContentMetadata"));
        putBool(q, "latestProcessedChange", args.get("latestProcessedChange"));
        putBool(q, "download", args.get("download"));
        putBool(q, "zipForUnix", args.get("zipForUnix"));
        putVersionDescriptor(q, args);

        Map<String, Object> resp = azureService.getGitApiWithQuery(project, "repositories/" + repo + "/items", q, apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opItemsBatch(Map<String, Object> args) {
        String project = requireProject(args, "items_batch");
        String repo = repoId(args);
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

        Map<String, Object> resp = azureService.postGitApiWithQuery(project, "repositories/" + repo + "/itemsbatch", baseQuery(args), body, apiVersion(args), MediaType.APPLICATION_JSON);
        return done(args, resp);
    }

    private Map<String, Object> opCommitsList(Map<String, Object> args) {
        String project = requireProject(args, "commits_list");
        String repo = repoId(args);
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
        String repo = repoId(args);
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
        String repo = repoId(args);
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
        String repo = repoId(args);
        Map<String, String> q = baseQuery(args);
        putInt(q, "$top", args.get("top"));
        putInt(q, "$skip", args.get("skip"));
        putIfNotBlank(q, "searchCriteria.refName", normalizeRef(str(args, "branch")));
        Map<String, Object> resp = azureService.getGitApiWithQuery(project, "repositories/" + repo + "/pushes", q, apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opPushesGet(Map<String, Object> args) {
        String project = requireProject(args, "pushes_get");
        String repo = repoId(args);
        Object pushId = args.get("pushId");
        if (pushId == null) throw new IllegalArgumentException("'pushId' es requerido para pushes_get");
        Map<String, Object> resp = azureService.getGitApiWithQuery(project, "repositories/" + repo + "/pushes/" + pushId, baseQuery(args), apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opPushesCreate(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "pushes_create");
        String repo = repoId(args);

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
                apiVersion(args),
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
        String repo = repoId(args);
        String path = str(args, "path");
        if (path.isBlank()) path = "/";

        Map<String, String> q = baseQuery(args);
        q.put("scopePath", path);
        q.put("download", "true");
        q.put("$format", "zip");
        putBool(q, "zipForUnix", args.get("zipForUnix"));
        putVersionDescriptor(q, args);

        Map<String, Object> binary = azureService.getGitBinary(project, "repositories/" + repo + "/items", q, apiVersion(args));
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

    private Map<String, Object> done(Map<String, Object> args, Map<String, Object> resp) {
        String err = tryFormatRemoteError(resp);
        if (err != null) return error(err);
        if (parseBool(args.get("raw"))) return rawSuccess(resp);
        return Map.of("isError", false, "result", resp);
    }

    private Map<String, Object> doneResult(Map<String, Object> args, Map<String, Object> result) {
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
