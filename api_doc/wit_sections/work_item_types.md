# Work Item Types

Permite consultar los tipos de work item definidos en un proyecto de Azure DevOps, así como sus propiedades y metadatos. Útil para construir interfaces dinámicas y validaciones de procesos.

**Documentación oficial:** [Work Item Types](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/work-item-types?view=azure-devops-rest-7.2)

## Operaciones principales

| Operación | Endpoint                                                                                                 | Método |
|-----------|----------------------------------------------------------------------------------------------------------|--------|
| List      | /{organization}/{project}/_apis/wit/workitemtypes?api-version=7.2-preview                                | GET    |
| Get       | /{organization}/{project}/_apis/wit/workitemtypes/{type}?api-version=7.2-preview                         | GET    |

## Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                 |
|----------------|-----------|-----------|--------|---------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps   |
| project        | path      | Sí        | string | Nombre o ID del proyecto                    |
| type           | path      | No (solo para Get) | string | Nombre del tipo de work item a consultar |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview'       |

## Ejemplo de consumo con cURL (List)

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/workitemtypes?api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

## Ejemplo de consumo con cURL (Get)

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/workitemtypes/Bug?api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

## Ejemplo de respuesta (List)

```json
{
  "count": 2,
  "value": [
    {
      "name": "Bug",
      "referenceName": "Microsoft.VSTS.WorkItemTypes.Bug",
      "description": "Rastrea errores y defectos.",
      "color": "b2b2b2"
    },
    {
      "name": "User Story",
      "referenceName": "Microsoft.VSTS.WorkItemTypes.UserStory",
      "description": "Rastrea historias de usuario.",
      "color": "007acc"
    }
  ]
}
```

## Filtros y propiedades a incluir

  - `value`: array de tipos de work item, cada uno con:
    - `name`: nombre del tipo (string)
    - `referenceName`: nombre de referencia (string)
    - `description`: descripción (string)
    - `color`: color asociado (string, hex)

## Notas

- Los tipos de work item definen la estructura y reglas de los elementos de trabajo.
- El scope mínimo requerido es `vso.work` (lectura de work items y metadatos).
