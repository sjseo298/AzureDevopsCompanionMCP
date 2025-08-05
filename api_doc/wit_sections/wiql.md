
# WIQL (Work Item Query Language)

Permite ejecutar consultas sobre work items usando WIQL, ya sea por texto o por ID de query guardada.

**Documentación oficial:** [WIQL](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/wiql?view=azure-devops-rest-7.2)

Más sobre la sintaxis de WIQL: [WIQL Syntax](https://learn.microsoft.com/en-us/vsts/collaborate/wiql-syntax?view=azure-devops-rest-7.2)

## Operaciones principales

| Operación         | Endpoint                                                                                                 | Método |
|-------------------|----------------------------------------------------------------------------------------------------------|--------|
| Query By Id       | /{organization}/{project}/_apis/wit/wiql/{id}?api-version=7.2-preview                                    | GET    |
| Query By Wiql     | /{organization}/{project}/_apis/wit/wiql?api-version=7.2-preview                                         | POST   |

## Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                 |
|----------------|-----------|-----------|--------|---------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps   |
| project        | path      | Sí        | string | Nombre o ID del proyecto                    |
| id             | path      | No (solo para Query By Id) | string | ID de la query guardada                 |
| api-version    | query     | Sí        | string | Usar '7.2-preview'                          |

## Cuerpo de la petición (Query By Wiql)

```json
{
  "query": "SELECT [System.Id], [System.Title] FROM WorkItems WHERE [System.State] = 'Active'"
}
```

## Ejemplo de uso (Query By Wiql)

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic <PAT>" \
  -d '{
    "query": "SELECT [System.Id], [System.Title] FROM WorkItems WHERE [System.State] = 'Active'"
  }' \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/wiql?api-version=7.2-preview'
```

## Ejemplo de respuesta

```json
{
  "queryType": "flat",
  "columns": [
    { "referenceName": "System.Id", "name": "ID" },
    { "referenceName": "System.Title", "name": "Title" }
  ],
  "workItems": [
    { "id": 1, "url": "..." },
    { "id": 2, "url": "..." }
  ]
}
```

## Notas

- Permite ejecutar queries complejas sobre work items.
- El scope mínimo requerido es `vso.work`.

# Reemplazar plantilla

**Endpoint:**
```
PUT https://dev.azure.com/{organization}/{project}/{team}/_apis/wit/templates/{templateId}?api-version=7.2-preview.1
```

**Body de la petición:**
```json
{
  "fields": {
    "System.State": "Replaced"
  },
  "name": "New Test Template",
  "description": "Replacing template",
  "workItemTypeName": "Feature"
}
```

**Ejemplo de uso con curl:**
```bash
curl -X PUT \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "fields": {"System.State": "Replaced"},
    "name": "New Test Template",
    "description": "Replacing template",
    "workItemTypeName": "Feature"
  }' \
  "https://dev.azure.com/fabrikam/Fabrikam-Fiber-Git/MyTeam/_apis/wit/templates/05132b3a-41d6-430f-8738-42b20b34a601?api-version=7.2-preview.1"
```

**Respuesta de ejemplo:**
```json
{
  "fields": {
    "System.State": "Replaced"
  },
  "id": "05132b3a-41d6-430f-8738-42b20b34a601",
  "name": "New Test Template",
  "description": "Replacing template",
  "workItemTypeName": "Feature",
  "url": "https://dev.azure.com/fabrikam/Fabrikam-Fiber-Git/MyTeam/_apis/wit/templates/05132b3a-41d6-430f-8738-42b20b34a601"
}
```

---

# Eliminar plantilla

**Endpoint:**
```
DELETE https://dev.azure.com/{organization}/{project}/{team}/_apis/wit/templates/{templateId}?api-version=7.2-preview.1
```

**Ejemplo de uso con curl:**
```bash
curl -X DELETE \
  -H "Authorization: Bearer <TOKEN>" \
  "https://dev.azure.com/fabrikam/Fabrikam-Fiber-Git/MyTeam/_apis/wit/templates/05132b3a-41d6-430f-8738-42b20b34a601?api-version=7.2-preview.1"
```

**Respuesta de ejemplo:**
```json
{}
```

---
