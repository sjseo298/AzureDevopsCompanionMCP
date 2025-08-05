# Work Item Revisions Discussions

Permite consultar las discusiones asociadas a las revisiones de los work items en Azure DevOps. Útil para auditar o sincronizar los cambios y comentarios realizados en cada revisión de un work item.

**Documentación oficial:** [Work Item Revisions Discussions](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/work-item-revisions-discussions?view=azure-devops-rest-7.2)

## Operaciones principales

| Operación                 | Endpoint                                                                                                 | Método |
|---------------------------|--------------------------------------------------------------------------------------------------------|--------|
| Read Reporting Discussions| /{organization}/{project}/_apis/wit/reporting/workitemrevisionsdiscussions?api-version=7.2-preview       | GET    |

## Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                 |
|----------------|-----------|-----------|--------|---------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps   |
| project        | path      | No        | string | Nombre o ID del proyecto (opcional)         |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview'       |

## Ejemplo de consumo con cURL

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/reporting/workitemrevisionsdiscussions?api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

## Ejemplo de respuesta

```json
{
  "count": 1,
  "value": [
    {
      "workItemId": 123,
      "revision": 5,
      "discussion": {
        "comments": [
          {
            "id": 1,
            "text": "Comentario en la revisión",
            "createdDate": "2025-08-01T12:00:00Z"
          }
        ]
      }
    }
  ]
}
```

## Filtros y propiedades a incluir

  - `value`: array de discusiones, cada una con:
    - `workItemId`: ID del work item (int)
    - `revision`: número de revisión (int)
    - `discussion`: objeto con los comentarios asociados

## Notas

- Permite auditar los comentarios y discusiones de cada revisión de work items.
- El scope mínimo requerido es `vso.work` (lectura de work items y comentarios).
