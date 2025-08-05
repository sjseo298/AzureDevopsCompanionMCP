# Comment Reactions Engaged Users

Permite obtener la lista de usuarios que han reaccionado a un comentario en un work item. Es útil para analizar la interacción y participación en discusiones de trabajo.

**Documentación oficial:** [Comment Reactions Engaged Users](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/comment-reactions-engaged-users?view=azure-devops-rest-7.2)

## Operaciones principales

| Operación | Endpoint                                                                                                                        | Método |
|-----------|----------------------------------------------------------------------------------------------------------------------------------|--------|
| List      | /{organization}/{project}/_apis/wit/workItems/{workItemId}/comments/{commentId}/reactions/users?api-version=7.2-preview         | GET    |

### Parámetros de URI

| Nombre        | Ubicación | Requerido | Tipo   | Descripción                                 |
|---------------|-----------|-----------|--------|---------------------------------------------|
| organization  | path      | Sí        | string | Nombre de la organización de Azure DevOps   |
| project       | path      | Sí        | string | Nombre o ID del proyecto                    |
| workItemId    | path      | Sí        | int    | ID del work item                            |
| commentId     | path      | Sí        | int    | ID del comentario                           |
| api-version   | query     | Sí        | string | Versión de la API. Usar '7.2-preview'       |

### Ejemplo de consumo con cURL

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/workItems/{workItemId}/comments/{commentId}/reactions/users?api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

### Ejemplo de respuesta

```json
{
  "count": 2,
  "value": [
    {
      "id": "1111-2222-3333-4444",
      "displayName": "Juan Pérez",
      "uniqueName": "juan.perez@fabrikam.com"
    },
    {
      "id": "5555-6666-7777-8888",
      "displayName": "Ana Gómez",
      "uniqueName": "ana.gomez@fabrikam.com"
    }
  ]
}
```

### Filtros y propiedades a incluir

- No hay filtros adicionales ni parámetros tipo enum para esta operación.
- El resultado incluye la lista de usuarios que reaccionaron, con sus IDs y nombres.
