# Comments Reactions

Permite agregar, eliminar y listar reacciones (emojis) sobre comentarios de work items. Es útil para interacción social y feedback rápido en discusiones.

**Documentación oficial:** [Comments Reactions](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/comments-reactions?view=azure-devops-rest-7.2)

## Operaciones principales

| Operación | Endpoint                                                                                                                               | Método |
|-----------|----------------------------------------------------------------------------------------------------------------------------------------|--------|
| Create    | /{organization}/{project}/_apis/wit/workItems/{workItemId}/comments/{commentId}/reactions/{reactionType}?api-version=7.2-preview.1     | PUT    |
| Delete    | /{organization}/{project}/_apis/wit/workItems/{workItemId}/comments/{commentId}/reactions/{reactionType}?api-version=7.2-preview.1     | DELETE |
| List      | /{organization}/{project}/_apis/wit/workItems/{workItemId}/comments/{commentId}/reactions?api-version=7.2-preview.1                    | GET    |

### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo                  | Descripción                                 |
|----------------|-----------|-----------|-----------------------|---------------------------------------------|
| organization   | path      | Sí        | string                | Nombre de la organización de Azure DevOps   |
| project        | path      | Sí        | string                | Nombre o ID del proyecto                    |
| workItemId     | path      | Sí        | int                   | ID del work item                            |
| commentId      | path      | Sí        | int                   | ID del comentario                           |
| reactionType   | path      | Sí (Create, Delete) | enum (ver abajo) | Tipo de reacción (para Create y Delete)     |
| api-version    | query     | Sí        | string                | Versión de la API. Usar '7.2-preview.1'     |

#### Valores posibles para `reactionType` (enum)

- `like`
- `dislike`
- `heart`
- `hooray`
- `smile`
- `confused`

### Ejemplo de consumo con cURL

**Agregar una reacción:**
```bash
curl -X PUT \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/workItems/{workItemId}/comments/{commentId}/reactions/like?api-version=7.2-preview.1' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

**Eliminar una reacción:**
```bash
curl -X DELETE \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/workItems/{workItemId}/comments/{commentId}/reactions/like?api-version=7.2-preview.1' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

**Listar reacciones de un comentario:**
```bash
curl -X GET \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/workItems/{workItemId}/comments/{commentId}/reactions?api-version=7.2-preview.1' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

### Ejemplo de respuesta (List)

```json
[
  {
    "commentId": 50,
    "type": "like",
    "count": 2,
    "isCurrentUserEngaged": true,
    "url": "https://dev.azure.com/org/proj/_apis/wit/workItems/299/comments/50/reactions/like"
  },
  {
    "commentId": 50,
    "type": "heart",
    "count": 1,
    "isCurrentUserEngaged": false,
    "url": "https://dev.azure.com/org/proj/_apis/wit/workItems/299/comments/50/reactions/heart"
  }
]
```

### Filtros y propiedades a incluir

- No hay filtros adicionales ni parámetros tipo enum fuera de `reactionType`.
- Cada objeto de reacción incluye:
  - `commentId`: ID del comentario
  - `type`: tipo de reacción (ver enum)
  - `count`: cantidad de reacciones de ese tipo
  - `isCurrentUserEngaged`: si el usuario actual reaccionó
  - `url`: URL del recurso de la reacción
