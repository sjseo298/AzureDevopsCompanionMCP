# Artifact URI Query

Permite consultar los work items vinculados a una lista dada de URIs de artefactos. Es útil para identificar relaciones entre work items y artefactos externos (por ejemplo, builds, repositorios, etc.).

## Operaciones principales

| Operación | Endpoint | Método |
|-----------|----------|--------|
| Query     | /_apis/wit/artifacturiquery?api-version=7.2-preview | POST   |

### Parámetros de URI

| Nombre       | Ubicación | Requerido | Tipo   | Descripción                               |
|--------------|-----------|-----------|--------|-------------------------------------------|
| organization | path      | Sí        | string | Nombre de la organización de Azure DevOps |
| api-version  | query     | Sí        | string | Versión de la API. Usar '7.2-preview'     |

### Cuerpo de la petición (Request Body)

| Campo         | Tipo     | Requerido | Descripción                                 |
|---------------|----------|-----------|---------------------------------------------|
| uris          | array    | Sí        | Lista de URIs de artefactos a consultar     |

Ejemplo de body:

```json
{
  "uris": [
    "vstfs:///Build/Build/12345",
    "vstfs:///Git/PullRequestId/67890"
  ]
}
```

### Ejemplo de consumo con cURL

```bash
curl -X POST \
  'https://dev.azure.com/{organization}/_apis/wit/artifacturiquery?api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>' \
  -H 'Content-Type: application/json' \
  -d '{
    "uris": [
      "vstfs:///Build/Build/12345",
      "vstfs:///Git/PullRequestId/67890"
    ]
  }'
```

### Ejemplo de respuesta

```json
[
  {
    "artifactUri": "vstfs:///Build/Build/12345",
    "workItemIds": [101, 102]
  },
  {
    "artifactUri": "vstfs:///Git/PullRequestId/67890",
    "workItemIds": [103]
  }
]
```
