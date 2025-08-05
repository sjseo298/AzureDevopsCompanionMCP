---

# Reporting Work Item Revisions

Permite obtener lotes de revisiones de work items para construir un almacén de datos, sincronizar integraciones o auditar cambios. Devuelve revisiones completas, soporta paginación y permite filtrar por campos, tipos, fecha, borrados, identidad, etc.

**Documentación oficial:** [Reporting Work Item Revisions](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/reporting-work-item-revisions?view=azure-devops-rest-7.2)

## Operaciones principales

| Operación | Endpoint                                                                                                 | Método |
|-----------|----------------------------------------------------------------------------------------------------------|--------|
| Get       | /{organization}/{project}/_apis/wit/reporting/workitemrevisions?api-version=7.2-preview.2                | GET    |
| Post      | /{organization}/{project}/_apis/wit/reporting/workitemrevisions?api-version=7.2-preview.2                | POST   |

---

## Parámetros de URI (GET y POST)

| Nombre                        | Ubicación | Requerido | Tipo                        | Descripción                                                                                 |
|-------------------------------|-----------|-----------|-----------------------------|---------------------------------------------------------------------------------------------|
| organization                  | path      | Sí        | string                      | Nombre de la organización de Azure DevOps                                                   |
| project                       | path      | No        | string                      | Nombre o ID del proyecto (opcional, para toda la colección omitir)                          |
| api-version                   | query     | Sí        | string                      | Versión de la API. Usar '7.2-preview.2'                                                     |
| continuationToken             | query     | No        | string                      | Token de continuación para paginación. Omitir para el primer lote.                          |
| startDateTime                 | query     | No        | string (date-time)          | Fecha/hora de inicio para filtrar revisiones posteriores a ese momento.                     |
| $expand                       | query     | No        | enum (ver abajo)            | Expande todos los campos, incluyendo long text. Valores: 'none', 'fields'.                 |
| $maxPageSize                  | query     | No        | int                         | Máximo de resultados por lote.                                                              |

### Parámetros adicionales GET
| fields                        | query     | No        | array(string)               | Lista de campos a incluir en la respuesta.                                                  |
| types                         | query     | No        | array(string)               | Lista de tipos de work item a incluir.                                                      |
| includeDeleted                | query     | No        | bool                        | Incluir work items eliminados.                                                              |
| includeDiscussionChangesOnly  | query     | No        | bool                        | Solo revisiones donde solo cambió el campo 'history'.                                       |
| includeIdentityRef            | query     | No        | bool                        | Devolver referencias de identidad en vez de string.                                         |
| includeLatestOnly             | query     | No        | bool                        | Solo la última revisión de cada work item.                                                  |
| includeTagRef                 | query     | No        | bool                        | Incluir objetos de tag para System.Tags.                                                    |

### Parámetros adicionales POST (en el body)
| fields                        | body      | No        | array(string)               | Lista de campos a incluir en la respuesta.                                                  |
| types                         | body      | No        | array(string)               | Lista de tipos de work item a incluir.                                                      |
| includeDeleted                | body      | No        | bool                        | Incluir work items eliminados.                                                              |
| includeIdentityRef            | body      | No        | bool                        | Devolver referencias de identidad en vez de string.                                         |
| includeLatestOnly             | body      | No        | bool                        | Solo la última revisión de cada work item.                                                  |
| includeTagRef                 | body      | No        | bool                        | Incluir objetos de tag para System.Tags.                                                    |

---

## Enums y valores posibles

**ReportingRevisionsExpand:**
- none (por defecto)
- fields

---

## Ejemplo de consumo con cURL (GET)

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/reporting/workitemrevisions?api-version=7.2-preview.2' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

**Con filtros:**
```bash
curl -X GET \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/reporting/workitemrevisions?fields=System.Title,System.State&types=Bug,Task&includeIdentityRef=true&includeDeleted=true&$expand=fields&api-version=7.2-preview.2' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

## Ejemplo de consumo con cURL (POST)

```bash
curl -X POST \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/reporting/workitemrevisions?api-version=7.2-preview.2' \
  -H 'Authorization: Basic <BASE64_PAT>' \
  -H 'Content-Type: application/json' \
  -d '{
    "types": ["Bug", "Task", "Product Backlog Item"],
    "fields": ["System.WorkItemType", "System.Title", "System.AreaPath"],
    "includeIdentityRef": true
  }'
```

---

## Ejemplo de respuesta

```json
{
  "values": [
    {
      "id": 3,
      "rev": 8,
      "fields": {
        "System.Id": 3,
        "System.AreaPath": "Fabrikam-Fiber-Git",
        "System.TeamProject": "Fabrikam-Fiber-Git",
        "System.Rev": 8,
        "System.RevisedDate": "9999-01-01T00:00:00Z",
        "System.IterationPath": "Fabrikam-Fiber-Git\\Release 1\\Sprint 1",
        "System.WorkItemType": "Product Backlog Item",
        "System.State": "Done",
        "System.Reason": "Work finished",
        "System.CreatedDate": "2014-03-18T17:17:06.857Z",
        "System.CreatedBy": {
          "id": "d291b0c4-a05c-4ea6-8df1-4b41d5f39eff",
          "uniqueName": "Jamal Hartnett <fabrikamfiber4@hotmail.com>"
        },
        "System.ChangedDate": "2015-06-23T18:25:16.137Z",
        "System.ChangedBy": {
          "id": "d6245f20-2af8-44f4-9451-8107cb2767db",
          "uniqueName": "Normal Paulk <fabrikamfiber16@hotmail.com>"
        },
        "System.Title": "Technician can submit invoices on Windows Phone",
        "System.BoardColumn": "Done",
        "Microsoft.VSTS.Common.ClosedDate": "2014-03-18T17:19:02.093Z",
        "Microsoft.VSTS.Common.Priority": 3,
        "Microsoft.VSTS.Common.BacklogPriority": 1000063244
      }
    }
  ],
  "nextLink": "https://dev.azure.com/{organization}/{project}/_apis/wit/reporting/workItemRevisions?continuationToken=842;5;3&api-version=7.2-preview.2",
  "continuationToken": "842;5;3",
  "isLastBatch": true
}
```

---

## Filtros y propiedades a incluir

- `fields`: lista de campos a devolver (ejemplo: 'System.Title', 'System.State', etc.).
- `types`: lista de tipos de work item (ejemplo: 'Bug', 'Task', 'User Story', etc.).
- `continuationToken`: para paginación, úsalo en la siguiente llamada usando el valor de `nextLink`.
- `startDateTime`: filtra solo revisiones después de la fecha/hora indicada.
- `includeIdentityRef`: devuelve referencias de identidad en vez de string.
- `includeDeleted`: incluye work items eliminados.
- `includeLatestOnly`: solo la última revisión de cada work item.
- `includeTagRef`: incluye objetos de tag para System.Tags.
- `includeDiscussionChangesOnly`: solo revisiones donde solo cambió el campo 'history'.
- `$expand`: 'none' o 'fields'.
- `$maxPageSize`: máximo de resultados por lote.
- El resultado incluye:
  - `values`: array de revisiones, cada una con:
    - `id`: ID del work item
    - `rev`: número de revisión
    - `fields`: objeto con los campos solicitados y sus valores
  - `nextLink`: URL para obtener el siguiente lote (string)
  - `continuationToken`: token para paginación
  - `isLastBatch`: indica si es el último lote disponible (bool)

---

## Notas de paginación y uso

- Para obtener todas las revisiones, realiza llamadas sucesivas usando el `nextLink` hasta que `isLastBatch` sea `true`.
- Si omites `project`, la consulta es a nivel de toda la colección.
- El scope mínimo requerido es `vso.work` (lectura de work items y metadatos).
- El endpoint POST es útil si la lista de campos es muy grande y puede exceder el límite de longitud de URL.

---
