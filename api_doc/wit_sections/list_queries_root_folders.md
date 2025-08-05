# List Queries/Root Folders

Permite listar las carpetas raíz de queries y sus subcarpetas en un proyecto de Azure DevOps.

**Endpoint:**
```
GET /{organization}/{project}/_apis/wit/queries?api-version=7.2-preview[&$expand=none|clauses|all|wiql][&depth=int][&includeDeleted=true|false][&queryType=flat|tree|oneHop]
```

**Ejemplo cURL:**
```bash
curl -X GET \
  -H "Authorization: Basic <PAT>" \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/queries?$expand=all&api-version=7.2-preview'
```

**Ejemplo de respuesta:**
```json
{
  "count": 2,
  "value": [
    { "id": "b1c7e6b2-6e2a-4e2e-8e2e-1234567890ab", "name": "Mis Bugs Abiertos", ... },
    { "id": "c2d7e6b2-6e2a-4e2e-8e2e-1234567890ac", "name": "Mis Tareas", ... }
  ]
}
```

---

## Filtros y propiedades a incluir

- `$expand`: none, clauses, all, wiql
- `queryType`: flat, tree, oneHop
- `errorPolicy` (batch): fail, omit
- `includeDeleted`: true, false
- `depth`: int
- `searchText`: string (búsqueda)
- `top`: int (límite de resultados en búsqueda)
