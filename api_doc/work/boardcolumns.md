
# BoardColumns

## Overview

The BoardColumns API allows you to retrieve the available board columns in a project. Board columns represent the workflow states for work items on a board.

**Official docs:** [BoardColumns](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/boardcolumns?view=azure-devops-rest-7.2)

---

## Endpoint

```
GET https://dev.azure.com/{organization}/{project}/_apis/work/boardcolumns?api-version=7.2-preview.1
```

## Parameters

| Name         | In    | Type   | Required | Description                                      |
|--------------|-------|--------|----------|--------------------------------------------------|
| organization | path  | string | Yes      | Name of the Azure DevOps organization            |
| project      | path  | string | Yes      | Project ID or project name                       |
| api-version  | query | string | Yes      | API version (use `7.2-preview.1`)                |

## Security

OAuth2 (scope: `vso.work`)

## Filters and Properties

No additional filters are available for this endpoint. The response includes all available board columns for the project.

## Example cURL Request

```bash
curl -X GET \
  -H "Authorization: Bearer <YOUR_TOKEN>" \
  "https://dev.azure.com/{organization}/{project}/_apis/work/boardcolumns?api-version=7.2-preview.1"
```

## Example Response

```json
{
  "count": 4,
  "value": [
    { "name": "Active" },
    { "name": "Closed" },
    { "name": "New" },
    { "name": "Resolved" }
  ]
}
```

## Response Properties

- `name`: Name of the board column (e.g., Active, Closed, New, Resolved)

For full property details, see the [official definitions](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/boardcolumns/list?view=azure-devops-rest-7.2#definitions).
