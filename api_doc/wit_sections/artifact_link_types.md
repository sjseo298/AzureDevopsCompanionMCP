# Artifact Link Types

Permite obtener la lista de tipos de enlaces de artefactos salientes utilizados en el seguimiento de work items. Es útil para conocer los tipos de vínculos que se pueden establecer desde un work item hacia otros artefactos externos o internos.

**Documentación oficial:** [Artifact Link Types](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/artifact-link-types?view=azure-devops-rest-7.2)

## Operaciones principales

| Operación | Endpoint | Método |
|-----------|----------|--------|
| List      | /_apis/wit/artifactlinktypes?api-version=7.2-preview | GET    |

### Parámetros de URI

| Nombre       | Ubicación | Requerido | Tipo   | Descripción                               |
|--------------|-----------|-----------|--------|-------------------------------------------|
| organization | path      | Sí        | string | Nombre de la organización de Azure DevOps |
| api-version  | query     | Sí        | string | Versión de la API. Usar '7.2-preview'     |

### Ejemplo de consumo con cURL

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/_apis/wit/artifactlinktypes?api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

### Ejemplo de respuesta

```json
[
  {
    "artifactType": "Build",
    "name": "Build",
    "linkType": "ArtifactLink"
  },
  {
    "artifactType": "PullRequest",
    "name": "Pull Request",
    "linkType": "ArtifactLink"
  }
]
```
