# Artifacts Package Types API (Azure DevOps REST API v7.2)

## Documentation Progress

- [x] Structure and sub-area listing
- [x] Cargo (all operations)
- [x] Maven (all operations)
- [x] Npm (all operations)
- [x] NuGet (all operations)
- [x] Python (all operations)
 - [x] Universal (all operations)
 - [x] All operations fully documented

This document provides a comprehensive English-language reference for the Artifacts Package Types API area in Azure DevOps REST API v7.2. It covers all sub-areas and operations, including endpoints, parameters, cURL examples, and sample request/response bodies.

## Purpose
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"behavior": "allow", "upstreamSource": "public"}' \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/maven/packages/{packageName}/upstreaming?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "upstreamSource": "public",
  "behavior": "allow"
}
```

---

- **Description:** Update state for a Maven package version. The project parameter must be supplied if the feed was created in a project.
- **Method:** PATCH
  - `feedId` (string, required)
  - `packageVersion` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)
- **Body:**
  - State update details (see official docs for schema)
**Example cURL:**
```bash
curl -X PATCH \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"state": "unlisted"}' \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/maven/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "id": "{packageName}",
  "version": "{packageVersion}",
  "state": "unlisted"
}
```

---

- **Description:** Update several Maven packages from a single feed in a single request. The updates to the packages do not happen atomically. The project parameter must be supplied if the feed was created in a project.
- **Method:** PATCH
  - `feedId` (string, required)
  - `project` (string, required if feed is project-scoped)
- **Body:**
  - Batch update details (see official docs for schema)
**Example cURL:**
```bash
curl -X PATCH \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"updates": [{"packageName": "foo", "packageVersion": "1.0.0", "state": "unlisted"}]}' \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/maven/packages/versionsBatch?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "updated": [
    {"packageName": "foo", "packageVersion": "1.0.0", "state": "unlisted"}
  ]
}
```

---

- **Description:** Delete or restore several Maven package versions from the recycle bin. The project parameter must be supplied if the feed was created in a project.
- **Method:** PATCH
  - `feedId` (string, required)
  - `project` (string, required if feed is project-scoped)
- **Body:**
  - Batch recycle bin update details (see official docs for schema)
**Example cURL:**
```bash
curl -X PATCH \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"actions": [{"packageName": "foo", "packageVersion": "1.0.0", "action": "restore"}]}' \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/maven/RecycleBin/packages/versionsBatch?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "restored": [
    {"packageName": "foo", "packageVersion": "1.0.0"}
  ]
}
```

---

- **Description:** Get information about a Cargo package version in the recycle bin. The project parameter must be supplied if the feed was created in a project.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/cargo/RecycleBin/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/cargo/RecycleBin/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "id": "{packageName}",
  "version": "{packageVersion}",
  "state": "deleted"
}
```

---

### Get Upstreaming Behavior
- **Description:** Get the upstreaming behavior of a Cargo package within the context of a feed.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/cargo/packages/{packageName}/upstreaming?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/cargo/packages/{packageName}/upstreaming?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "upstreamSource": "public",
  "behavior": "allow"
}
```

---

### Restore Package Version From Recycle Bin
- **Description:** Restore a Cargo package version from the recycle bin to its associated feed. The project parameter must be supplied if the feed was created in a project.
- **Method:** PATCH
- **Endpoint:**
  `/feeds/{feedId}/cargo/RecycleBin/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X PATCH \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/cargo/RecycleBin/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "id": "{packageName}",
  "version": "{packageVersion}",
  "restored": true
}
```

---

### Set Upstreaming Behavior
- **Description:** Set the upstreaming behavior of a Cargo package within the context of a feed. The package does not need to exist in the feed prior to setting the behavior.
- **Method:** PUT
- **Endpoint:**
  `/feeds/{feedId}/cargo/packages/{packageName}/upstreaming?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)
- **Body:**
  - Upstreaming behavior details (see official docs for schema)

**Example cURL:**
```bash
curl -X PUT \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"behavior": "allow", "upstreamSource": "public"}' \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/cargo/packages/{packageName}/upstreaming?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "upstreamSource": "public",
  "behavior": "allow"
}
```

---

### Update Package Version
- **Description:** Update state for a Cargo package version. The project parameter must be supplied if the feed was created in a project.
- **Method:** PATCH
- **Endpoint:**
  `/feeds/{feedId}/cargo/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)
- **Body:**
  - State update details (see official docs for schema)

**Example cURL:**
```bash
curl -X PATCH \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"state": "unlisted"}' \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/cargo/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "id": "{packageName}",
  "version": "{packageVersion}",
  "state": "unlisted"
}
```

---

### Update Package Versions
- **Description:** Update several Cargo packages from a single feed in a single request. The updates to the packages do not happen atomically. The project parameter must be supplied if the feed was created in a project.
- **Method:** PATCH
- **Endpoint:**
  `/feeds/{feedId}/cargo/packages/versionsBatch?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)
- **Body:**
  - Batch update details (see official docs for schema)

**Example cURL:**
```bash
curl -X PATCH \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"updates": [{"packageName": "foo", "packageVersion": "1.0.0", "state": "unlisted"}]}' \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/cargo/packages/versionsBatch?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "updated": [
    {"packageName": "foo", "packageVersion": "1.0.0", "state": "unlisted"}
  ]
}
```

---

### Update Recycle Bin Package Versions
- **Description:** Delete or restore several Cargo package versions from the recycle bin. The project parameter must be supplied if the feed was created in a project.
- **Method:** PATCH
- **Endpoint:**
  `/feeds/{feedId}/cargo/RecycleBin/packages/versionsBatch?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)
- **Body:**
  - Batch recycle bin update details (see official docs for schema)

**Example cURL:**
```bash
curl -X PATCH \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"actions": [{"packageName": "foo", "packageVersion": "1.0.0", "action": "restore"}]}' \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/cargo/RecycleBin/packages/versionsBatch?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "restored": [
    {"packageName": "foo", "packageVersion": "1.0.0"}
  ]
}
```

---

### 2. Maven
- Delete Package Version
- Delete Package Version From Recycle Bin

**Sample Response:**
```json
{
  "id": "{packageName}",
  "version": "{packageVersion}",
  "permanentlyDeleted": true
}
```

---

### Download Package
- **Description:** Download a NuGet package file. Returns the URL of the requested package file.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/nuget/packages/{packageName}/versions/{packageVersion}/content/{filePath}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `filePath` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/nuget/packages/{packageName}/versions/{packageVersion}/content/{filePath}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "downloadUrl": "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/nuget/packages/{packageName}/versions/{packageVersion}/content/{filePath}"
}
```

---

### Get Package Version
- **Description:** Get information about a NuGet package version. The project parameter must be supplied if the feed was created in a project.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/nuget/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/nuget/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "id": "{packageName}",
  "version": "{packageVersion}",
  "state": "active"
}
```

---

#### Get Package Version From Recycle Bin
  `/feeds/{feedId}/upack/RecycleBin/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview`
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/upack/RecycleBin/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "id": "{packageName}",
  "version": "{packageVersion}",
  "state": "deleted"
}
```

#### Restore Package Version From Recycle Bin
- **Description:** Restore a Universal package version from the recycle bin to its associated feed. The project parameter must be supplied if the feed was created in a project.
- **Method:** PATCH
- **Endpoint:**
  `/feeds/{feedId}/upack/RecycleBin/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X PATCH \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/upack/RecycleBin/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "id": "{packageName}",
  "version": "{packageVersion}",
  "restored": true
}
```

---

### Get Package Version From Recycle Bin
- **Description:** Get information about a NuGet package version in the recycle bin. The project parameter must be supplied if the feed was created in a project.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/nuget/RecycleBin/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/nuget/RecycleBin/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "id": "{packageName}",
  "version": "{packageVersion}",
  "state": "deleted"
}
```

---

## 3. Npm

### Delete Package Version From Recycle Bin
- **Description:** Delete a package version without an npm scope from the recycle bin. The project parameter must be supplied if the feed was created in a project.
- **Method:** DELETE
- **Endpoint:**
  `/feeds/{feedId}/npm/RecycleBin/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X DELETE \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/npm/RecycleBin/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "id": "{packageName}",
  "version": "{packageVersion}",
  "permanentlyDeleted": true
}
```

---

### Delete Scoped Package Version From Recycle Bin
- **Description:** Delete a package version with an npm scope from the recycle bin. The project parameter must be supplied if the feed was created in a project.
- **Method:** DELETE
- **Endpoint:**
  `/feeds/{feedId}/npm/RecycleBin/packages/{scope}.{packageName}/versions/{packageVersion}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `scope` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X DELETE \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/npm/RecycleBin/packages/{scope}.{packageName}/versions/{packageVersion}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "id": "@{scope}/{packageName}",
  "version": "{packageVersion}",
  "permanentlyDeleted": true
}
```

---

### Download Package
- **Description:** Get an unscoped npm package. The project parameter must be supplied if the feed was created in a project.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/npm/packages/{packageName}/versions/{packageVersion}/content/{filePath}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `filePath` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/npm/packages/{packageName}/versions/{packageVersion}/content/{filePath}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "downloadUrl": "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/npm/packages/{packageName}/versions/{packageVersion}/content/{filePath}"
}
```

---

### Download Scoped Package

#### Get Package Version
- **Description:** Get information about a Universal package version. The project parameter must be supplied if the feed was created in a project.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/upack/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/upack/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "id": "{packageName}",
  "version": "{packageVersion}",
  "state": "active"
}
```
```

---

### 4. NuGet

### Delete Package Version
- **Description:** Delete a NuGet package version from the feed and move it to the feed's recycle bin. The project parameter must be supplied if the feed was created in a project.
- **Method:** DELETE
- **Endpoint:**
  `/feeds/{feedId}/nuget/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X DELETE \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/nuget/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "id": "{packageName}",
  "version": "{packageVersion}",
  "deleted": true
}
```

---

### Delete Package Version From Recycle Bin
- **Description:** Permanently delete a NuGet package version from a feed's recycle bin. The project parameter must be supplied if the feed was created in a project.
- **Method:** DELETE
- **Endpoint:**
  `/feeds/{feedId}/nuget/RecycleBin/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X DELETE \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/nuget/RecycleBin/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "id": "{packageName}",
  "version": "{packageVersion}",
  "permanentlyDeleted": true
}
```

---


### 5. Python

#### Delete Package Version
- **Description:** Delete a Python package version from the feed and move it to the feed's recycle bin. The project parameter must be supplied if the feed was created in a project.
- **Method:** DELETE
- **Endpoint:**
  `/feeds/{feedId}/pypi/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X DELETE \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/pypi/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "id": "{packageName}",
  "version": "{packageVersion}",
  "deleted": true
}
```

---

#### Delete Package Version From Recycle Bin
- **Description:** Permanently delete a Python package version from a feed's recycle bin. The project parameter must be supplied if the feed was created in a project.
- **Method:** DELETE
- **Endpoint:**
  `/feeds/{feedId}/pypi/RecycleBin/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X DELETE \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/pypi/RecycleBin/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "id": "{packageName}",
  "version": "{packageVersion}",
  "permanentlyDeleted": true
}
```

---

#### Download Package
- **Description:** Download a Python package file. Returns the URL of the requested package file.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/pypi/packages/{packageName}/versions/{packageVersion}/content/{filePath}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `filePath` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/pypi/packages/{packageName}/versions/{packageVersion}/content/{filePath}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "downloadUrl": "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/pypi/packages/{packageName}/versions/{packageVersion}/content/{filePath}"
}
```

---

#### Get Package Version
- **Description:** Get information about a Python package version. The project parameter must be supplied if the feed was created in a project.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/pypi/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/pypi/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "id": "{packageName}",
  "version": "{packageVersion}",
  "state": "active"
}
```

---

#### Get Package Version From Recycle Bin
- **Description:** Get information about a Python package version in the recycle bin. The project parameter must be supplied if the feed was created in a project.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/pypi/RecycleBin/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/pypi/RecycleBin/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "id": "{packageName}",
  "version": "{packageVersion}",
  "state": "deleted"
}
```

---

#### Get Upstreaming Behavior
- **Description:** Get the upstreaming behavior of a Python package within the context of a feed.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/pypi/packages/{packageName}/upstreaming?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/pypi/packages/{packageName}/upstreaming?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "upstreamSource": "public",
  "behavior": "allow"
}
```

---

#### Restore Package Version From Recycle Bin
- **Description:** Restore a Python package version from the recycle bin to its associated feed. The project parameter must be supplied if the feed was created in a project.
- **Method:** PATCH
- **Endpoint:**
  `/feeds/{feedId}/pypi/RecycleBin/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X PATCH \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/pypi/RecycleBin/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "id": "{packageName}",
  "version": "{packageVersion}",
  "restored": true
}
```

---

#### Set Upstreaming Behavior
- **Description:** Set the upstreaming behavior of a Python package within the context of a feed. The package does not need to exist in the feed prior to setting the behavior.
- **Method:** PUT
- **Endpoint:**
  `/feeds/{feedId}/pypi/packages/{packageName}/upstreaming?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)
- **Body:**
  - Upstreaming behavior details (see official docs for schema)

**Example cURL:**
```bash
curl -X PUT \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"behavior": "allow", "upstreamSource": "public"}' \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/pypi/packages/{packageName}/upstreaming?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "upstreamSource": "public",
  "behavior": "allow"
}
```

---

#### Update Package Version
- **Description:** Update state for a Python package version. The project parameter must be supplied if the feed was created in a project.
- **Method:** PATCH
- **Endpoint:**
  `/feeds/{feedId}/pypi/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)
- **Body:**
  - State update details (see official docs for schema)

**Example cURL:**
```bash
curl -X PATCH \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"state": "unlisted"}' \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/pypi/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "id": "{packageName}",
  "version": "{packageVersion}",
  "state": "unlisted"
}
```

---

#### Update Package Versions
- **Description:** Update several Python packages from a single feed in a single request. The updates to the packages do not happen atomically. The project parameter must be supplied if the feed was created in a project.
- **Method:** PATCH
- **Endpoint:**
  `/feeds/{feedId}/pypi/packages/versionsBatch?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)
- **Body:**
  - Batch update details (see official docs for schema)

**Example cURL:**
```bash
curl -X PATCH \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"updates": [{"packageName": "foo", "packageVersion": "1.0.0", "state": "unlisted"}]}' \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/pypi/packages/versionsBatch?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "updated": [
    {"packageName": "foo", "packageVersion": "1.0.0", "state": "unlisted"}
  ]
}
```

---

#### Update Recycle Bin Package Versions
- **Description:** Delete or restore several Python package versions from the recycle bin. The project parameter must be supplied if the feed was created in a project.
- **Method:** PATCH
- **Endpoint:**
  `/feeds/{feedId}/pypi/RecycleBin/packages/versionsBatch?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)
- **Body:**
  - Batch recycle bin update details (see official docs for schema)

**Example cURL:**
```bash
curl -X PATCH \
  -H "Authorization: Bearer <PAT>" \
  -H "Content-Type: application/json" \
  -d '{"actions": [{"packageName": "foo", "packageVersion": "1.0.0", "action": "restore"}]}' \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/pypi/RecycleBin/packages/versionsBatch?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "restored": [
    {"packageName": "foo", "packageVersion": "1.0.0"}
  ]
}
```

---



## Documentation Progress

- [x] Structure and sub-area listing
- [x] Cargo (all operations)
- [x] Maven (all operations)
- [x] Npm (all operations)
- [x] NuGet (all operations)
- [x] Python (all operations)
- [ ] Universal (all operations)
- [ ] All operations fully documented

## 6. Universal


#### Delete Package Version
- **Description:** Delete a Universal package version from the feed and move it to the feed's recycle bin. The project parameter must be supplied if the feed was created in a project.
- **Method:** DELETE
- **Endpoint:**
  `/feeds/{feedId}/upack/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X DELETE \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/upack/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "id": "{packageName}",
  "version": "{packageVersion}",
  "deleted": true
}
```

---

#### Delete Package Version From Recycle Bin
- **Description:** Permanently delete a Universal package version from a feed's recycle bin. The project parameter must be supplied if the feed was created in a project.
- **Method:** DELETE
- **Endpoint:**
  `/feeds/{feedId}/upack/RecycleBin/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X DELETE \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/upack/RecycleBin/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "id": "{packageName}",
  "version": "{packageVersion}",
  "permanentlyDeleted": true
}
```

---

## 2. Maven

### Delete Package Version
- **Description:** Delete a Maven package version from the feed and move it to the feed's recycle bin. The project parameter must be supplied if the feed was created in a project.
- **Method:** DELETE
- **Endpoint:**
  `/feeds/{feedId}/maven/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X DELETE \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/maven/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "id": "{packageName}",
  "version": "{packageVersion}",
  "deleted": true
}
```

---

#### Delete Package Version From Recycle Bin

#### Download Package
- **Description:** Download a Universal package file. Returns the URL of the requested package file.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/upack/packages/{packageName}/versions/{packageVersion}/content/{filePath}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `filePath` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/upack/packages/{packageName}/versions/{packageVersion}/content/{filePath}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "downloadUrl": "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/upack/packages/{packageName}/versions/{packageVersion}/content/{filePath}"
}
```

---
- **Endpoint:**
  `/feeds/{feedId}/maven/RecycleBin/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X DELETE \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/maven/RecycleBin/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "id": "{packageName}",
  "version": "{packageVersion}",
  "permanentlyDeleted": true
}
```

---

### Download Package
- **Description:** Download a Maven package file. Returns the URL of the requested package file.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/maven/packages/{packageName}/versions/{packageVersion}/content/{filePath}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `filePath` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/maven/packages/{packageName}/versions/{packageVersion}/content/{filePath}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "downloadUrl": "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/maven/packages/{packageName}/versions/{packageVersion}/content/{filePath}"
}
```

---

### Get Package Version
- **Description:** Get information about a Maven package version. The project parameter must be supplied if the feed was created in a project.
- **Method:** GET
- **Endpoint:**
  `/feeds/{feedId}/maven/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview`
- **Parameters:**
  - `feedId` (string, required)
  - `packageName` (string, required)
  - `packageVersion` (string, required)
  - `api-version` (string, required)
  - `project` (string, required if feed is project-scoped)

**Example cURL:**
```bash
curl -X GET \
  -H "Authorization: Bearer <PAT>" \
  "https://feeds.dev.azure.com/{organization}/{project}/_apis/packaging/feeds/{feedId}/maven/packages/{packageName}/versions/{packageVersion}?api-version=7.2-preview"
```

**Sample Response:**
```json
{
  "id": "{packageName}",
  "version": "{packageVersion}",
  "state": "active"
}
```

---
