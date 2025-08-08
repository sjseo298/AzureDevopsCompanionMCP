# Accounts API Documentation (Azure DevOps REST API v7.2)

## What is the Accounts API?
The Accounts API allows you to list and retrieve Azure DevOps accounts that a user has access to. This is useful for discovering organizations and managing access at the account/organization level.

**Official Docs:** [Accounts API Reference](https://learn.microsoft.com/en-us/rest/api/azure/devops/accounts/?view=azure-devops-rest-7.2)

---

## Operations

### 0. Get My MemberId (helper)
- Description: Retrieves the memberId (GUID) of the authenticated user, required by some Accounts queries.
- Endpoint: `GET https://app.vssps.visualstudio.com/_apis/profile/profiles/me?api-version=7.1`
- Tool MCP: `azuredevops_get_my_memberid`
- cURL Example:
```bash
curl -u :$AZURE_DEVOPS_PAT "https://app.vssps.visualstudio.com/_apis/profile/profiles/me?api-version=7.1"
```
- Response sample (fields): `id`, `displayName`, `emailAddress`

### 1. List Accounts
- **Description:** Retrieves all accounts that the authenticated user has access to.
- **Endpoint:**
  - `GET https://app.vssps.visualstudio.com/_apis/accounts?api-version=7.1`
- **Parameters:**
  - `ownerId` (optional, string): Filter by owner GUID.
  - `memberId` (optional, string): Filter by member GUID.
  - `properties` (optional, string): Comma-separated list of additional properties to return.
- **cURL Example:**
```bash
curl -u :<PAT> \
  "https://app.vssps.visualstudio.com/_apis/accounts?memberId=<guid>&api-version=7.1"
```
- **Sample Response:**
```json
{
  "count": 1,
  "value": [
    {
      "accountId": "<guid>",
      "accountName": "my-organization",
      "organizationName": "my-organization",
      "createdDate": "2020-01-01T00:00:00Z",
      "accountType": "personal"
    }
  ]
}
```

---

### 2. Get Account
- **Description:** Retrieves a specific account by its ID.
- **Endpoint:**
  - `GET https://app.vssps.visualstudio.com/_apis/accounts/{accountId}?api-version=7.1`
- **Parameters:**
  - `accountId` (required, string): The GUID of the account.
- **cURL Example:**
```bash
curl -u :<PAT> \
  "https://app.vssps.visualstudio.com/_apis/accounts/<accountId>?api-version=7.1"
```
- **Sample Response:**
```json
{
  "accountId": "<guid>",
  "accountName": "my-organization",
  "organizationName": "my-organization",
  "createdDate": "2020-01-01T00:00:00Z",
  "accountType": "personal"
}
```

---

## Progress
- [x] Operations listed
- [x] Detailed documentation for each operation
- [x] Helper documented: get_my_memberid (Profiles)
