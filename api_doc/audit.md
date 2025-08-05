# Audit API (Azure DevOps REST API v7.2)

## Overview

The Audit API in Azure DevOps allows you to access and manage the audit log for your Azure DevOps organization. The audit log contains records of auditable events, providing visibility into actions and changes for compliance, security, and operational monitoring.

**Main functionalities:**
- Query the audit log for specific events or time ranges.
- Download the audit log for offline analysis or archiving.
- Retrieve the list of available audit actions.

## Operations

### 1. Query Log
- **Description:** Retrieve audit log entries based on filters such as date range, users, or actions.
- **Endpoint:** `GET /_apis/audit/auditlog`
- **Docs:** [Query Log](https://learn.microsoft.com/en-us/rest/api/azure/devops/audit/audit-log?view=azure-devops-rest-7.2)

### 2. Download Log
- **Description:** Download the audit log as a file for external review or compliance purposes.
- **Endpoint:** `GET /_apis/audit/downloadlog`
- **Docs:** [Download Log](https://learn.microsoft.com/en-us/rest/api/azure/devops/audit/download-log?view=azure-devops-rest-7.2)

### 3. Get Actions
- **Description:** Retrieve the list of possible audit actions that can be logged.
- **Endpoint:** `GET /_apis/audit/actions`
- **Docs:** [Get Actions](https://learn.microsoft.com/en-us/rest/api/azure/devops/audit/actions?view=azure-devops-rest-7.2)

## Usage Example

To query the audit log for a specific date range:

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/_apis/audit/auditlog?startTime=2024-01-01T00:00:00Z&endTime=2024-01-31T23:59:59Z&api-version=7.2-preview.1' \
  -H 'Authorization: Basic <PAT>'
```

## Additional Resources
- [Official documentation](https://learn.microsoft.com/en-us/rest/api/azure/devops/audit/?view=azure-devops-rest-7.2)
- [Code samples](https://github.com/microsoft/azure-devops-dotnet-samples/tree/master/ClientLibrary/Samples/Audit)

---

This documentation covers all main operations and links to further details for each endpoint.
