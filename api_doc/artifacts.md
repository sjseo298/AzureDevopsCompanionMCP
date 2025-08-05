# Artifacts API (Azure DevOps REST API v7.2)

This document provides a comprehensive English-language reference for the Artifacts API area in Azure DevOps REST API v7.2. It covers all sub-areas and operations, including endpoints, parameters, cURL examples, and sample request/response bodies.

## Purpose
The Artifacts API enables you to manage feeds, packages, retention policies, permissions, and more in Azure DevOps Artifacts. It is used for automating and integrating package management tasks in your DevOps workflows.

## Sub-Areas & Operations

### 1. Artifact Details
- Get Badge
- Get Package
- Get Packages
- Get Package Version
- Get Package Version Provenance
- Get Package Versions
- Query Package Metrics
- Query Package Version Metrics

### 2. Change Tracking
- Get Feed Change
- Get Feed Changes
- Get Package Changes

- Create Feed
- Create Feed View
- Get Feed Permissions
- Get Feed View
- Get Feed Views
- Set Feed Permissions
- Update Feed
- Update Feed View

### 4. Feed Recycle Bin
- List (feeds in recycle bin)
- Permanent Delete Feed
- Restore Deleted Feed

### 5. Provenance
- Create Session

### 6. Recycle Bin
- Empty Recycle Bin
- Get Recycle Bin Package
- Get Recycle Bin Packages
- Get Recycle Bin Package Versions

- Set Retention Policy
### 8. Service Settings
- Get Global Permissions
- Set Global Permissions

---


## 1. Artifact Details

### Get Badge
- **Description:** Generate a SVG badge for the latest version of a package. The badge can be used as an image in an HTML link to the feed containing the package.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/packages/{packageId}/badge?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required): Feed identifier
  - `packageId` (string, required): Package identifier
  - `api-version` (string, required): Use `7.2-preview.1`

**Example cURL:**
```bash
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/packages/{packageId}/badge?api-version=7.2-preview.1"
SVG image (not shown here)
---

- **Description:** Get details about a specific package.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/packages/{packageId}?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `packageId` (string, required)
  - `api-version` (string, required)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/packages/{packageId}?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "id": "packageId",
  "name": "MyPackage",
  "version": "1.0.0",


## 8. Service Settings

### Get Global Permissions
- **Description:** Get the global permissions for the Artifacts service in the organization.
- **Method:** GET
- **Endpoint:**
  `/serviceSettings/permissions?api-version=7.2-preview.1`
- **Parameters:**
  - `api-version` (string, required)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/_apis/packaging/serviceSettings/permissions?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "permissions": [
    {"identity": "user1", "role": "Administrator"},
    {"identity": "user2", "role": "Reader"}
  ]
}
```

---

### Set Global Permissions
- **Description:** Set the global permissions for the Artifacts service in the organization.
- **Method:** PATCH
- **Endpoint:**
  `/serviceSettings/permissions?api-version=7.2-preview.1`
- **Parameters:**
  - `api-version` (string, required)
- **Body:**
  - Permissions update details

**Example cURL:**
```bash
curl -X PATCH \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"permissions": [{"identity": "user1", "role": "Administrator"}]}' \
  "https://feeds.dev.azure.com/{organization}/_apis/packaging/serviceSettings/permissions?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "permissions": [
    {"identity": "user1", "role": "Administrator"}
  ]
}
```

---
  "description": "Sample package"
}
```

---

### Get Packages
- **Description:** Get details about all of the packages in the feed. Use filters to include or exclude information from the result set.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/packages?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `api-version` (string, required)
  - Optional filters: `packageNameQuery`, `protocolType`, `includeDeleted`, etc.

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/packages?api-version=7.2-preview.1"
```

**Sample Response:**
```json
[
  {
    "id": "packageId1",
    "name": "MyPackage1",
    "version": "1.0.0"
  },
  {
    "id": "packageId2",
    "name": "MyPackage2",
    "version": "2.0.0"
  }
]
```

---

### Get Package Version
- **Description:** Get details about a specific package version.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/packages/{packageId}/versions/{version}?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `packageId` (string, required)
  - `version` (string, required)
  - `api-version` (string, required)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/packages/{packageId}/versions/{version}?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "id": "packageId",
  "version": "1.0.0",
  "description": "Sample version"
}
```

---

### Get Package Version Provenance
- **Description:** Gets provenance for a package version.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/packages/{packageId}/versions/{version}/provenance?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `packageId` (string, required)
  - `version` (string, required)
  - `api-version` (string, required)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/packages/{packageId}/versions/{version}/provenance?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "provenance": "build pipeline info or other provenance data"
}
```

---

### Get Package Versions
- **Description:** Get a list of package versions, optionally filtering by state.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/packages/{packageId}/versions?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `packageId` (string, required)
  - `api-version` (string, required)
  - Optional filters: `isListed`, `state`, etc.

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/packages/{packageId}/versions?api-version=7.2-preview.1"
```

**Sample Response:**
```json
[
  {
    "version": "1.0.0",
    "state": "listed"
  },
  {
    "version": "2.0.0",
    "state": "unlisted"
  }
]
```

---

### Query Package Metrics
- **Description:** Query metrics for a package.
- **Method:** POST
- **Endpoint:**
  `/feeds/{feedId}/packages/{packageId}/metrics/query?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `packageId` (string, required)
  - `api-version` (string, required)
- **Body:**
  - Query parameters for metrics (see official docs for details)

**Example cURL:**
```bash
curl -X POST \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"metricTypes": ["DownloadCount"]}' \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/packages/{packageId}/metrics/query?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "metrics": [
    {"type": "DownloadCount", "value": 42}
  ]
}
```

---

### Query Package Version Metrics
- **Description:** Query metrics for a specific package version.
- **Method:** POST
- **Endpoint:**
  `/feeds/{feedId}/packages/{packageId}/versions/{version}/metrics/query?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `packageId` (string, required)
  - `version` (string, required)
  - `api-version` (string, required)
- **Body:**
  - Query parameters for metrics (see official docs for details)

**Example cURL:**
```bash
curl -X POST \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"metricTypes": ["DownloadCount"]}' \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/packages/{packageId}/versions/{version}/metrics/query?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "metrics": [
    {"type": "DownloadCount", "value": 10}
  ]
}
```

---


## 2. Change Tracking

### Get Feed Change
- **Description:** Query a feed to determine its current state. The project parameter must be supplied if the feed was created in a project.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/changeTracking/feedChange?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/changeTracking/feedChange?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "feedId": "feedId",
  "changeId": 12345,
  "timestamp": "2025-08-04T12:00:00Z"
}
```

---

### Get Feed Changes
- **Description:** Query to determine which feeds have changed since the last call, tracked through the provided continuationToken. Only changes to a feed itself are returned.
- **Method:** GET
- **Endpoint:**
  `/feeds/changeTracking/feedChanges?continuationToken={token}&api-version=7.2-preview.1`
- **Parameters:**
  - `continuationToken` (string, required)
  - `api-version` (string, required)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/_apis/packaging/feeds/changeTracking/feedChanges?continuationToken={token}&api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "changes": [
    {"feedId": "feedId1", "changeId": 123, "timestamp": "2025-08-04T12:00:00Z"},
    {"feedId": "feedId2", "changeId": 456, "timestamp": "2025-08-04T12:05:00Z"}
  ],
  "continuationToken": "nextToken"
}
```

---

### Get Package Changes
- **Description:** Get a batch of package changes made to a feed. The changes returned are 'most recent change' for each package.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/changeTracking/packageChanges?continuationToken={token}&api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `continuationToken` (string, required)
  - `api-version` (string, required)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/changeTracking/packageChanges?continuationToken={token}&api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "changes": [
    {"packageId": "pkg1", "changeId": 789, "timestamp": "2025-08-04T12:10:00Z"},
    {"packageId": "pkg2", "changeId": 790, "timestamp": "2025-08-04T12:15:00Z"}
  ],
  "continuationToken": "nextToken"
}
```

---


## 3. Feed Management

### Create Feed
- **Description:** Create a feed, a container for various package types. Feeds can be created in a project if the project parameter is included in the request URL.
- **Method:** POST
- **Endpoint:**
  `/feeds?api-version=7.2-preview.1`
- **Parameters:**
  - `api-version` (string, required)
  - `project` (string, optional)
- **Body:**
  - Feed creation details (name, description, etc.)

**Example cURL:**
```bash
curl -X POST \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"name": "MyFeed", "description": "Sample feed"}' \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "id": "feedId",
  "name": "MyFeed",
  "description": "Sample feed"
}
```

---

### Create Feed View
- **Description:** Create a new view on the referenced feed.
- **Method:** POST
- **Endpoint:**
  `/feeds/{feedId}/views?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `api-version` (string, required)
- **Body:**
  - View creation details (name, etc.)

**Example cURL:**
```bash
curl -X POST \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"name": "@prerelease"}' \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/views?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "id": "viewId",
  "name": "@prerelease"
}
```

---

### Delete Feed
- **Description:** Remove a feed and all its packages. The feed moves to the recycle bin and is reversible.
- **Method:** DELETE
- **Endpoint:**
  `/feeds/{feedId}?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `api-version` (string, required)

**Example cURL:**
```bash
curl -X DELETE \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "id": "feedId",
  "deleted": true
}
```

---

### Delete Feed View
- **Description:** Delete a feed view.
- **Method:** DELETE
- **Endpoint:**
  `/feeds/{feedId}/views/{viewId}?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `viewId` (string, required)
  - `api-version` (string, required)

**Example cURL:**
```bash
curl -X DELETE \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/views/{viewId}?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "id": "viewId",
  "deleted": true
}
```

---

### Get Feed
- **Description:** Get the settings for a specific feed.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `api-version` (string, required)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "id": "feedId",
  "name": "MyFeed",
  "description": "Sample feed"
}
```

---

### Get Feed Permissions
- **Description:** Get the permissions for a feed.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/permissions?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `api-version` (string, required)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/permissions?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "permissions": [
    {"identity": "user1", "role": "Contributor"},
    {"identity": "user2", "role": "Reader"}
  ]
}
```

---

### Get Feeds
- **Description:** Get all feeds in an account where you have the provided role access.
- **Method:** GET
- **Endpoint:**
  `/feeds?api-version=7.2-preview.1`
- **Parameters:**
  - `api-version` (string, required)
  - `project` (string, optional)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds?api-version=7.2-preview.1"
```

**Sample Response:**
```json
[
  {"id": "feedId1", "name": "Feed1"},
  {"id": "feedId2", "name": "Feed2"}
]
```

---

### Get Feed View
- **Description:** Get a view by Id.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/views/{viewId}?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `viewId` (string, required)
  - `api-version` (string, required)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/views/{viewId}?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "id": "viewId",
  "name": "@prerelease"
}
```

---

### Get Feed Views
- **Description:** Get all views for a feed.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/views?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `api-version` (string, required)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/views?api-version=7.2-preview.1"
```

**Sample Response:**
```json
[
  {"id": "viewId1", "name": "@prerelease"},
  {"id": "viewId2", "name": "@release"}
]
```

---

### Set Feed Permissions
- **Description:** Update the permissions on a feed.
- **Method:** PATCH
- **Endpoint:**
  `/feeds/{feedId}/permissions?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `api-version` (string, required)
- **Body:**
  - Permissions update details

**Example cURL:**
```bash
curl -X PATCH \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"permissions": [{"identity": "user1", "role": "Contributor"}]}' \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/permissions?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "permissions": [
    {"identity": "user1", "role": "Contributor"}
  ]
}
```

---

### Update Feed
- **Description:** Change the attributes of a feed.
- **Method:** PATCH
- **Endpoint:**
  `/feeds/{feedId}?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `api-version` (string, required)
- **Body:**
  - Feed update details (name, description, etc.)

**Example cURL:**
```bash
curl -X PATCH \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"name": "MyFeedUpdated", "description": "Updated description"}' \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "id": "feedId",
  "name": "MyFeedUpdated",
  "description": "Updated description"
}
```

---

### Update Feed View
- **Description:** Update a view.
- **Method:** PATCH
- **Endpoint:**
  `/feeds/{feedId}/views/{viewId}?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `viewId` (string, required)
  - `api-version` (string, required)
- **Body:**
  - View update details (name, etc.)

**Example cURL:**
```bash
curl -X PATCH \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"name": "@release"}' \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/views/{viewId}?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "id": "viewId",
  "name": "@release"
}
```

---


## 4. Feed Recycle Bin

### List (feeds in recycle bin)
- **Description:** Query for feeds within the recycle bin. If the project parameter is present, gets all feeds in recycle bin in the given project.
- **Method:** GET
- **Endpoint:**
  `/recycleBin/feeds?api-version=7.2-preview.1`
- **Parameters:**
  - `api-version` (string, required)
  - `project` (string, optional)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/recycleBin/feeds?api-version=7.2-preview.1"
```

**Sample Response:**
```json
[
  {"id": "feedId1", "name": "DeletedFeed1"},
  {"id": "feedId2", "name": "DeletedFeed2"}
]
```

---

### Permanent Delete Feed
- **Description:** Permanently delete a feed and all of its packages. The action is irreversible and the package content will be deleted immediately.
- **Method:** DELETE
- **Endpoint:**
  `/recycleBin/feeds/{feedId}?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `api-version` (string, required)

**Example cURL:**
```bash
curl -X DELETE \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/recycleBin/feeds/{feedId}?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "id": "feedId",
  "permanentlyDeleted": true
}
```

---

### Restore Deleted Feed
- **Description:** Restores a deleted feed and all of its packages.
- **Method:** PATCH
- **Endpoint:**
  `/recycleBin/feeds/{feedId}?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `api-version` (string, required)

**Example cURL:**
```bash
curl -X PATCH \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/recycleBin/feeds/{feedId}?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "id": "feedId",
  "restored": true
}
```

---


## 5. Provenance

### Create Session
- **Description:** Creates a session, a wrapper around a feed that can store additional metadata on the packages published to it.
- **Method:** POST
- **Endpoint:**
  `/feeds/{feedId}/provenance/session?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `api-version` (string, required)
- **Body:**
  - Session creation details (see official docs for details)

**Example cURL:**
```bash
curl -X POST \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"metadata": {"key": "value"}}' \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/provenance/session?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "sessionId": "sessionId",
  "feedId": "feedId",
  "metadata": {"key": "value"}
}
```

---


## 6. Recycle Bin

### Empty Recycle Bin
- **Description:** Queues a job to remove all package versions from a feed's recycle bin.
- **Method:** POST
- **Endpoint:**
  `/feeds/{feedId}/recycleBin/empty?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `api-version` (string, required)

**Example cURL:**
```bash
curl -X POST \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/recycleBin/empty?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "jobId": "jobId",
  "status": "queued"
}
```

---

### Get Recycle Bin Package
- **Description:** Get information about a package and all its versions within the recycle bin.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/recycleBin/packages/{packageId}?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `packageId` (string, required)
  - `api-version` (string, required)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/recycleBin/packages/{packageId}?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "id": "packageId",
  "name": "DeletedPackage",
  "versions": ["1.0.0", "2.0.0"]
}
```

---

### Get Recycle Bin Packages
- **Description:** Query for packages within the recycle bin.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/recycleBin/packages?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `api-version` (string, required)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/recycleBin/packages?api-version=7.2-preview.1"
```

**Sample Response:**
```json
[
  {"id": "packageId1", "name": "DeletedPackage1"},
  {"id": "packageId2", "name": "DeletedPackage2"}
]
```

---

### Get Recycle Bin Package Version
- **Description:** Get information about a package version within the recycle bin.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/recycleBin/packages/{packageId}/versions/{version}?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `packageId` (string, required)
  - `version` (string, required)
  - `api-version` (string, required)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/recycleBin/packages/{packageId}/versions/{version}?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "id": "packageId",
  "version": "1.0.0",
  "state": "deleted"
}
```

---

### Get Recycle Bin Package Versions
- **Description:** Get a list of package versions within the recycle bin.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/recycleBin/packages/{packageId}/versions?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `packageId` (string, required)
  - `api-version` (string, required)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/recycleBin/packages/{packageId}/versions?api-version=7.2-preview.1"
```

**Sample Response:**
```json
[
  {"version": "1.0.0", "state": "deleted"},
  {"version": "2.0.0", "state": "deleted"}
]
```

---


## 7. Retention Policies

### Delete Retention Policy
- **Description:** Delete the retention policy for a feed.
- **Method:** DELETE
- **Endpoint:**
  `/feeds/{feedId}/retentionPolicies?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `api-version` (string, required)

**Example cURL:**
```bash
curl -X DELETE \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/retentionPolicies?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "id": "feedId",
  "retentionPolicyDeleted": true
}
```

---

### Get Retention Policy
- **Description:** Get the retention policy for a feed.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/retentionPolicies?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `api-version` (string, required)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/retentionPolicies?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "id": "feedId",
  "retentionPolicy": {
    "daysToKeep": 30,
    "minimumToKeep": 2
  }
}
```

---

### Set Retention Policy
- **Description:** Set the retention policy for a feed.
- **Method:** PUT
- **Endpoint:**
  `/feeds/{feedId}/retentionPolicies?api-version=7.2-preview.1`
- **Parameters:**
  - `feedId` (string, required)
  - `api-version` (string, required)
- **Body:**
  - Retention policy details (daysToKeep, minimumToKeep, etc.)

**Example cURL:**
```bash
curl -X PUT \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"daysToKeep": 30, "minimumToKeep": 2}' \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/retentionPolicies?api-version=7.2-preview.1"
```

**Sample Response:**
```json
{
  "id": "feedId",
  "retentionPolicy": {
    "daysToKeep": 30,
    "minimumToKeep": 2
  }
}
```

---

## Documentation Progress

- [x] Structure and sub-area listing
- [x] Artifact Details (all operations)
- [x] Change Tracking (all operations)
- [x] Feed Management (all operations)
- [x] Feed Recycle Bin (all operations)
- [x] Provenance (all operations)
- [x] Recycle Bin (all operations)
- [x] Retention Policies (all operations)
- [x] Service Settings (all operations)
- [x] All operations fully documented
