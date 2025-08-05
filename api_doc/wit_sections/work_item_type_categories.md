# Work Item Type Categories

Permite consultar las categorías de tipos de work item definidas en un proyecto de Azure DevOps. Las categorías agrupan tipos de work item para facilitar la gestión y configuración de procesos.

**Documentación oficial:** [Work Item Type Categories](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/work-item-type-categories?view=azure-devops-rest-7.2)

## Operaciones principales

| Operación | Endpoint                                                                                                 | Método |
|-----------|----------------------------------------------------------------------------------------------------------|--------|
| List      | /{organization}/{project}/_apis/wit/workitemtypecategories?api-version=7.2-preview                       | GET    |
| Get       | /{organization}/{project}/_apis/wit/workitemtypecategories/{category}?api-version=7.2-preview            | GET    |

## Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                 |
|----------------|-----------|-----------|--------|---------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps   |
| project        | path      | Sí        | string | Nombre o ID del proyecto                    |
| category       | path      | No (solo para Get) | string | Nombre de la categoría a consultar        |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview'       |

## Ejemplo de consumo con cURL (List)

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/workitemtypecategories?api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

## Ejemplo de consumo con cURL (Get)

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/workitemtypecategories/{category}?api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

## Ejemplo de respuesta (List)

```json
{
  "count": 2,
  "value": [
    {
      "name": "Microsoft.RequirementCategory",
      "referenceName": "Microsoft.RequirementCategory",
      "defaultWorkItemType": "User Story",
      "workItemTypes": [
        { "name": "User Story" },
        { "name": "Bug" }
      ]
    },
    {
      "name": "Microsoft.TaskCategory",
      "referenceName": "Microsoft.TaskCategory",
      "defaultWorkItemType": "Task",
      "workItemTypes": [
        { "name": "Task" }
      ]
    }
  ]
}
```

## Filtros y propiedades a incluir

  - `value`: array de categorías, cada una con:
    - `name`: nombre de la categoría (string)
    - `referenceName`: nombre de referencia (string)
    - `defaultWorkItemType`: tipo de work item por defecto (string)
    - `workItemTypes`: array de tipos de work item asociados

## Notas

- Las categorías permiten agrupar tipos de work item para reglas de proceso y configuración.
- El scope mínimo requerido es `vso.work` (lectura de work items y metadatos).
