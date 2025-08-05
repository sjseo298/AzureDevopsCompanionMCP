

# Update Query / Folder / Rename / Undelete

Permite actualizar, renombrar, mover o restaurar (undelete) queries y carpetas de queries en Azure DevOps.

**Documentación oficial:** [Queries - Update](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/queries/update?view=azure-devops-rest-7.2)

## Endpoint principal

```
PATCH /{organization}/{project}/_apis/wit/queries/{query}?api-version=7.2-preview.2
```

Parámetro opcional para restaurar descendientes:
```
PATCH /{organization}/{project}/_apis/wit/queries/{query}?$undeleteDescendants=true&api-version=7.2-preview.2
```

## Parámetros de URI

| Nombre                | Ubicación | Requerido | Tipo     | Descripción |
|-----------------------|-----------|-----------|----------|-------------|
| organization          | path      | Sí        | string   | Organización |
| project               | path      | Sí        | string   | Proyecto     |
| query                 | path      | Sí        | string   | ID o ruta de la query o carpeta |
| api-version           | query     | Sí        | string   | Usar '7.2-preview.2' |
| $undeleteDescendants  | query     | No        | boolean  | Restaurar hijos (solo undelete) |

## Cuerpo de la petición (Request Body)

```json
{
  "name": "Nuevo Nombre",           // Renombrar query o carpeta
  "wiql": "string",                // Solo para queries
  "isFolder": true|false,           // True para carpeta, false para query
  "queryType": "flat|tree|oneHop", // Tipo de query
  "isDeleted": false                // Para restaurar (undelete)
  // ...otros campos opcionales
}
```

## Ejemplos de uso

### Renombrar una carpeta o query

```bash
curl -X PATCH \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic <PAT>" \
  -d '{
    "name": "Website"
  }' \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/queries/OldName?api-version=7.2-preview.2'
```

### Restaurar (undelete) una query o carpeta (y opcionalmente sus hijos)

```bash
curl -X PATCH \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic <PAT>" \
  -d '{
    "isDeleted": false
  }' \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/queries/CarpetaEliminada?$undeleteDescendants=true&api-version=7.2-preview.2'
```

### Actualizar el WIQL de una query

```bash
curl -X PATCH \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic <PAT>" \
  -d '{
    "wiql": "SELECT [System.Id], [System.Title] FROM WorkItems WHERE [System.State] = 'Active'"
  }' \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/queries/ActiveBugs?api-version=7.2-preview.2'
```

## Ejemplo de respuesta

```json
{
  "id": "8a8c8212-15ca-41ed-97aa-1d6fbfbcd581",
  "name": "Website",
  "path": "Shared Queries/Website",
  "createdDate": "2016-06-01T16:58:56.323Z",
  "lastModifiedBy": {
    "displayName": "Jamal Hartnett",
    "uniqueName": "fabrikamfiber4@hotmail.com"
  },
  "lastModifiedDate": "2016-06-01T16:58:59.077Z",
  "isFolder": true,
  "hasChildren": true,
  "isPublic": true,
  "wiql": "SELECT ...",
  "queryType": "flat",
  // ...otros campos
}
```

## Filtros y propiedades relevantes

- `$expand`: none, clauses, all, wiql
- `queryType`: flat, tree, oneHop
- `errorPolicy` (batch): fail, omit
- `includeDeleted`: true, false
- `depth`: int
- `searchText`: string (búsqueda)
- `top`: int (límite de resultados en búsqueda)

## Notas

- El scope mínimo requerido es `vso.work_write`.
- Restaurar (undelete) no recupera permisos previos de los hijos.

# Filtros y propiedades a incluir

- `$expand`: none, clauses, all, wiql
- `queryType`: flat, tree, oneHop
- `errorPolicy` (batch): fail, omit
- `includeDeleted`: true, false
- `depth`: int
- `searchText`: string (búsqueda)
- `top`: int (límite de resultados en búsqueda)

---
