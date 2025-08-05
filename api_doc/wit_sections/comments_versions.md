# Comments Versions

Permite consultar las diferentes versiones de los comentarios asociados a un work item en Azure DevOps. Cada vez que un comentario es editado, se crea una nueva versión que puede ser consultada para auditoría o historial.

**Documentación oficial:** [Comments Versions](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/comments?view=azure-devops-rest-7.2)

## Operaciones principales

| Operación | Endpoint                                                                                                 | Método |
|-----------|----------------------------------------------------------------------------------------------------------|--------|
| List      | /{organization}/{project}/_apis/wit/workItems/{workItemId}/comments/{commentId}/versions?api-version=7.2-preview.3 | GET    |
| Get       | /{organization}/{project}/_apis/wit/workItems/{workItemId}/comments/{commentId}/versions/{version}?api-version=7.2-preview.3 | GET    |

## Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                 |
|----------------|-----------|-----------|--------|---------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps   |
| project        | path      | Sí        | string | Nombre o ID del proyecto                    |
| workItemId     | path      | Sí        | int    | ID del work item                            |
| commentId      | path      | Sí        | int    | ID del comentario                           |
| version        | path      | No        | int    | Número de versión del comentario            |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.3'     |

## Ejemplo de consumo con cURL

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/workItems/123/comments/456/versions?api-version=7.2-preview.3' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

## Ejemplo de respuesta

```json
{
  "count": 2,
  "value": [
    {
      "version": 1,
      "text": "Comentario original",
      "createdDate": "2025-08-01T12:00:00Z"
    },
    {
      "version": 2,
      "text": "Comentario editado",
      "createdDate": "2025-08-01T12:05:00Z"
    }
  ]
}
```

## Filtros y propiedades a incluir

  - `value`: array de versiones de comentario, cada una con:
    - `version`: número de versión (int)
    - `text`: texto del comentario (string)
    - `createdDate`: fecha/hora de creación (string, date-time)

## Notas

- Permite auditar cambios en los comentarios de work items.
- El scope mínimo requerido es `vso.work` (lectura de work items y comentarios).

## Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                 |
|----------------|-----------|-----------|--------|---------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps   |
| project        | path      | Sí        | string | Nombre o ID del proyecto                    |
| workItemId     | path      | Sí        | int    | ID del work item                            |
| commentId      | path      | Sí        | int    | ID del comentario                           |
| version        | path      | No        | int    | Número de versión del comentario            |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.3'     |

---

## Ejemplo de consumo con cURL

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/workItems/123/comments/456/versions?api-version=7.2-preview.3' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

---

## Ejemplo de respuesta

```json
{
  "count": 2,
  "value": [
    {
      "version": 1,
      "text": "Comentario original",
      "createdDate": "2025-08-01T12:00:00Z"
    },
    {
      "version": 2,
      "text": "Comentario editado",
      "createdDate": "2025-08-01T12:05:00Z"
    }
  ]
}
```

---

## Notas

- Permite auditar cambios en los comentarios de work items.
- El scope mínimo requerido es `vso.work` (lectura de work items y comentarios).
