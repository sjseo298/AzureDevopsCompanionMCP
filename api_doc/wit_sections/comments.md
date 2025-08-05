# Comments

Permite agregar, obtener, actualizar y eliminar comentarios en work items. Soporta paginación, batch y operaciones sobre versiones y reacciones (ver subsecciones específicas). Es fundamental para la colaboración y trazabilidad en el trabajo.

**Documentación oficial:** [Comments](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/comments?view=azure-devops-rest-7.2)

## Operaciones principales

| Operación         | Endpoint                                                                                                              | Método |
|-------------------|-----------------------------------------------------------------------------------------------------------------------|--------|
| Add Comment       | /{organization}/{project}/_apis/wit/workItems/{workItemId}/comments?api-version=7.0-preview.3                         | POST   |
| Delete            | /{organization}/{project}/_apis/wit/workItems/{workItemId}/comments/{commentId}?api-version=7.2-preview.4             | DELETE |
| Update            | /{organization}/{project}/_apis/wit/workItems/{workItemId}/comments/{commentId}?api-version=7.0-preview.3             | PATCH  |

### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo    | Descripción                                 |
|----------------|-----------|-----------|---------|---------------------------------------------|
| organization   | path      | Sí        | string  | Nombre de la organización de Azure DevOps   |
| project        | path      | Sí        | string  | Nombre o ID del proyecto                    |
| workItemId     | path      | Sí        | int     | ID del work item                            |
| commentId      | path      | Sí (Delete, Update) | int | ID del comentario (para Delete y Update)    |
| api-version    | query     | Sí        | string  | Versión de la API (ver tabla de operaciones)|

### Cuerpo de la petición (Request Body)

- **Add Comment:**
  - `text` (string, requerido): Texto del comentario a agregar.
  - Ejemplo:
    ```json
    {
      "text": "Moving to the right area path"
    }
    ```
- **Update:**
  - `text` (string, requerido): Texto actualizado del comentario.
  - Ejemplo:
    ```json
    {
      "text": "Moving to the right area path - Fabrikam-Git"
    }
    ```

### Ejemplo de consumo con cURL

**Agregar un comentario:**
```bash
curl -X POST \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/workItems/{workItemId}/comments?api-version=7.0-preview.3' \
  -H 'Authorization: Basic <BASE64_PAT>' \
  -H 'Content-Type: application/json' \
  -d '{"text": "Moving to the right area path"}'
```

**Actualizar un comentario:**
```bash
curl -X PATCH \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/workItems/{workItemId}/comments/{commentId}?api-version=7.0-preview.3' \
  -H 'Authorization: Basic <BASE64_PAT>' \
  -H 'Content-Type: application/json' \
  -d '{"text": "Moving to the right area path - Fabrikam-Git"}'
```

**Eliminar un comentario:**
```bash
curl -X DELETE \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/workItems/{workItemId}/comments/{commentId}?api-version=7.2-preview.4' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

### Ejemplo de respuesta (Add/Update/Delete)

```json
{
  "workItemId": 299,
  "commentId": 50,
  "version": 2,
  "text": "Moving to the right area path - Fabrikam-Git",
  "createdBy": {
    "displayName": "Jamal Hartnett",
    "id": "d291b0c4-a05c-4ea6-8df1-4b41d5f39eff"
  },
  "createdDate": "2019-01-16T03:03:28.97Z",
  "modifiedBy": {
    "displayName": "Jamal Hartnett",
    "id": "d291b0c4-a05c-4ea6-8df1-4b41d5f39eff"
  },
  "modifiedDate": "2019-01-22T02:32:10.67Z",
  "isDeleted": false,
  "url": "https://dev.azure.com/fabrikam/6ce954b1-ce1f-45d1-b94d-e6bf2464ba2c/_apis/wit/workItems/299/comments/50"
}
```

### Filtros y propiedades a incluir

  - `workItemId`, `commentId`, `version`, `text`, `createdBy`, `createdDate`, `modifiedBy`, `modifiedDate`, `isDeleted`, `url`, `mentions`, `reactions`.
  - `type` (enum):
    - `like`, `dislike`, `heart`, `hooray`, `smile`, `confused`
  - `count` (int): cantidad de reacciones de ese tipo
  - `isCurrentUserEngaged` (bool): si el usuario actual reaccionó
