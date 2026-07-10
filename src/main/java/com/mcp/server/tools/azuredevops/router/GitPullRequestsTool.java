package com.mcp.server.tools.azuredevops.router;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.tools.azuredevops.base.AbstractAzureDevOpsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
public class GitPullRequestsTool extends AbstractAzureDevOpsTool {

    private static final String NAME = "azuredevops_git_pull_requests";
    private static final String DESC = "Operaciones Git Pull Requests. operation: get|list|list_by_project|assigned_to_me|create|update|reviewers_list|reviewer_add|reviewer_update|threads_list|thread_create|thread_update|comments_add|comment_update|comment_delete|statuses_list|status_add|labels_list|label_add|label_delete|iterations_list|iteration_changes_get|work_items_list|query|share.";
    private static final String DEFAULT_API_VERSION = "7.2-preview.2";

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    public GitPullRequestsTool(AzureDevOpsClientService svc) {
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
                        "get", "list", "list_by_project", "assigned_to_me",
                        "create", "update",
                        "reviewers_list", "reviewer_add", "reviewer_update",
                        "threads_list", "thread_create", "thread_update",
                        "comments_add", "comment_update", "comment_delete",
                        "statuses_list", "status_add",
                        "labels_list", "label_add", "label_delete",
                        "iterations_list", "iteration_changes_get", "work_items_list",
                        "query", "share"
                ),
                "description", "Operación a ejecutar"
        ));

        props.put("repositoryId", Map.of("type", "string", "description", "ID del repositorio"));
        props.put("pullRequestId", Map.of("type", "integer", "description", "ID del Pull Request"));

        props.put("sourceRefName", Map.of("type", "string", "description", "Rama origen (refs/heads/* o nombre corto)"));
        props.put("targetRefName", Map.of("type", "string", "description", "Rama destino (refs/heads/* o nombre corto)"));
        props.put("title", Map.of("type", "string", "description", "Título del PR"));
        props.put("description", Map.of("type", "string", "description", "Descripción del PR"));
        props.put("status", Map.of("type", "string", "description", "Estado PR (active|abandoned|completed|all)"));

        props.put("top", Map.of("type", "integer", "description", "Límite"));
        props.put("skip", Map.of("type", "integer", "description", "Offset"));
        props.put("creatorId", Map.of("type", "string", "description", "Filtro creatorId (UUID)"));
        props.put("reviewerId", Map.of("type", "string", "description", "Filtro reviewerId (UUID)"));
        props.put("sourceRepositoryId", Map.of("type", "string", "description", "Filtro sourceRepositoryId"));
        props.put("targetBranch", Map.of("type", "string", "description", "Filtro target ref"));
        props.put("sourceBranch", Map.of("type", "string", "description", "Filtro source ref"));

        props.put("reviewerDescriptor", Map.of("type", "string", "description", "Descriptor/ID del reviewer para add/update"));
        props.put("vote", Map.of("type", "integer", "description", "Vote reviewer (-10, -5, 0, 5, 10)"));
        props.put("isRequired", Map.of("type", "boolean", "description", "Reviewer requerido"));

        props.put("threadId", Map.of("type", "integer", "description", "ID del thread"));
        props.put("commentId", Map.of("type", "integer", "description", "ID del comentario"));
        props.put("content", Map.of("type", "string", "description", "Contenido de comentario/thread"));

        props.put("contextFilePath", Map.of("type", "string", "description", "Thread context: ruta archivo"));
        props.put("contextRightFileStartLine", Map.of("type", "integer", "description", "Thread context: línea inicio"));
        props.put("contextRightFileEndLine", Map.of("type", "integer", "description", "Thread context: línea fin"));

        props.put("statusContextName", Map.of("type", "string", "description", "Status context name"));
        props.put("statusContextGenre", Map.of("type", "string", "description", "Status context genre"));
        props.put("statusState", Map.of("type", "string", "description", "pending|succeeded|failed|error|notSet"));
        props.put("statusDescription", Map.of("type", "string", "description", "Descripción de status"));
        props.put("targetUrl", Map.of("type", "string", "description", "URL destino de status"));

        props.put("labelName", Map.of("type", "string", "description", "Nombre de etiqueta"));
        props.put("labelIdOrName", Map.of("type", "string", "description", "Label id o name para delete"));

        props.put("iterationId", Map.of("type", "integer", "description", "ID de iteración"));

        props.put("supportsIterations", Map.of("type", "boolean", "description", "create/update PR supportsIterations"));
        props.put("bodyJson", Map.of("type", "string", "description", "Body JSON crudo opcional para operaciones avanzadas"));

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
                case "get" -> opGet(arguments);
                case "list" -> opList(arguments);
                case "list_by_project" -> opListByProject(arguments);
                case "assigned_to_me" -> opAssignedToMe(arguments);
                case "create" -> opCreate(arguments);
                case "update" -> opUpdate(arguments);
                case "reviewers_list" -> opReviewersList(arguments);
                case "reviewer_add" -> opReviewerAdd(arguments);
                case "reviewer_update" -> opReviewerUpdate(arguments);
                case "threads_list" -> opThreadsList(arguments);
                case "thread_create" -> opThreadCreate(arguments);
                case "thread_update" -> opThreadUpdate(arguments);
                case "comments_add" -> opCommentAdd(arguments);
                case "comment_update" -> opCommentUpdate(arguments);
                case "comment_delete" -> opCommentDelete(arguments);
                case "statuses_list" -> opStatusesList(arguments);
                case "status_add" -> opStatusAdd(arguments);
                case "labels_list" -> opLabelsList(arguments);
                case "label_add" -> opLabelAdd(arguments);
                case "label_delete" -> opLabelDelete(arguments);
                case "iterations_list" -> opIterationsList(arguments);
                case "iteration_changes_get" -> opIterationChangesGet(arguments);
                case "work_items_list" -> opWorkItemsList(arguments);
                case "query" -> opQuery(arguments);
                case "share" -> opShare(arguments);
                default -> error("Operación no soportada: " + op);
            };
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        } catch (Exception e) {
            return error("Error ejecutando git pull requests: " + e.getMessage());
        }
    }

    private Map<String, Object> opGet(Map<String, Object> args) {
        String project = requireProject(args, "get");
        Object prId = requireIntArg(args, "pullRequestId");
        String repo = str(args, "repositoryId");
        Map<String, String> q = baseQuery(args);
        Map<String, Object> resp;
        if (!repo.isBlank()) {
            resp = azureService.getGitApiWithQuery(project, "repositories/" + repo + "/pullrequests/" + prId, q, apiVersion(args));
        } else {
            resp = azureService.getGitApiWithQuery(project, "pullrequests/" + prId, q, apiVersion(args));
        }
        return done(args, resp);
    }

    private Map<String, Object> opList(Map<String, Object> args) {
        String project = requireProject(args, "list");
        String repo = requireRepo(args);
        Map<String, String> q = baseQuery(args);
        putInt(q, "$top", args.get("top"));
        putInt(q, "$skip", args.get("skip"));
        putIfNotBlank(q, "searchCriteria.status", normalizeStatus(str(args, "status")));
        putIfNotBlank(q, "searchCriteria.creatorId", str(args, "creatorId"));
        putIfNotBlank(q, "searchCriteria.reviewerId", str(args, "reviewerId"));
        putIfNotBlank(q, "searchCriteria.sourceRefName", normalizeRef(str(args, "sourceBranch")));
        putIfNotBlank(q, "searchCriteria.targetRefName", normalizeRef(str(args, "targetBranch")));
        putIfNotBlank(q, "searchCriteria.sourceRepositoryId", str(args, "sourceRepositoryId"));
        Map<String, Object> resp = azureService.getGitApiWithQuery(project, "repositories/" + repo + "/pullrequests", q, apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opListByProject(Map<String, Object> args) {
        String project = requireProject(args, "list_by_project");
        Map<String, String> q = baseQuery(args);
        String rawStatus = str(args, "status");
        if ("all".equalsIgnoreCase(rawStatus)) {
            return listByProjectAllStatuses(args, project, q);
        }
        putInt(q, "$top", args.get("top"));
        putInt(q, "$skip", args.get("skip"));
        putIfNotBlank(q, "searchCriteria.status", normalizeStatus(rawStatus));
        putIfNotBlank(q, "searchCriteria.creatorId", str(args, "creatorId"));
        putIfNotBlank(q, "searchCriteria.reviewerId", str(args, "reviewerId"));
        putIfNotBlank(q, "searchCriteria.repositoryId", str(args, "repositoryId"));
        putIfNotBlank(q, "searchCriteria.sourceRepositoryId", str(args, "sourceRepositoryId"));
        putIfNotBlank(q, "searchCriteria.sourceRefName", normalizeRef(str(args, "sourceBranch")));
        putIfNotBlank(q, "searchCriteria.targetRefName", normalizeRef(str(args, "targetBranch")));
        Map<String, Object> resp = azureService.getGitApiWithQuery(project, "pullrequests", q, apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> listByProjectAllStatuses(Map<String, Object> args, String project, Map<String, String> baseQ) {
        List<String> statuses = List.of("active", "completed", "abandoned");
        List<Map<String, Object>> merged = new ArrayList<>();
        int total = 0;

        for (String status : statuses) {
            Map<String, String> q = new LinkedHashMap<>(baseQ);
            putInt(q, "$top", args.get("top"));
            putInt(q, "$skip", args.get("skip"));
            q.put("searchCriteria.status", status);
            putIfNotBlank(q, "searchCriteria.creatorId", str(args, "creatorId"));
            putIfNotBlank(q, "searchCriteria.reviewerId", str(args, "reviewerId"));
            putIfNotBlank(q, "searchCriteria.repositoryId", str(args, "repositoryId"));
            putIfNotBlank(q, "searchCriteria.sourceRepositoryId", str(args, "sourceRepositoryId"));
            putIfNotBlank(q, "searchCriteria.sourceRefName", normalizeRef(str(args, "sourceBranch")));
            putIfNotBlank(q, "searchCriteria.targetRefName", normalizeRef(str(args, "targetBranch")));

            Map<String, Object> part = azureService.getGitApiWithQuery(project, "pullrequests", q, apiVersion(args));
            String err = tryFormatRemoteError(part);
            if (err != null) return error(err);

            Object countObj = part.get("count");
            if (countObj instanceof Number n) total += n.intValue();

            Object valueObj = part.get("value");
            if (valueObj instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (Map.Entry<?, ?> e : m.entrySet()) {
                            if (e.getKey() != null) row.put(e.getKey().toString(), e.getValue());
                        }
                        merged.add(row);
                    }
                }
            }
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("count", total > 0 ? total : merged.size());
        resp.put("value", merged);
        resp.put("statusQueryMode", "all");
        return done(args, resp);
    }

    private Map<String, Object> opAssignedToMe(Map<String, Object> args) {
        String project = requireProject(args, "assigned_to_me");
        Map<String, Object> profile = azureService.getVsspsApi("profile/profiles/me");
        String err = tryFormatRemoteError(profile);
        if (err != null) return error("No se pudo resolver identidad actual: " + err);
        String me = Objects.toString(profile.get("id"), "");
        if (me.isBlank()) return error("No se pudo resolver reviewerId del usuario autenticado");

        Map<String, Object> copy = new LinkedHashMap<>(args);
        copy.put("reviewerId", me);
        if (str(copy, "status").isBlank()) copy.put("status", "active");
        return opListByProject(copy);
    }

    private Map<String, Object> opCreate(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "create");
        String repo = requireRepo(args);
        Map<String, Object> body = parseBodyJsonIfPresent(args);
        if (body.isEmpty()) {
            String source = normalizeRef(requireString(args, "sourceRefName"));
            String target = normalizeRef(requireString(args, "targetRefName"));
            String title = requireString(args, "title");
            body.put("sourceRefName", source);
            body.put("targetRefName", target);
            body.put("title", title);
            putBody(body, "description", str(args, "description"));
            if (args.get("supportsIterations") != null) body.put("supportsIterations", parseBool(args.get("supportsIterations")));
        }
        Map<String, Object> resp = azureService.postGitApiWithQuery(project, "repositories/" + repo + "/pullrequests", baseQuery(args), body, apiVersion(args), MediaType.APPLICATION_JSON);
        return done(args, resp);
    }

    private Map<String, Object> opUpdate(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "update");
        String repo = requireRepo(args);
        Object prId = requireIntArg(args, "pullRequestId");
        Map<String, Object> body = parseBodyJsonIfPresent(args);
        if (body.isEmpty()) {
            putBody(body, "title", str(args, "title"));
            putBody(body, "description", str(args, "description"));
            if (!str(args, "status").isBlank()) body.put("status", normalizeStatus(str(args, "status")));
            if (args.get("supportsIterations") != null) body.put("supportsIterations", parseBool(args.get("supportsIterations")));
        }
        if (body.isEmpty()) throw new IllegalArgumentException("No hay campos para update");

        Map<String, Object> resp = azureService.patchGitApiWithQuery(project, "repositories/" + repo + "/pullrequests/" + prId, baseQuery(args), body, apiVersion(args), MediaType.APPLICATION_JSON);
        return done(args, resp);
    }

    private Map<String, Object> opReviewersList(Map<String, Object> args) {
        String project = requireProject(args, "reviewers_list");
        String repo = requireRepo(args);
        Object prId = requireIntArg(args, "pullRequestId");
        Map<String, Object> resp = azureService.getGitApiWithQuery(project, "repositories/" + repo + "/pullRequests/" + prId + "/reviewers", baseQuery(args), apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opReviewerAdd(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "reviewer_add");
        String repo = requireRepo(args);
        Object prId = requireIntArg(args, "pullRequestId");
        String reviewer = requireString(args, "reviewerDescriptor");

        Map<String, Object> body = parseBodyJsonIfPresent(args);
        if (body.isEmpty()) {
            body.put("id", reviewer);
            if (args.get("vote") != null) body.put("vote", parseInt(args.get("vote")));
            if (args.get("isRequired") != null) body.put("isRequired", parseBool(args.get("isRequired")));
        }

        Map<String, Object> resp = azureService.putGitApiWithQuery(
                project,
                "repositories/" + repo + "/pullRequests/" + prId + "/reviewers/" + reviewer,
                baseQuery(args),
                body,
                apiVersion(args),
                MediaType.APPLICATION_JSON
        );
        return done(args, resp);
    }

    private Map<String, Object> opReviewerUpdate(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "reviewer_update");
        String repo = requireRepo(args);
        Object prId = requireIntArg(args, "pullRequestId");
        String reviewer = requireString(args, "reviewerDescriptor");

        Map<String, Object> body = parseBodyJsonIfPresent(args);
        if (body.isEmpty()) {
            if (args.get("vote") != null) body.put("vote", parseInt(args.get("vote")));
            if (args.get("isRequired") != null) body.put("isRequired", parseBool(args.get("isRequired")));
        }
        if (body.isEmpty()) throw new IllegalArgumentException("Debe indicar voto o bodyJson para reviewer_update");

        Map<String, Object> resp = azureService.patchGitApiWithQuery(
                project,
                "repositories/" + repo + "/pullRequests/" + prId + "/reviewers/" + reviewer,
                baseQuery(args),
                body,
                apiVersion(args),
                MediaType.APPLICATION_JSON
        );
        return done(args, resp);
    }

    private Map<String, Object> opThreadsList(Map<String, Object> args) {
        String project = requireProject(args, "threads_list");
        String repo = requireRepo(args);
        Object prId = requireIntArg(args, "pullRequestId");
        Map<String, Object> resp = azureService.getGitApiWithQuery(project, "repositories/" + repo + "/pullRequests/" + prId + "/threads", baseQuery(args), apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opThreadCreate(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "thread_create");
        String repo = requireRepo(args);
        Object prId = requireIntArg(args, "pullRequestId");

        Map<String, Object> body = parseBodyJsonIfPresent(args);
        if (body.isEmpty()) {
            String content = requireString(args, "content");
            body.put("comments", List.of(Map.of("parentCommentId", 0, "content", content, "commentType", 1)));
            Map<String, Object> context = buildThreadContext(args);
            if (!context.isEmpty()) body.put("threadContext", context);
            body.put("status", "active");
        }

        Map<String, Object> resp = azureService.postGitApiWithQuery(project, "repositories/" + repo + "/pullRequests/" + prId + "/threads", baseQuery(args), body, apiVersion(args), MediaType.APPLICATION_JSON);
        return done(args, resp);
    }

    private Map<String, Object> opThreadUpdate(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "thread_update");
        String repo = requireRepo(args);
        Object prId = requireIntArg(args, "pullRequestId");
        Object threadId = requireIntArg(args, "threadId");

        Map<String, Object> body = parseBodyJsonIfPresent(args);
        if (body.isEmpty()) {
            if (!str(args, "status").isBlank()) body.put("status", str(args, "status"));
            if (body.isEmpty()) throw new IllegalArgumentException("thread_update requiere status o bodyJson");
        }

        Map<String, Object> resp = azureService.patchGitApiWithQuery(project, "repositories/" + repo + "/pullRequests/" + prId + "/threads/" + threadId, baseQuery(args), body, apiVersion(args), MediaType.APPLICATION_JSON);
        return done(args, resp);
    }

    private Map<String, Object> opCommentAdd(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "comments_add");
        String repo = requireRepo(args);
        Object prId = requireIntArg(args, "pullRequestId");
        Object threadId = requireIntArg(args, "threadId");

        Map<String, Object> body = parseBodyJsonIfPresent(args);
        if (body.isEmpty()) {
            String content = requireString(args, "content");
            body.put("content", content);
            body.put("commentType", 1);
        }

        Map<String, Object> resp = azureService.postGitApiWithQuery(project, "repositories/" + repo + "/pullRequests/" + prId + "/threads/" + threadId + "/comments", baseQuery(args), body, apiVersion(args), MediaType.APPLICATION_JSON);
        return done(args, resp);
    }

    private Map<String, Object> opCommentUpdate(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "comment_update");
        String repo = requireRepo(args);
        Object prId = requireIntArg(args, "pullRequestId");
        Object threadId = requireIntArg(args, "threadId");
        Object commentId = requireIntArg(args, "commentId");

        Map<String, Object> body = parseBodyJsonIfPresent(args);
        if (body.isEmpty()) {
            String content = requireString(args, "content");
            body.put("content", content);
        }

        Map<String, Object> resp = azureService.patchGitApiWithQuery(project, "repositories/" + repo + "/pullRequests/" + prId + "/threads/" + threadId + "/comments/" + commentId, baseQuery(args), body, apiVersion(args), MediaType.APPLICATION_JSON);
        return done(args, resp);
    }

    private Map<String, Object> opCommentDelete(Map<String, Object> args) {
        String project = requireProject(args, "comment_delete");
        String repo = requireRepo(args);
        Object prId = requireIntArg(args, "pullRequestId");
        Object threadId = requireIntArg(args, "threadId");
        Object commentId = requireIntArg(args, "commentId");
        Map<String, Object> resp = azureService.deleteGitApiWithQuery(project, "repositories/" + repo + "/pullRequests/" + prId + "/threads/" + threadId + "/comments/" + commentId, baseQuery(args), apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opStatusesList(Map<String, Object> args) {
        String project = requireProject(args, "statuses_list");
        String repo = requireRepo(args);
        Object prId = requireIntArg(args, "pullRequestId");
        Map<String, Object> resp = azureService.getGitApiWithQuery(project, "repositories/" + repo + "/pullRequests/" + prId + "/statuses", baseQuery(args), apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opStatusAdd(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "status_add");
        String repo = requireRepo(args);
        Object prId = requireIntArg(args, "pullRequestId");

        Map<String, Object> body = parseBodyJsonIfPresent(args);
        if (body.isEmpty()) {
            String contextName = requireString(args, "statusContextName");
            String contextGenre = requireString(args, "statusContextGenre");
            String state = normalizeStatusState(requireString(args, "statusState"));
            body.put("state", state);
            body.put("description", str(args, "statusDescription"));
            putBody(body, "targetUrl", str(args, "targetUrl"));
            body.put("context", Map.of("name", contextName, "genre", contextGenre));
        }

        Map<String, Object> resp = azureService.postGitApiWithQuery(project, "repositories/" + repo + "/pullRequests/" + prId + "/statuses", baseQuery(args), body, apiVersion(args), MediaType.APPLICATION_JSON);
        return done(args, resp);
    }

    private Map<String, Object> opLabelsList(Map<String, Object> args) {
        String project = requireProject(args, "labels_list");
        String repo = requireRepo(args);
        Object prId = requireIntArg(args, "pullRequestId");
        Map<String, Object> resp = azureService.getGitApiWithQuery(project, "repositories/" + repo + "/pullRequests/" + prId + "/labels", baseQuery(args), apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opLabelAdd(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "label_add");
        String repo = requireRepo(args);
        Object prId = requireIntArg(args, "pullRequestId");

        Map<String, Object> body = parseBodyJsonIfPresent(args);
        if (body.isEmpty()) {
            String labelName = requireString(args, "labelName");
            body.put("name", labelName);
            putBody(body, "active", true);
        }

        Map<String, Object> resp = azureService.postGitApiWithQuery(project, "repositories/" + repo + "/pullRequests/" + prId + "/labels", baseQuery(args), body, apiVersion(args), MediaType.APPLICATION_JSON);
        return done(args, resp);
    }

    private Map<String, Object> opLabelDelete(Map<String, Object> args) {
        String project = requireProject(args, "label_delete");
        String repo = requireRepo(args);
        Object prId = requireIntArg(args, "pullRequestId");
        String label = str(args, "labelIdOrName");
        if (label.isBlank()) label = str(args, "labelName");
        if (label.isBlank()) throw new IllegalArgumentException("'labelIdOrName' o 'labelName' es requerido");
        Map<String, Object> resp = azureService.deleteGitApiWithQuery(project, "repositories/" + repo + "/pullRequests/" + prId + "/labels/" + label, baseQuery(args), apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opIterationsList(Map<String, Object> args) {
        String project = requireProject(args, "iterations_list");
        String repo = requireRepo(args);
        Object prId = requireIntArg(args, "pullRequestId");
        Map<String, Object> resp = azureService.getGitApiWithQuery(project, "repositories/" + repo + "/pullRequests/" + prId + "/iterations", baseQuery(args), apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opIterationChangesGet(Map<String, Object> args) {
        String project = requireProject(args, "iteration_changes_get");
        String repo = requireRepo(args);
        Object prId = requireIntArg(args, "pullRequestId");
        Object iterationId = requireIntArg(args, "iterationId");
        Map<String, String> q = baseQuery(args);
        putInt(q, "$top", args.get("top"));
        putInt(q, "$skip", args.get("skip"));
        Map<String, Object> resp = azureService.getGitApiWithQuery(project, "repositories/" + repo + "/pullRequests/" + prId + "/iterations/" + iterationId + "/changes", q, apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opWorkItemsList(Map<String, Object> args) {
        String project = requireProject(args, "work_items_list");
        String repo = requireRepo(args);
        Object prId = requireIntArg(args, "pullRequestId");
        Map<String, Object> resp = azureService.getGitApiWithQuery(project, "repositories/" + repo + "/pullRequests/" + prId + "/workitems", baseQuery(args), apiVersion(args));
        return done(args, resp);
    }

    private Map<String, Object> opQuery(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "query");
        String repo = requireRepo(args);
        Map<String, Object> body = parseBodyJsonIfPresent(args);
        if (body.isEmpty()) {
            throw new IllegalArgumentException("'bodyJson' es requerido para query");
        }
        Map<String, Object> resp = azureService.postGitApiWithQuery(project, "repositories/" + repo + "/pullrequestquery", baseQuery(args), body, apiVersion(args), MediaType.APPLICATION_JSON);
        return done(args, resp);
    }

    private Map<String, Object> opShare(Map<String, Object> args) throws Exception {
        String project = requireProject(args, "share");
        String repo = requireRepo(args);
        Object prId = requireIntArg(args, "pullRequestId");

        Map<String, Object> body = parseBodyJsonIfPresent(args);
        if (body.isEmpty()) {
            throw new IllegalArgumentException("'bodyJson' es requerido para share (recipients)");
        }
        Map<String, Object> resp = azureService.postGitApiWithQuery(project, "repositories/" + repo + "/pullRequests/" + prId + "/share", baseQuery(args), body, apiVersion(args), MediaType.APPLICATION_JSON);
        return done(args, resp);
    }

    private Map<String, Object> done(Map<String, Object> args, Map<String, Object> resp) {
        String err = tryFormatRemoteError(resp);
        if (err != null) return error(err);
        if (parseBool(args.get("raw"))) return rawSuccess(resp);
        return Map.of("isError", false, "result", resp);
    }

    private String apiVersion(Map<String, Object> args) {
        String v = str(args, "apiVersion");
        return v.isBlank() ? DEFAULT_API_VERSION : v;
    }

    private Map<String, String> baseQuery(Map<String, Object> args) {
        return new LinkedHashMap<>();
    }

    private void putInt(Map<String, String> q, String key, Object value) {
        Integer n = parseInt(value);
        if (n != null) q.put(key, String.valueOf(n));
    }

    private void putIfNotBlank(Map<String, String> q, String key, String value) {
        if (value != null && !value.isBlank()) q.put(key, value);
    }

    private String requireProject(Map<String, Object> args, String op) {
        String p = str(args, "project");
        if (p.isBlank()) throw new IllegalArgumentException("'project' es requerido para " + op);
        return p;
    }

    private String requireRepo(Map<String, Object> args) {
        String r = str(args, "repositoryId");
        if (r.isBlank()) throw new IllegalArgumentException("'repositoryId' es requerido");
        return r;
    }

    private Object requireIntArg(Map<String, Object> args, String key) {
        Object v = args.get(key);
        if (v == null) throw new IllegalArgumentException("'" + key + "' es requerido");
        Integer n = parseInt(v);
        if (n == null) throw new IllegalArgumentException("'" + key + "' debe ser numérico");
        return n;
    }

    private String requireString(Map<String, Object> args, String key) {
        String v = str(args, key);
        if (v.isBlank()) throw new IllegalArgumentException("'" + key + "' es requerido");
        return v;
    }

    private void putBody(Map<String, Object> body, String key, Object value) {
        if (value == null) return;
        if (value instanceof String s) {
            if (!s.isBlank()) body.put(key, s);
            return;
        }
        body.put(key, value);
    }

    private Map<String, Object> parseBodyJsonIfPresent(Map<String, Object> args) throws Exception {
        String bodyJson = str(args, "bodyJson");
        if (bodyJson.isBlank()) return new LinkedHashMap<>();
        Object parsed = JSON.readValue(bodyJson, Object.class);
        if (!(parsed instanceof Map<?, ?> m)) {
            throw new IllegalArgumentException("'bodyJson' debe ser un objeto JSON");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : m.entrySet()) {
            if (e.getKey() != null) out.put(e.getKey().toString(), e.getValue());
        }
        return out;
    }

    private Map<String, Object> buildThreadContext(Map<String, Object> args) {
        String filePath = str(args, "contextFilePath");
        Integer start = parseInt(args.get("contextRightFileStartLine"));
        Integer end = parseInt(args.get("contextRightFileEndLine"));
        if (filePath.isBlank() || start == null || end == null) return Map.of();
        Map<String, Object> posStart = Map.of("line", start, "offset", 1);
        Map<String, Object> posEnd = Map.of("line", end, "offset", 1);
        Map<String, Object> range = new LinkedHashMap<>();
        range.put("start", posStart);
        range.put("end", posEnd);
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("filePath", filePath);
        context.put("rightFileStart", posStart);
        context.put("rightFileEnd", posEnd);
        context.put("rightFileRange", range);
        return context;
    }

    private String normalizeRef(String value) {
        if (value == null || value.isBlank()) return value;
        if (value.startsWith("refs/")) return value;
        if (value.startsWith("heads/")) return "refs/" + value;
        return "refs/heads/" + value;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return status;
        String s = status.toLowerCase(Locale.ROOT);
        return switch (s) {
            case "active" -> "active";
            case "abandoned" -> "abandoned";
            case "completed" -> "completed";
            case "all" -> "all";
            default -> status;
        };
    }

    private String normalizeStatusState(String state) {
        String s = state.toLowerCase(Locale.ROOT);
        return switch (s) {
            case "pending" -> "pending";
            case "succeeded" -> "succeeded";
            case "failed" -> "failed";
            case "error" -> "error";
            case "notset" -> "notSet";
            default -> state;
        };
    }

    private String str(Map<String, Object> args, String key) {
        Object v = args.get(key);
        return v == null ? "" : v.toString().trim();
    }

    private Integer parseInt(Object value) {
        if (value == null) return null;
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean parseBool(Object value) {
        if (value instanceof Boolean b) return b;
        if (value == null) return false;
        String s = value.toString().trim().toLowerCase(Locale.ROOT);
        return "true".equals(s) || "1".equals(s) || "yes".equals(s) || "si".equals(s) || "sí".equals(s);
    }
}
