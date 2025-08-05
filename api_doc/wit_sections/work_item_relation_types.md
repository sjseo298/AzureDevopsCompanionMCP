# Work Item Relation Types

Permite obtener los tipos de relaciones entre work items definidos en Azure DevOps. Estas relaciones determinan cómo se vinculan los work items entre sí (por ejemplo, dependencia, jerarquía, etc.).

**Documentación oficial:** [Work Item Relation Types](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/work-item-relation-types?view=azure-devops-rest-7.2)

## Operaciones principales

| Operación | Endpoint                                                                                                 | Método |
|-----------|----------------------------------------------------------------------------------------------------------|--------|
| List     | /{organization}/_apis/wit/workitemrelationtypes?api-version=7.2-preview                                   | GET    |
| Get      | /{organization}/_apis/wit/workitemrelationtypes/{relationType}?api-version=7.2-preview                    | GET    |

## Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                 |
|----------------|-----------|-----------|--------|---------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps   |
| relationType   | path      | No (solo para Get) | string | Nombre del tipo de relación a consultar    |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview'       |

## Ejemplo de consumo con cURL (List)

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/_apis/wit/workitemrelationtypes?api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

## Ejemplo de consumo con cURL (Get)

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/_apis/wit/workitemrelationtypes/{relationType}?api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

## Ejemplo de respuesta (List)

```json
{
  "count": 2,
  "value": [
    {
      "referenceName": "System.LinkTypes.Hierarchy-Forward",
      "name": "Parent",
      "attributes": {
        "usage": "workItemLink",
        "topology": "tree",
        "direction": "forward"
      }
    },
    {
      "referenceName": "System.LinkTypes.Hierarchy-Reverse",
      "name": "Child",
      "attributes": {
        "usage": "workItemLink",
        "topology": "tree",
        "direction": "reverse"
      }
    }
  ]
}
```

## Filtros y propiedades a incluir

  - `value`: array de tipos de relación, cada uno con:
    - `referenceName`: nombre de referencia (string)
    - `name`: nombre visible (string)
    - `attributes`: objeto con detalles de uso, topología y dirección
