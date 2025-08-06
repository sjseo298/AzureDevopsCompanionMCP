
# Backlogs

## Overview

The Backlogs API provides access to backlog levels in Azure DevOps, allowing you to list all backlog levels, get a specific backlog level, and retrieve work items within a backlog level.

**Official docs:** [Backlogs](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/backlogs?view=azure-devops-rest-7.2)

---

## Endpoints

### List all backlog levels
```
GET https://dev.azure.com/{organization}/{project}/{team}/_apis/work/backlogs?api-version=7.2-preview.1
```

### Get a backlog level
```
GET https://dev.azure.com/{organization}/{project}/{team}/_apis/work/backlogs/{id}?api-version=7.2-preview.1
```

### Get work items in a backlog level
```
GET https://dev.azure.com/{organization}/{project}/{team}/_apis/work/backlogs/{id}/workItems?api-version=7.2-preview.1
```

## Parameters (common)

| Name         | In    | Type   | Required | Description                                      |
|--------------|-------|--------|----------|--------------------------------------------------|
| organization | path  | string | Yes      | Name of the Azure DevOps organization            |
| project      | path  | string | Yes      | Project ID or project name                       |
| team         | path  | string | No       | Team ID or team name                             |
| id           | path  | string | For single backlog endpoints | Backlog level ID |
| api-version  | query | string | Yes      | API version (use `7.2-preview.1`)                |

## Security

OAuth2 (scope: `vso.work`)

## Filters and Properties

No additional filters are available for these endpoints. The response includes all backlog levels or work items for the specified backlog.

## Example cURL Request (List all backlog levels)

```bash
curl -X GET \
  -H "Authorization: Bearer <YOUR_TOKEN>" \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/backlogs?api-version=7.2-preview.1"
```

## Example Response (List all backlog levels)

```json
{
  "count": 3,
  "value": [
    {
      "id": "Microsoft.EpicCategory",
      "name": "Epics",
      "rank": 1,
      "workItemTypes": [
        { "name": "Epic" }
      ]
    },
    {
      "id": "Microsoft.FeatureCategory",
      "name": "Features",
      "rank": 2,
      "workItemTypes": [
        { "name": "Feature" }
      ]
    }
  ]
}
```

## Response Properties

- `id`: Backlog level ID (e.g., Microsoft.EpicCategory)
- `name`: Name of the backlog level
- `rank`: Order of the backlog level
- `workItemTypes`: Work item types associated with this backlog

For full property details, see the [official definitions](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/backlogs?view=azure-devops-rest-7.2#definitions).
