# Search Queries

Permite buscar queries existentes en un proyecto de Azure DevOps mediante texto.

**Endpoint:**
```
POST /{organization}/{project}/_apis/wit/queries/$search?api-version=7.2-preview
```

**Request Body:**
```json
{
  "searchText": "string",
  "$expand": "none|clauses|all|wiql",
  "top": 10
}
```

**Ejemplo cURL:**
```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic <PAT>" \
  -d '{
    "searchText": "Bugs",
    "$expand": "all",
    "top": 5
  }' \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/queries/$search?api-version=7.2-preview'
```

**Ejemplo de respuesta:**
```json
{
  "count": 1,
  "value": [
    { "id": "b1c7e6b2-6e2a-4e2e-8e2e-1234567890ab", "name": "Mis Bugs Abiertos", ... }
  ]
}
```
