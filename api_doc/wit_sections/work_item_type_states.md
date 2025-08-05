# Work Item Type States

Permite consultar los estados definidos para un tipo de work item en un proyecto de Azure DevOps, incluyendo nombre, color y orden. Útil para construir flujos de trabajo y validaciones.

**Documentación oficial:** [Work Item Type States](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/work-item-type-states?view=azure-devops-rest-7.2)

## Operaciones principales

| Operación | Endpoint                                                                                                 | Método |
|-----------|----------------------------------------------------------------------------------------------------------|--------|
| List      | /{organization}/{project}/_apis/wit/workitemtypes/{type}/states?api-version=7.2-preview                  | GET    |

## Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                 |
|----------------|-----------|-----------|--------|---------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps   |
| project        | path      | Sí        | string | Nombre o ID del proyecto                    |
| type           | path      | Sí        | string | Nombre del tipo de work item                |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview'       |

## Ejemplo de consumo con cURL

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/workitemtypes/Bug/states?api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

## Ejemplo de respuesta

```json
{
  "count": 3,
  "value": [
    {
      "name": "New",
      "color": "b2b2b2",
      "order": 1
    },
    {
      "name": "Active",
      "color": "007acc",
      "order": 2
    },
    {
      "name": "Closed",
      "color": "339933",
      "order": 3
    }
  ]
}
```

## Filtros y propiedades a incluir

  - `value`: array de estados, cada uno con:
    - `name`: nombre del estado (string)
    - `color`: color asociado (string, hex)
    - `order`: orden del estado (int)

## Notas

- Los estados definen el ciclo de vida de cada tipo de work item.
- El scope mínimo requerido es `vso.work` (lectura de work items y metadatos).
