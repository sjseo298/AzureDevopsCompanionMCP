# Git Version Coverage Matrix

Scope: routers `azuredevops_git_repositories`, `azuredevops_git_pull_requests`, `azuredevops_git_api`, `azuredevops_git_local`.

## 1) Repositories router

| Router operation | Endpoint family | Default `apiVersion` in router | Catalog/reference | Status |
|---|---|---|---|---|
| `list`, `search`, `find`, `get_by_name`, `get`, `create`, `update`, `delete` | `repositories_*` | `7.2-preview.2` | `repositories_*` (`7.2-preview.2`) | Aligned |
| `items_get`, `items_list`, `items_list_recursive`, `items_get_safe`, content fallback blobs | `items_*`, `trees_get`, `blobs_get_blob` | `7.2-preview.1` | `items_*`, `trees_get`, `blobs_get_blob` (`7.2-preview.1`) | Aligned |
| `items_batch` | `items_get_items_batch` | `7.2-preview.1` | `items_get_items_batch` (`7.2-preview.1`) | Aligned (fixed) |
| `commits_list` | `commits_get_commits` | `7.2-preview.2` | `commits_get_commits` (`7.2-preview.2`) | Aligned |
| `refs_list`, `refs_update` | `refs_list`, `refs_update_refs` | `7.2-preview.2` | `refs_list`, `refs_update_refs` (`7.2-preview.2`) | Aligned |
| `pushes_list`, `pushes_get`, `pushes_create` | `pushes_*` | `7.2-preview.3` | `pushes_*` (`7.2-preview.3`) | Aligned (fixed) |

Notes:
- Router still supports override per call via `apiVersion`.
- Previous mismatch fixed: `items_batch` and blob fallback no longer use `7.2-preview.2` default.

## 2) Pull Requests router

| Router operation family | Default `apiVersion` in router | Catalog/reference |
|---|---|---|
| Core PR endpoints: `get`, `list`, `list_by_project`, `assigned_to_me`, `create`, `update`, `statuses_list`, `status_add`, `iterations_list` | `7.2-preview.2` | `pull_requests_*`, `pull_request_statuses_*`, `pull_request_iterations_list` |
| Legacy PR endpoints: reviewers/threads/comments/labels/`iteration_changes_get`/`work_items_list`/`query`/`share` | `7.2-preview.1` | `pull_request_reviewers_*`, `pull_request_threads_*`, `pull_request_thread_comments_*`, `pull_request_labels_*`, `pull_request_iteration_changes_get`, `pull_request_work_items_list`, `pull_request_query_get`, `pull_request_share_share_pull_request` |

Status: aligned and covered by unit tests, including destructive endpoints mapping.

## 3) Git API router

- Source of truth is `src/main/resources/git-endpoints-7.2.json`.
- Default `apiVersion` is per operation catalog entry; `apiVersion` argument overrides it.
- Status: aligned by design.

## 4) Git local router

| Call site | Current version |
|---|---|
| Metadata fetch `repositories/{idOrName}` | `7.2-preview.2` |

Status: aligned for the endpoint used (`repositories_get_repository`).

## 5) Test/verification state

- Unit tests:
  - `GitRepositoriesToolTest`: family defaults + override precedence.
  - `GitPullRequestsToolTest`: primary/legacy mapping + destructive operations + override precedence.
- Live MCP smoke (read-only):
  - `items_batch`, `pushes_list`, `pushes_get` using router defaults.
