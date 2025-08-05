# Account My Work Recent Activity

Permite obtener la lista de actividades recientes de work items asociadas al usuario autenticado. Es útil para mostrar en dashboards personales o para seguimiento de trabajo reciente.

**Documentación oficial:** [Account My Work Recent Activity](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/account-my-work-recent-activity?view=azure-devops-rest-7.2)

## Operaciones principales

| Operación | Endpoint | Método |
|-----------|----------|--------|
| List      | /_apis/wit/accountmyworkrecentactivity?api-version=7.2-preview | GET    |

### Parámetros de URI

| Nombre       | Ubicación | Requerido | Tipo   | Descripción                               |
|--------------|-----------|-----------|--------|-------------------------------------------|
| organization | path      | Sí        | string | Nombre de la organización de Azure DevOps |
| api-version  | query     | Sí        | string | Versión de la API. Usar '7.2-preview'     |

### Ejemplo de consumo con cURL

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/_apis/wit/accountmyworkrecentactivity?api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

### Ejemplo de respuesta

```json
{
  "workItemId": 123,
  "activityDate": "2025-08-01T12:34:56.789Z",
  "activityType": "Updated",
  "title": "Fix login bug"
}
```
