
# Advanced Security API (Azure DevOps REST API v7.2)

This section provides a comprehensive English-language reference for the Advanced Security API area in Azure DevOps REST API v7.2. Each sub-area is documented with its purpose, available operations, required and optional parameters, cURL examples, and sample request/response bodies.

## Sub-Areas

- [Alerts](https://learn.microsoft.com/en-us/rest/api/azure/devops/advancedsecurity/alerts?view=azure-devops-rest-7.2)
- [Analysis](https://learn.microsoft.com/en-us/rest/api/azure/devops/advancedsecurity/analysis?view=azure-devops-rest-7.2)
- [Instances](https://learn.microsoft.com/en-us/rest/api/azure/devops/advancedsecurity/instances?view=azure-devops-rest-7.2)
- [Meter Usage](https://learn.microsoft.com/en-us/rest/api/azure/devops/advancedsecurity/meter-usage?view=azure-devops-rest-7.2)
- [Org Enablement](https://learn.microsoft.com/en-us/rest/api/azure/devops/advancedsecurity/org-enablement?view=azure-devops-rest-7.2)
- [Org Meter Usage Estimate](https://learn.microsoft.com/en-us/rest/api/azure/devops/advancedsecurity/org-meter-usage-estimate?view=azure-devops-rest-7.2)
- [Project Enablement](https://learn.microsoft.com/en-us/rest/api/azure/devops/advancedsecurity/project-enablement?view=azure-devops-rest-7.2)
- [Project Meter Usage Estimate](https://learn.microsoft.com/en-us/rest/api/azure/devops/advancedsecurity/project-meter-usage-estimate?view=azure-devops-rest-7.2)
- [Repo Enablement](https://learn.microsoft.com/en-us/rest/api/azure/devops/advancedsecurity/repo-enablement?view=azure-devops-rest-7.2)
- [Repo Meter Usage Estimate](https://learn.microsoft.com/en-us/rest/api/azure/devops/advancedsecurity/repo-meter-usage-estimate?view=azure-devops-rest-7.2)
- [Summary Dashboard](https://learn.microsoft.com/en-us/rest/api/azure/devops/advancedsecurity/summary-dashboard?view=azure-devops-rest-7.2)

---










## Documentation Progress

- [x] Structure and sub-area listing
- [x] Alerts
- [x] Analysis
- [x] Instances
- [x] Meter Usage
- [x] Org Enablement
- [x] Org Meter Usage Estimate
- [x] Project Enablement
- [x] Project Meter Usage Estimate
- [x] Repo Enablement
- [x] Repo Meter Usage Estimate
- [x] Summary Dashboard

---

## Alerts
...existing code...

---

## Analysis
...existing code...

---

## Instances
...existing code...

---

## Meter Usage
...existing code...

---

## Org Enablement
...existing code...

---

## Org Meter Usage Estimate
...existing code...

---

## Project Enablement
...existing code...

---

## Project Meter Usage Estimate
...existing code...

---

## Repo Enablement

**Purpose:**
The Repo Enablement API in Azure DevOps Advanced Security provides endpoints to get and update the enablement status of Code Security, Secret Protection, and their features for a specific repository. This allows organizations to manage security features at the repository level.

**Operations:**

| Operation | Description |
|-----------|-------------|
| Get       | Determines if Code Security, Secret Protection, and their features are enabled for the repository. |
| Update    | Update the enablement status of Code Security and Secret Protection, along with their respective features, for a given repository. |

---

### Get Repo Enablement

- **Description:** Retrieve the enablement status of Code Security, Secret Protection, and their features for a repository.
- **Method:** GET
- **Endpoint:**
  `/organizations/{organization}/_apis/advancedsecurity/repo-enablement?repositoryId={repositoryId}&api-version=7.2-preview.1`
- **Parameters:**
  - `organization` (string, required): The name of the Azure DevOps organization.
  - `repositoryId` (string, required): The ID of the repository.
  - `api-version` (string, required): The API version (use `7.2-preview.1`).

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://dev.azure.com/{organization}/_apis/advancedsecurity/repo-enablement?repositoryId={repositoryId}&api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "codeSecurityEnabled": true,
  "secretProtectionEnabled": true,
  "features": {
    "dependencyScanning": true,
    "secretScanning": true
  },
  "lastUpdated": "2025-08-01T12:00:00Z"
}
```

---

### Update Repo Enablement

- **Description:** Update the enablement status of Code Security, Secret Protection, and their features for a repository.
- **Method:** PATCH
- **Endpoint:**
  `/organizations/{organization}/_apis/advancedsecurity/repo-enablement?repositoryId={repositoryId}&api-version=7.2-preview.1`
- **Parameters:**
  - `organization` (string, required): The name of the Azure DevOps organization.
  - `repositoryId` (string, required): The ID of the repository.
  - `api-version` (string, required): The API version (use `7.2-preview.1`).
- **Body:**
  - `codeSecurityEnabled` (boolean, optional): Enable or disable Code Security.
  - `secretProtectionEnabled` (boolean, optional): Enable or disable Secret Protection.
  - `features` (object, optional): Enable or disable specific features (e.g., `dependencyScanning`, `secretScanning`).

**Example cURL:**
```bash
curl -X PATCH \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"codeSecurityEnabled": true, "secretProtectionEnabled": true, "features": {"dependencyScanning": true, "secretScanning": true}}' \
  "https://dev.azure.com/{organization}/_apis/advancedsecurity/repo-enablement?repositoryId={repositoryId}&api-version=7.2-preview.1"
```

**Sample Request Body:**
```json
{
  "codeSecurityEnabled": true,
  "secretProtectionEnabled": true,
  "features": {
    "dependencyScanning": true,
    "secretScanning": true
  }
}
```

**Sample Response:**
```json
{
  "codeSecurityEnabled": true,
  "secretProtectionEnabled": true,
  "features": {
    "dependencyScanning": true,
    "secretScanning": true
  },
  "lastUpdated": "2025-08-01T12:34:56Z"
}
```

---

## Repo Meter Usage Estimate

**Purpose:**
The Repo Meter Usage Estimate API in Azure DevOps Advanced Security provides an endpoint to estimate the number of pushers (committers) that would be added to a repository's usage if Advanced Security were enabled. This helps organizations forecast billing impact before enabling the feature at the repository level.

**Operations:**

| Operation | Description |
|-----------|-------------|
| Get       | Estimate the pushers that would be added to the customer's usage if Advanced Security was enabled for this repository. |

---

### Get Repo Meter Usage Estimate

- **Description:** Estimate the number of pushers that would be counted towards billing if Advanced Security were enabled for the repository.
- **Method:** GET
- **Endpoint:**
  `/organizations/{organization}/_apis/advancedsecurity/repo-meter-usage-estimate?repositoryId={repositoryId}&api-version=7.2-preview.1`
- **Parameters:**
  - `organization` (string, required): The name of the Azure DevOps organization.
  - `repositoryId` (string, required): The ID of the repository.
  - `api-version` (string, required): The API version (use `7.2-preview.1`).

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://dev.azure.com/{organization}/_apis/advancedsecurity/repo-meter-usage-estimate?repositoryId={repositoryId}&api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "estimatedPushers": 5,
  "details": [
    {
      "committerId": "user5",
      "displayName": "Eve Adams",
      "email": "eve@example.com"
    },
    {
      "committerId": "user6",
      "displayName": "Frank Green",
      "email": "frank@example.com"
    }
  ]
}
```

---

## Summary Dashboard

**Purpose:**
The Summary Dashboard API in Azure DevOps Advanced Security provides an endpoint to retrieve a summary of Advanced Security status and usage for an organization. This dashboard aggregates key metrics and feature enablement states, helping organizations monitor their security posture at a glance.

**Operations:**

| Operation | Description |
|-----------|-------------|
| Get       | Get a summary dashboard of Advanced Security status and usage for the organization. |

---

### Get Summary Dashboard

- **Description:** Retrieve a summary dashboard of Advanced Security status, feature enablement, and usage metrics for the organization.
- **Method:** GET
- **Endpoint:**
  `/organizations/{organization}/_apis/advancedsecurity/summary-dashboard?api-version=7.2-preview.1`
- **Parameters:**
  - `organization` (string, required): The name of the Azure DevOps organization.
  - `api-version` (string, required): The API version (use `7.2-preview.1`).

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://dev.azure.com/{organization}/_apis/advancedsecurity/summary-dashboard?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "organization": "contoso",
  "advancedSecurityEnabled": true,
  "totalProjects": 8,
  "projectsWithAdvancedSecurity": 5,
  "totalRepositories": 20,
  "repositoriesWithAdvancedSecurity": 12,
  "totalPushers": 50,
  "features": {
    "dependencyScanning": true,
    "secretScanning": true
  },
  "lastUpdated": "2025-08-01T13:00:00Z"
}
```

**Purpose:**
The Project Meter Usage Estimate API in Azure DevOps Advanced Security provides an endpoint to estimate the number of pushers (committers) that would be added to a project's usage if Advanced Security were enabled. This helps organizations forecast billing impact before enabling the feature at the project level.

**Operations:**

| Operation | Description |
|-----------|-------------|
| Get       | Estimate the pushers that would be added to the customer's usage if Advanced Security was enabled for this project. |

---

### Get Project Meter Usage Estimate

- **Description:** Estimate the number of pushers that would be counted towards billing if Advanced Security were enabled for the project.
- **Method:** GET
- **Endpoint:**
  `/organizations/{organization}/_apis/advancedsecurity/project-meter-usage-estimate?projectId={projectId}&api-version=7.2-preview.1`
- **Parameters:**
  - `organization` (string, required): The name of the Azure DevOps organization.
  - `projectId` (string, required): The ID of the project.
  - `api-version` (string, required): The API version (use `7.2-preview.1`).

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://dev.azure.com/{organization}/_apis/advancedsecurity/project-meter-usage-estimate?projectId={projectId}&api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "estimatedPushers": 12,
  "details": [
    {
      "committerId": "user3",
      "displayName": "Carol White",
      "email": "carol@example.com"
    },
    {
      "committerId": "user4",
      "displayName": "Dan Black",
      "email": "dan@example.com"
    }
  ]
}
```

**Purpose:**
The Project Enablement API in Azure DevOps Advanced Security provides endpoints to get and update the enablement status of Advanced Security features for a specific project. This allows organizations to manage Advanced Security at the project level.

**Operations:**

| Operation | Description |
|-----------|-------------|
| Get       | Get the current status of Advanced Security for a project. |
| Update    | Update the status of Advanced Security for the project. |

---

### Get Project Enablement

- **Description:** Retrieve the current enablement status of Advanced Security for a project.
- **Method:** GET
- **Endpoint:**
  `/organizations/{organization}/_apis/advancedsecurity/project-enablement?projectId={projectId}&api-version=7.2-preview.1`
- **Parameters:**
  - `organization` (string, required): The name of the Azure DevOps organization.
  - `projectId` (string, required): The ID of the project.
  - `api-version` (string, required): The API version (use `7.2-preview.1`).

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://dev.azure.com/{organization}/_apis/advancedsecurity/project-enablement?projectId={projectId}&api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "enabled": true,
  "lastUpdated": "2025-08-01T12:00:00Z"
}
```

---

### Update Project Enablement

- **Description:** Update (enable or disable) Advanced Security for a project.
- **Method:** PATCH
- **Endpoint:**
  `/organizations/{organization}/_apis/advancedsecurity/project-enablement?projectId={projectId}&api-version=7.2-preview.1`
- **Parameters:**
  - `organization` (string, required): The name of the Azure DevOps organization.
  - `projectId` (string, required): The ID of the project.
  - `api-version` (string, required): The API version (use `7.2-preview.1`).
- **Body:**
  - `enabled` (boolean, required): Whether to enable (`true`) or disable (`false`) Advanced Security for the project.

**Example cURL:**
```bash
curl -X PATCH \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"enabled": true}' \
  "https://dev.azure.com/{organization}/_apis/advancedsecurity/project-enablement?projectId={projectId}&api-version=7.2-preview.1"
```

**Sample Request Body:**
```json
{
  "enabled": true
}
```

**Sample Response:**
```json
{
  "enabled": true,
  "lastUpdated": "2025-08-01T12:34:56Z"
}
```

**Purpose:**
The Org Meter Usage Estimate API in Azure DevOps Advanced Security provides an endpoint to estimate the number of pushers (committers) that would be added to the organization's usage if Advanced Security were enabled. This helps organizations forecast billing impact before enabling the feature.

**Operations:**

| Operation | Description |
|-----------|-------------|
| Get       | Estimate the pushers that would be added to the customer's usage if Advanced Security was enabled for this organization. |

---

### Get Org Meter Usage Estimate

- **Description:** Estimate the number of pushers that would be counted towards billing if Advanced Security were enabled for the organization.
- **Method:** GET
- **Endpoint:**
  `/organizations/{organization}/_apis/advancedsecurity/org-meter-usage-estimate?api-version=7.2-preview.1`
- **Parameters:**
  - `organization` (string, required): The name of the Azure DevOps organization.
  - `api-version` (string, required): The API version (use `7.2-preview.1`).

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://dev.azure.com/{organization}/_apis/advancedsecurity/org-meter-usage-estimate?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "estimatedPushers": 42,
  "details": [
    {
      "committerId": "user1",
      "displayName": "Alice Smith",
      "email": "alice@example.com"
    },
    {
      "committerId": "user2",
      "displayName": "Bob Jones",
      "email": "bob@example.com"
    }
  ]
}
```

**Purpose:**
The Org Enablement API in Azure DevOps Advanced Security provides endpoints to get and update the enablement status of Advanced Security features for an entire organization. This allows organizations to centrally manage whether Advanced Security is enabled or disabled at the org level.

**Operations:**

| Operation | Description |
|-----------|-------------|
| Get       | Get the current status of Advanced Security for the organization. |
| Update    | Update the status of Advanced Security for the organization. |

---

### Get Org Enablement

- **Description:** Retrieve the current enablement status of Advanced Security for the organization.
- **Method:** GET
- **Endpoint:**
  `/organizations/{organization}/_apis/advancedsecurity/org-enablement?api-version=7.2-preview.1`
- **Parameters:**
  - `organization` (string, required): The name of the Azure DevOps organization.
  - `api-version` (string, required): The API version (use `7.2-preview.1`).

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://dev.azure.com/{organization}/_apis/advancedsecurity/org-enablement?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "enabled": true,
  "lastUpdated": "2025-08-01T12:00:00Z"
}
```

---

### Update Org Enablement

- **Description:** Update (enable or disable) Advanced Security for the organization.
- **Method:** PATCH
- **Endpoint:**
  `/organizations/{organization}/_apis/advancedsecurity/org-enablement?api-version=7.2-preview.1`
- **Parameters:**
  - `organization` (string, required): The name of the Azure DevOps organization.
  - `api-version` (string, required): The API version (use `7.2-preview.1`).
- **Body:**
  - `enabled` (boolean, required): Whether to enable (`true`) or disable (`false`) Advanced Security for the organization.

**Example cURL:**
```bash
curl -X PATCH \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"enabled": true}' \
  "https://dev.azure.com/{organization}/_apis/advancedsecurity/org-enablement?api-version=7.2-preview.1"
```

**Sample Request Body:**
```json
{
  "enabled": true
}
```

**Sample Response:**
```json
{
  "enabled": true,
  "lastUpdated": "2025-08-01T12:34:56Z"
}
```

**Purpose:**
The Meter Usage API in Azure DevOps Advanced Security provides endpoints to retrieve information about committers used for billing calculations. This helps organizations understand and audit which users are contributing to their Advanced Security usage and costs.

**Operations:**

| Operation | Description |
|-----------|-------------|
| Get       | Get committers used when calculating billing information. |

---

### Get Meter Usage

- **Description:** Retrieve the list of committers that are counted towards billing for Advanced Security in a repository.
- **Method:** GET
- **Endpoint:**
  `/organizations/{organization}/_apis/advancedsecurity/meter-usage?repositoryId={repositoryId}&api-version=7.2-preview.1`
- **Parameters:**
  - `organization` (string, required): The name of the Azure DevOps organization.
  - `repositoryId` (string, required): The ID of the repository.
  - `api-version` (string, required): The API version (use `7.2-preview.1`).

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://dev.azure.com/{organization}/_apis/advancedsecurity/meter-usage?repositoryId={repositoryId}&api-version=7.2-preview.1"
```

**Sample Response:**
```json
[
  {
    "committerId": "user1",
    "displayName": "Alice Smith",
    "email": "alice@example.com",
    "commitCount": 120
  },
  {
    "committerId": "user2",
    "displayName": "Bob Jones",
    "email": "bob@example.com",
    "commitCount": 85
  }
]
```

**Purpose:**
The Instances API in Azure DevOps Advanced Security provides endpoints to retrieve instances of a specific alert on a branch. This allows organizations to see where and how often a particular security alert has occurred across branches.

**Operations:**

| Operation | Description |
|-----------|-------------|
| List      | Get instances of an alert on a branch (or default branch if not specified). |

---

### List Alert Instances

- **Description:** Retrieve all instances of a specific alert on a given branch. If no branch is specified, returns instances on the default branch (if the alert exists there).
- **Method:** GET
- **Endpoint:**
  `/organizations/{organization}/_apis/advancedsecurity/instances?alertId={alertId}&repositoryId={repositoryId}&ref={branch}&api-version=7.2-preview.1`
- **Parameters:**
  - `organization` (string, required): The name of the Azure DevOps organization.
  - `alertId` (string, required): The unique identifier of the alert.
  - `repositoryId` (string, required): The ID of the repository.
  - `ref` (string, optional): The branch name (e.g., `refs/heads/main`). If omitted, defaults to the default branch.
  - `api-version` (string, required): The API version (use `7.2-preview.1`).

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://dev.azure.com/{organization}/_apis/advancedsecurity/instances?alertId={alertId}&repositoryId={repositoryId}&ref={branch}&api-version=7.2-preview.1"
```

**Sample Response:**
```json
[
  {
    "instanceId": "abc123",
    "alertId": "12345",
    "repositoryId": "repo1",
    "ref": "refs/heads/main",
    "location": {
      "filePath": "src/app.js",
      "line": 42
    },
    "status": "active",
    "detectedDate": "2025-08-01T12:34:56Z"
  },
  {
    "instanceId": "def456",
    "alertId": "12345",
    "repositoryId": "repo1",
    "ref": "refs/heads/main",
    "location": {
      "filePath": "src/utils.js",
      "line": 10
    },
    "status": "active",
    "detectedDate": "2025-08-01T12:35:10Z"
  }
]
```

**Purpose:**
The Analysis API in Azure DevOps Advanced Security provides endpoints to list branches for which security analysis results have been submitted. This helps organizations track which branches have been analyzed for security issues.

**Operations:**

| Operation | Description |
|-----------|-------------|
| List      | Returns the branches for which analysis results were submitted. |

---

### List Analysis Branches

- **Description:** Retrieve the list of branches that have submitted security analysis results for a repository.
- **Method:** GET
- **Endpoint:**
  `/organizations/{organization}/_apis/advancedsecurity/analysis?repositoryId={repositoryId}&api-version=7.2-preview.1`
- **Parameters:**
  - `organization` (string, required): The name of the Azure DevOps organization.
  - `repositoryId` (string, required): The ID of the repository.
  - `api-version` (string, required): The API version (use `7.2-preview.1`).

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://dev.azure.com/{organization}/_apis/advancedsecurity/analysis?repositoryId={repositoryId}&api-version=7.2-preview.1"
```

**Sample Response:**
```json
[
  {
    "branch": "main",
    "lastAnalysisDate": "2025-08-01T12:34:56Z"
  },
  {
    "branch": "feature/login",
    "lastAnalysisDate": "2025-07-30T09:21:00Z"
  }
]
```

**Purpose:**
The Alerts API in Azure DevOps Advanced Security provides endpoints to retrieve, list, and update security alerts for repositories. These alerts help organizations monitor and respond to security issues detected in their codebases.

**Operations:**

| Operation | Description |
|-----------|-------------|
| Get       | Get an alert by its ID. |
| List      | Get all alerts for a repository. |
| Update    | Update the status of an alert. |

---

### Get Alert

- **Description:** Retrieve a specific alert by its ID.
- **Method:** GET
- **Endpoint:**
  `/organizations/{organization}/_apis/advancedsecurity/alerts/{alertId}?api-version=7.2-preview.1`
- **Parameters:**
  - `organization` (string, required): The name of the Azure DevOps organization.
  - `alertId` (string, required): The unique identifier of the alert.
  - `api-version` (string, required): The API version (use `7.2-preview.1`).

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://dev.azure.com/{organization}/_apis/advancedsecurity/alerts/{alertId}?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "id": "12345",
  "repositoryId": "abcde",
  "status": "active",
  "severity": "high",
  "description": "Sensitive data detected in code.",
  ...
}
```

---

### List Alerts

- **Description:** Retrieve all alerts for a specific repository.
- **Method:** GET
- **Endpoint:**
  `/organizations/{organization}/_apis/advancedsecurity/alerts?repositoryId={repositoryId}&api-version=7.2-preview.1`
- **Parameters:**
  - `organization` (string, required): The name of the Azure DevOps organization.
  - `repositoryId` (string, required): The ID of the repository.
  - `api-version` (string, required): The API version (use `7.2-preview.1`).

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://dev.azure.com/{organization}/_apis/advancedsecurity/alerts?repositoryId={repositoryId}&api-version=7.2-preview.1"
```

**Sample Response:**
```json
[
  {
    "id": "12345",
    "status": "active",
    "severity": "high",
    ...
  },
  {
    "id": "67890",
    "status": "resolved",
    "severity": "medium",
    ...
  }
]
```

---

### Update Alert

- **Description:** Update the status of a specific alert (e.g., mark as resolved).
- **Method:** PATCH
- **Endpoint:**
  `/organizations/{organization}/_apis/advancedsecurity/alerts/{alertId}?api-version=7.2-preview.1`
- **Parameters:**
  - `organization` (string, required): The name of the Azure DevOps organization.
  - `alertId` (string, required): The unique identifier of the alert.
  - `api-version` (string, required): The API version (use `7.2-preview.1`).
- **Body:**
  - `status` (string, required): The new status for the alert (e.g., `resolved`).

**Example cURL:**
```bash
curl -X PATCH \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"status": "resolved"}' \
  "https://dev.azure.com/{organization}/_apis/advancedsecurity/alerts/{alertId}?api-version=7.2-preview.1"
```

**Sample Request Body:**
```json
{
  "status": "resolved"
}
```

**Sample Response:**
```json
{
  "id": "12345",
  "status": "resolved",
  ...
}
```

---

> **Note:** Each sub-area will be expanded with endpoint lists, descriptions, required and optional parameters, example cURL requests, and example request/response bodies.

---

## Progress
- [x] All operations and sub-areas fully documented
