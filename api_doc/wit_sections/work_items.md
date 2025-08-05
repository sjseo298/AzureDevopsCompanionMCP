# Work Items

Permite crear, consultar, actualizar y eliminar work items en Azure DevOps. Es la API principal para gestionar elementos de trabajo, soportando operaciones individuales y por lotes.

**Documentación oficial:** [Work Items](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/work-items?view=azure-devops-rest-7.2)

## Operaciones principales

| Operación              | Endpoint                                                                                                 | Método |
|------------------------|--------------------------------------------------------------------------------------------------------|--------|
| Create                 | /{organization}/{project}/_apis/wit/workitems/${type}?api-version=7.2-preview                            | POST   |
| Get Work Item          | /{organization}/{project}/_apis/wit/workitems/{id}?api-version=7.2-preview                               | GET    |
| Get Work Items Batch   | /{organization}/{project}/_apis/wit/workitemsbatch?api-version=7.2-preview                               | POST   |
| List                   | /{organization}/{project}/_apis/wit/workitems?ids={ids}&api-version=7.2-preview                          | GET    |
| Update                 | /{organization}/{project}/_apis/wit/workitems/{id}?api-version=7.2-preview                               | PATCH  |
| Delete                 | /{organization}/{project}/_apis/wit/workitems/{id}?api-version=7.2-preview                               | DELETE |
| Delete Work Items      | /{organization}/{project}/_apis/wit/workitems?ids={ids}&api-version=7.2-preview                          | DELETE |
| Get Work Item Template | /{organization}/{project}/_apis/wit/workitems/{id}/templates/{template}?api-version=7.2-preview          | GET    |

## Parámetros de URI comunes

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                 |
|----------------|-----------|-----------|--------|---------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps   |
| project        | path      | Sí        | string | Nombre o ID del proyecto                    |
| id(s)          | path/query| Sí        | int/list| ID(s) de los work items                     |
| type           | path      | Sí (POST) | string | Tipo de work item a crear                   |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview'       |

## Ejemplo de consumo con cURL (Crear)

```bash
curl -X POST \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/workitems/$Bug?api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>' \
  -H 'Content-Type: application/json-patch+json' \
  -d '[{"op": "add", "path": "/fields/System.Title", "value": "Nuevo bug"}]'
```

## Ejemplo de consumo con cURL (Consultar)

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/workitems/123?api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

## Ejemplo de respuesta (Get Work Item)

```json
{
  "id": 123,
  "rev": 1,
  "fields": {
    "System.Title": "Nuevo bug",
    "System.State": "New"
  },
  "url": "https://dev.azure.com/org/proj/_apis/wit/workitems/123"
}
```

## Filtros y propiedades a incluir

  - `id`: identificador del work item (int)
  - `fields`: objeto con los campos y valores del work item
  - `rev`: número de revisión (int)
  - `url`: URL del recurso (string)

## Notas

- Permite gestionar el ciclo de vida completo de los work items.
- El scope mínimo requerido es `vso.work` (lectura y escritura de work items).
