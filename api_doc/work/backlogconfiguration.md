
# BacklogConfiguration

## Overview

The BacklogConfiguration API allows you to retrieve the backlog configuration for a specific team in Azure DevOps. This includes information about task, requirement, and portfolio backlogs, their fields, work item types, and how bugs are managed.

**Official docs:** [BacklogConfiguration](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/backlogconfiguration?view=azure-devops-rest-7.2)

---

## Endpoint

```
GET https://dev.azure.com/{organization}/{project}/{team}/_apis/work/backlogconfiguration?api-version=7.2-preview.1
```

## Parameters

| Name         | In    | Type   | Required | Description                                      |
|--------------|-------|--------|----------|--------------------------------------------------|
| organization | path  | string | Yes      | Name of the Azure DevOps organization            |
| project      | path  | string | Yes      | Project ID or project name                       |
| team         | path  | string | No       | Team ID or team name                             |
| api-version  | query | string | Yes      | API version (use `7.2-preview.1`)                |

## Security

OAuth2 (scope: `vso.work`)

## Filters and Properties

No additional filters are available for this endpoint. The response includes all configuration for the team's backlogs.

## Possible Values (Enums)

### BugsBehavior

- `off`
- `asRequirements`
- `asTasks`

### BacklogType

- `portfolio`
- `requirement`
- `task`

## Example cURL Request

```bash
curl -X GET \
  -H "Authorization: Bearer <YOUR_TOKEN>" \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/backlogconfiguration?api-version=7.2-preview.1"
```

## Example Response

```json
{
  "taskBacklog": { /* ... see full response in official docs ... */ },
  "requirementBacklog": { /* ... */ },
  "portfolioBacklogs": [ /* ... */ ],
  "workItemTypeMappedStates": [ /* ... */ ],
  "backlogFields": {
    "typeFields": {
      "Order": "Microsoft.VSTS.Common.StackRank",
      "Effort": "Microsoft.VSTS.Scheduling.StoryPoints",
      "RemainingWork": "Microsoft.VSTS.Scheduling.RemainingWork",
      "Activity": "Microsoft.VSTS.Common.Activity"
    }
  },
  "bugsBehavior": "asTasks",
  "hiddenBacklogs": ["Microsoft.EpicCategory"],
  "isBugsBehaviorConfigured": true,
  "url": "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/backlogconfiguration"
}
```

## Response Properties

- `taskBacklog`, `requirementBacklog`, `portfolioBacklogs`: Describe configuration for each backlog level (fields, columns, work item types, etc.)
- `workItemTypeMappedStates`: Maps work item states to categories (e.g., New → Proposed)
- `backlogFields`: Maps field types to reference names
- `bugsBehavior`: How bugs are managed in the backlog
- `hiddenBacklogs`: List of hidden backlog categories
- `isBugsBehaviorConfigured`: Whether bug behavior is configured
- `url`: API URL

For full property details, see the [official definitions](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/backlogconfiguration/get?view=azure-devops-rest-7.2&tabs=HTTP#definitions).
