
# BoardParents

## Overview

The BoardParents API returns the list of parent field filter models for a given list of work item IDs. This is useful for understanding parent-child relationships in boards.

**Official docs:** [BoardParents](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/boardparents?view=azure-devops-rest-7.2)

---

## Endpoint

```
GET https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/boardparents?childBacklogContextCategoryRefName={childBacklogContextCategoryRefName}&workitemIds={workitemIds}&api-version=7.2-preview.1
```

## Parameters

| Name                              | In    | Type                | Required | Description                                      |
|----------------------------------- |-------|---------------------|----------|--------------------------------------------------|
| organization                      | path  | string              | Yes      | Name of the Azure DevOps organization            |
| project                           | path  | string              | Yes      | Project ID or project name                       |
| team                              | path  | string              | No       | Team ID or team name                             |
| childBacklogContextCategoryRefName| query | string              | Yes      | Category reference name for the child backlog     |
| workitemIds                       | query | array of int32      | Yes      | List of work item IDs                            |
| api-version                       | query | string              | Yes      | API version (use `7.2-preview.1`)                |

## Security

OAuth2 (scope: `vso.work`)

## Filters and Properties

No additional filters are available for this endpoint. The response includes parent-child mappings for the specified work items.

## Example cURL Request

```bash
curl -X GET \
  -H "Authorization: Bearer <YOUR_TOKEN>" \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/boardparents?childBacklogContextCategoryRefName={childBacklogContextCategoryRefName}&workitemIds=1,2,3&api-version=7.2-preview.1"
```

## Example Response

```json
[
  {
    "id": 1,
    "title": "Parent Work Item",
    "workItemTypeName": "Feature",
    "childWorkItemIds": [2, 3]
  }
]
```

## Response Properties

- `id`: ID of the parent work item
- `title`: Title of the parent work item
- `workItemTypeName`: Type of the parent work item
- `childWorkItemIds`: List of child work item IDs

For full property details, see the [official definitions](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/boardparents/list?view=azure-devops-rest-7.2#definitions).
