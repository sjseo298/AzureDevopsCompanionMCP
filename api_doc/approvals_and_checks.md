# Approvals and Checks API (Azure DevOps REST API v7.2)

This document provides a comprehensive English-language reference for the Approvals and Checks API area in Azure DevOps REST API v7.2. It covers all sub-areas and operations, including check configurations, approvals, permissions, and check evaluations, with purpose, endpoints, parameters, cURL examples, and sample request/response bodies.

## Purpose
The Approvals and Checks API enables you to manage checks and approvals for resources in Azure DevOps pipelines. Checks are conditions that must be satisfied before a pipeline stage can consume a resource (such as environments, service connections, agent pools, etc.). This API allows you to configure, evaluate, and manage these checks, as well as handle approvals and permissions for pipeline resources.

## Sub-Areas & Operations

### 1. Check Configurations
- Add Check Configuration
- List Check Configurations

### 2. Approvals
- Get Approval
- Update (Approve) Approval

### 3. Permissions
- Get Authorized Pipelines
- Update Pipeline Permissions

### 4. Check Evaluations
- Initiate Check Evaluation
- Get Check Evaluation

---

## 1. Check Configurations

### Add Check Configuration
- **Description:** Add a new check configuration to a resource.
- **Method:** POST
- **Endpoint:**
  `/organizations/{organization}/_apis/approvalsandchecks/check-configurations?api-version=7.2-preview.1`
- **Parameters:**
  - `organization` (string, required)
  - `api-version` (string, required, use `7.2-preview.1`)
- **Body:**
  - `type` (string, required): Type of check (e.g., `Approval`, `BusinessHours`, etc.)
  - `resourceType` (string, required)
  - `resourceId` (string, required)
  - `settings` (object, required)

**Example cURL:**
```bash
curl -X POST \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"type": "Approval", "resourceType": "environment", "resourceId": "env1", "settings": {"approvers": ["user1"]}}' \
  "https://dev.azure.com/{organization}/_apis/approvalsandchecks/check-configurations?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "id": 100,
  "type": "Approval",
  "resourceType": "environment",
  "resourceId": "env1",
  "settings": {"approvers": ["user1"]},
  "createdOn": "2025-08-01T12:00:00Z"
}
```

---

### List Check Configurations
- **Description:** List all check configurations for a resource.
- **Method:** GET
- **Endpoint:**
  `/organizations/{organization}/_apis/approvalsandchecks/check-configurations?resourceType={resourceType}&resourceId={resourceId}&api-version=7.2-preview.1`
- **Parameters:**
  - `organization` (string, required)
  - `resourceType` (string, required)
  - `resourceId` (string, required)
  - `api-version` (string, required, use `7.2-preview.1`)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://dev.azure.com/{organization}/_apis/approvalsandchecks/check-configurations?resourceType={resourceType}&resourceId={resourceId}&api-version=7.2-preview.1"
```

**Sample Response:**
```json
[
  {
    "id": 100,
    "type": "Approval",
    "resourceType": "environment",
    "resourceId": "env1",
    "settings": {"approvers": ["user1"]}
  }
]
```

---

## 2. Approvals

### Get Approval
- **Description:** Get an approval by its ID.
- **Method:** GET
- **Endpoint:**
  `/organizations/{organization}/_apis/approvalsandchecks/approvals/{approvalId}?api-version=7.2-preview.1`
- **Parameters:**
  - `organization` (string, required)
  - `approvalId` (integer, required)
  - `api-version` (string, required, use `7.2-preview.1`)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://dev.azure.com/{organization}/_apis/approvalsandchecks/approvals/{approvalId}?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "id": 200,
  "status": "pending",
  "approvers": ["user1"],
  "createdOn": "2025-08-01T12:00:00Z"
}
```

---

### Update (Approve) Approval
- **Description:** Approve or update an approval.
- **Method:** PATCH
- **Endpoint:**
  `/organizations/{organization}/_apis/approvalsandchecks/approvals/{approvalId}?api-version=7.2-preview.1`
- **Parameters:**
  - `organization` (string, required)
  - `approvalId` (integer, required)
  - `api-version` (string, required, use `7.2-preview.1`)
- **Body:**
  - `status` (string, required): New status (e.g., `approved`)

**Example cURL:**
```bash
curl -X PATCH \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"status": "approved"}' \
  "https://dev.azure.com/{organization}/_apis/approvalsandchecks/approvals/{approvalId}?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "id": 200,
  "status": "approved",
  "approvers": ["user1"],
  "updatedOn": "2025-08-01T12:10:00Z"
}
```

---

## 3. Permissions

### Get Authorized Pipelines
- **Description:** Get authorized pipelines for a resource.
- **Method:** GET
- **Endpoint:**
  `/organizations/{organization}/_apis/approvalsandchecks/pipeline-permissions?resourceType={resourceType}&resourceId={resourceId}&api-version=7.2-preview.1`
- **Parameters:**
  - `organization` (string, required)
  - `resourceType` (string, required)
  - `resourceId` (string, required)
  - `api-version` (string, required, use `7.2-preview.1`)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://dev.azure.com/{organization}/_apis/approvalsandchecks/pipeline-permissions?resourceType={resourceType}&resourceId={resourceId}&api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "authorizedPipelines": [
    {"id": "pipeline1", "name": "CI Pipeline"},
    {"id": "pipeline2", "name": "CD Pipeline"}
  ]
}
```

---

### Update Pipeline Permissions
- **Description:** Update pipeline permissions for a resource.
- **Method:** PATCH
- **Endpoint:**
  `/organizations/{organization}/_apis/approvalsandchecks/pipeline-permissions?resourceType={resourceType}&resourceId={resourceId}&api-version=7.2-preview.1`
- **Parameters:**
  - `organization` (string, required)
  - `resourceType` (string, required)
  - `resourceId` (string, required)
  - `api-version` (string, required, use `7.2-preview.1`)
- **Body:**
  - `authorizedPipelines` (array, required): List of pipeline IDs to authorize

**Example cURL:**
```bash
curl -X PATCH \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"authorizedPipelines": ["pipeline1", "pipeline2"]}' \
  "https://dev.azure.com/{organization}/_apis/approvalsandchecks/pipeline-permissions?resourceType={resourceType}&resourceId={resourceId}&api-version=7.2-preview.1"
```

**Sample Request Body:**
```json
{
  "authorizedPipelines": ["pipeline1", "pipeline2"]
}
```

**Sample Response:**
```json
{
  "authorizedPipelines": [
    {"id": "pipeline1", "name": "CI Pipeline"},
    {"id": "pipeline2", "name": "CD Pipeline"}
  ]
}
```

---

## 4. Check Evaluations

### Initiate Check Evaluation
- **Description:** Initiate an evaluation for a check in a pipeline.
- **Method:** POST
- **Endpoint:**
  `/organizations/{organization}/_apis/approvalsandchecks/check-evaluations/evaluate?api-version=7.2-preview.1`
- **Parameters:**
  - `organization` (string, required)
  - `api-version` (string, required, use `7.2-preview.1`)
- **Body:**
  - `checkConfigurationId` (integer, required)
  - `resourceType` (string, required)
  - `resourceId` (string, required)
  - `pipelineId` (string, required)

**Example cURL:**
```bash
curl -X POST \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"checkConfigurationId": 100, "resourceType": "environment", "resourceId": "env1", "pipelineId": "pipeline1"}' \
  "https://dev.azure.com/{organization}/_apis/approvalsandchecks/check-evaluations/evaluate?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "evaluationId": 300,
  "status": "inProgress",
  "checkConfigurationId": 100,
  "pipelineId": "pipeline1"
}
```

---

### Get Check Evaluation
- **Description:** Get details for a specific check evaluation.
- **Method:** GET
- **Endpoint:**
  `/organizations/{organization}/_apis/approvalsandchecks/check-evaluations/{evaluationId}?api-version=7.2-preview.1`
- **Parameters:**
  - `organization` (string, required)
  - `evaluationId` (integer, required)
  - `api-version` (string, required, use `7.2-preview.1`)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://dev.azure.com/{organization}/_apis/approvalsandchecks/check-evaluations/{evaluationId}?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "evaluationId": 300,
  "status": "succeeded",
  "checkConfigurationId": 100,
  "pipelineId": "pipeline1",
  "result": "approved"
}
```

---

## Documentation Progress

- [x] Structure and sub-area listing
- [x] Check Configurations (Add, List)
- [x] Approvals (Get, Update)
- [x] Permissions (Get, Update)
- [x] Check Evaluations (Initiate, Get)
- [x] All operations fully documented
