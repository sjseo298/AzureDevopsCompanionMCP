# Build API (Azure DevOps REST API v7.2)
# Getting Started: Basic Commands

Before using the Build API, ensure you have:
- An Azure DevOps organization and project
- A Personal Access Token (PAT) with appropriate permissions
- The `curl` command-line tool (or similar HTTP client)

All API requests must include:
- The `Authorization` header with your PAT (use `Basic` auth, username can be empty)
- The `Content-Type: application/json` header for POST/PATCH requests

**Example: List builds**

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/{project}/_apis/build/builds?api-version=7.2-preview.7' \
  -H 'Authorization: Basic <PAT>'
```

**Example: Queue a build**

```bash
curl -X POST \
  'https://dev.azure.com/{organization}/{project}/_apis/build/builds?api-version=7.2-preview.7' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Basic <PAT>' \
  -d '{ "definition": { "id": <definitionId> } }'
```

Replace `{organization}`, `{project}`, `<PAT>`, and `<definitionId>` with your actual values.


## Overview

The Build API in Azure DevOps provides endpoints to manage and automate build pipelines, definitions, and build results. It enables you to queue builds, retrieve build information, manage build tags, and interact with build definitions and templates.

**Main functionalities:**
- Run and queue builds
- Retrieve build details and results
- Manage build definitions and templates
- Tag builds and search builds by tags

## Main Operations

## Main Resource Groups (Subsections)

### Documentation Progress


| Subsection            | Documentation URL | Progress |
|-----------------------|-------------------|----------|
| Artifacts             | [Artifacts](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/artifacts?view=azure-devops-rest-7.2) | ✅ |
| Attachments           | [Attachments](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/attachments?view=azure-devops-rest-7.2) | ✅ |
| Builds                | [Builds](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/builds?view=azure-devops-rest-7.2) | ✅ |
| Controllers           | [Controllers](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/controllers?view=azure-devops-rest-7.2) | ✅ |
| Definitions           | [Definitions](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/definitions?view=azure-devops-rest-7.2) | ✅ |
| Definition Templates  | [Definition Templates](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/templates?view=azure-devops-rest-7.2) | ✅ |
| Deployments           | [Deployments](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/deployments?view=azure-devops-rest-7.2) | ✅ |
| Folders               | [Folders](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/folders?view=azure-devops-rest-7.2) | ✅ |
| Logs                  | [Logs](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/logs?view=azure-devops-rest-7.2) | ✅ |
| Metrics               | [Metrics](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/metrics?view=azure-devops-rest-7.2) | ✅ |
| Phases                | [Phases](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/phases?view=azure-devops-rest-7.2) | ✅ |
| Retention             | [Retention](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/retention?view=azure-devops-rest-7.2) | ✅ |
| Tags                  | [Tags](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/tags?view=azure-devops-rest-7.2) | ✅ |
| Work Items            | [Work Items](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/workitems?view=azure-devops-rest-7.2) | ✅ |


- **Create**: Asocia un artefacto a un build.
  - `POST /_apis/build/builds/{buildId}/artifacts?artifactName={artifactName}`
  - `GET /_apis/build/builds/{buildId}/artifacts?artifactName={artifactName}`
- **Get File**: Descarga un archivo específico de un artefacto de build.
- **List**: Lista todos los artefactos asociados a un build.
  - `GET /_apis/build/builds/{buildId}/artifacts`

**Ejemplo de uso:**

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/{project}/_apis/build/builds/{buildId}/artifacts?api-version=7.2-preview.7' \
  -H 'Authorization: Basic <PAT>'
```

**Más información:** [Artifacts API Reference](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/artifacts?view=azure-devops-rest-7.2)


## Attachments


La subsección **Attachments** de la Build API permite trabajar con archivos adjuntos asociados a un build en Azure DevOps. Los attachments pueden ser archivos de logs, reportes u otros artefactos generados durante el proceso de build y asociados a un build específico.

**Operaciones principales:**

- **Get**: Obtiene un archivo adjunto específico de un build.
  - **Endpoint:** `GET /_apis/build/builds/{buildId}/attachments/{type}/{name}`
  - **Parámetros:**
    - `buildId` (string, requerido): ID del build.
    - `type` (string, requerido): Tipo de attachment (por ejemplo, "log").
    - `name` (string, requerido): Nombre del archivo adjunto.
    - `api-version` (string, requerido): Versión de la API (ejemplo: `7.2-preview.1`).
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/builds/{buildId}/attachments/{type}/{name}?api-version=7.2-preview.1' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response (archivo):**
    El response será el archivo binario solicitado (por ejemplo, un log o reporte). Si se solicita como JSON, se puede recibir un objeto con metadatos:
    ```json
    {
      "id": "{attachmentId}",
      "name": "{name}",
      "type": "{type}",
      "url": "https://dev.azure.com/..."
    }
    ```

- **List**: Lista los archivos adjuntos de un tipo específico asociados a un build.
  - **Endpoint:** `GET /_apis/build/builds/{buildId}/attachments/{type}`
  - **Parámetros:**
    - `buildId` (string, requerido): ID del build.
    - `type` (string, requerido): Tipo de attachment.
    - `api-version` (string, requerido): Versión de la API (ejemplo: `7.2-preview.1`).
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/builds/{buildId}/attachments/{type}?api-version=7.2-preview.1' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "count": 2,
      "value": [
        {
          "id": "{attachmentId1}",
          "name": "log1.txt",
          "type": "log",
          "url": "https://dev.azure.com/..."
        },
        {
          "id": "{attachmentId2}",
          "name": "log2.txt",
          "type": "log",
          "url": "https://dev.azure.com/..."
        }
      ]
    }
    ```

**Más información:** [Attachments API Reference](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/attachments?view=azure-devops-rest-7.2)

---


## Builds


La subsección **Builds** de la Build API permite gestionar y consultar los builds ejecutados en Azure DevOps. Incluye operaciones para crear (queue), listar, obtener detalles, actualizar y eliminar builds, así como consultar cambios, logs, work items y retención asociados a builds.

**Operaciones principales:**


- **Queue**: Cola un nuevo build.
  - **Endpoint:** `POST /_apis/build/builds`
  - **Parámetros (body):**
    - `definition.id` (int, requerido): ID de la definición de build.
    - `parameters` (string, opcional): Parámetros personalizados para el build. Debe ser un string en formato JSON que representa un objeto clave-valor con los parámetros definidos en la definición de build. Permite sobreescribir valores predeterminados definidos en la pipeline.
      - **Tipo:** string (JSON serializado)
      - **Ejemplo de valor:** `"{\"param1\":\"valor1\",\"param2\":\"valor2\"}"`
      - **Ejemplo de uso:**
        ```bash
        curl -X POST \
          'https://dev.azure.com/{organization}/{project}/_apis/build/builds?api-version=7.2-preview.7' \
          -H 'Content-Type: application/json' \
          -H 'Authorization: Basic <PAT>' \
          -d '{ "definition": { "id": 123 }, "parameters": "{\"param1\":\"valor1\"}" }'
        ```
      - **Notas:** Los nombres de los parámetros deben coincidir con los definidos en la definición de build YAML o clásica.
    - `priority` (string, opcional): Prioridad del build. Valores posibles: `"high"`, `"aboveNormal"`, `"normal"`, `"belowNormal"`, `"low"`.
    - `reason` (string, opcional): Motivo del build. Valores posibles: `"manual"`, `"individualCI"`, `"batchedCI"`, `"schedule"`, `"validateShelveset"`, `"checkInShelveset"`, `"pullRequest"`, `"buildCompletion"`, `"triggered"`, `"all"`.
    - `sourceBranch` (string, opcional): Rama de origen para el build (ejemplo: `refs/heads/main`).
    - `sourceVersion` (string, opcional): Commit SHA o versión a construir.
    - `demands` (array, opcional): Lista de demandas para el agente. Permite especificar condiciones que debe cumplir el agente de build (por ejemplo, capacidades de software o hardware).
      - **Tipo:** array de strings
      - **Ejemplo de valor:** `["Agent.OS -equals Linux", "npm -exists"]`
      - **Ejemplo de uso:**
        ```json
        "demands": ["Agent.OS -equals Linux", "npm -exists"]
        ```
      - **Notas:** Si alguna demanda no se cumple, el build no será asignado a ese agente.
    - `api-version` (string, requerido): Versión de la API (ejemplo: `7.2-preview.7`).
  - **Ejemplo de request:**
    ```bash
    curl -X POST \
      'https://dev.azure.com/{organization}/{project}/_apis/build/builds?api-version=7.2-preview.7' \
      -H 'Content-Type: application/json' \
      -H 'Authorization: Basic <PAT>' \
      -d '{ "definition": { "id": 123 }, "priority": "high", "sourceBranch": "refs/heads/main" }'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "id": 456,
      "buildNumber": "20250804.1",
      "status": "inProgress",
      "definition": { "id": 123, "name": "MyPipeline" },
      ...
    }
    ```


- **List**: Obtiene una lista de builds.
  - **Endpoint:** `GET /_apis/build/builds`
  - **Parámetros (query):**
    - `definitions` (string, opcional): IDs de definiciones separados por coma para filtrar (ejemplo: `1,2,3`).
    - `statusFilter` (string, opcional): Estado de los builds. Valores posibles: `"all"`, `"cancelling"`, `"completed"`, `"inProgress"`, `"none"`, `"notStarted"`, `"postponed"`.
    - `resultFilter` (string, opcional): Resultado de los builds. Valores posibles: `"canceled"`, `"failed"`, `"none"`, `"partiallySucceeded"`, `"succeeded"`.
    - `queueIds` (string, opcional): IDs de colas separados por coma.
    - `requestedFor` (string, opcional): Usuario que solicitó el build.
    - `reasonFilter` (string, opcional): Motivo del build (ver valores en Queue).
    - `minTime`/`maxTime` (string, opcional): Rango de fechas para filtrar builds por fecha de creación. Deben estar en formato ISO 8601 (`YYYY-MM-DDTHH:MM:SSZ`).
      - **Tipo:** string (ISO 8601)
      - **Ejemplo de valor:** `minTime=2025-08-01T00:00:00Z&maxTime=2025-08-04T23:59:59Z`
      - **Notas:** Permite obtener builds creados dentro de un rango específico de fechas y horas.
    - `api-version` (string, requerido): Versión de la API.
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/builds?statusFilter=completed&resultFilter=succeeded&api-version=7.2-preview.7' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "count": 1,
      "value": [
        {
          "id": 456,
          "buildNumber": "20250804.1",
          "status": "completed",
          "result": "succeeded",
          ...
        }
      ]
    }
    ```


- **Get**: Obtiene los detalles de un build específico.
  - **Endpoint:** `GET /_apis/build/builds/{buildId}`
  - **Parámetros:**
    - `buildId` (int, requerido): ID del build.
    - `propertyFilters` (string, opcional): Filtros de propiedades adicionales a incluir en la respuesta. Permite especificar una lista separada por comas de nombres de propiedades que se desean obtener en el objeto build, además de las propiedades estándar. Si se omite, solo se devuelven los campos básicos del build.
      - **Tipo:** string (separado por comas)
      - **Valores posibles y significado:**
        - `properties`: Incluye el objeto de propiedades personalizadas asociadas al build.
        - `parameters`: Incluye los parámetros utilizados para ejecutar el build (útil para builds parametrizados).
        - `tags`: Incluye el arreglo de etiquetas asociadas al build.
        - `triggerInfo`: Incluye información detallada sobre el disparador que originó el build (por ejemplo, variables de pipeline, tipo de trigger, etc.).
        - `requestedFor`: Incluye información extendida del usuario que solicitó el build.
        - `lastChangedBy`: Incluye información sobre el usuario que realizó el último cambio en el build.
        - `validationResults`: Incluye los resultados de validaciones asociadas al build.
        - `plans`: Incluye información sobre los planes de ejecución del build.
        - `repository`: Incluye detalles extendidos del repositorio fuente.
        - `queue`: Incluye detalles extendidos de la cola de agentes utilizada.
        - `orchestrationPlan`: Incluye información sobre el plan de orquestación del build.
        - `logs`: Incluye información extendida de los logs asociados al build.
        - `retainedByRelease`: Indica si el build está retenido por una release.
        - `all`: Incluye todas las propiedades adicionales disponibles (puede aumentar el tamaño de la respuesta).
        - Puedes combinar varios valores separados por coma, por ejemplo: `properties,parameters,tags`.
      - **Ejemplo de uso:**
        ```bash
        curl -X GET \
          'https://dev.azure.com/{organization}/{project}/_apis/build/builds/456?propertyFilters=properties,parameters,tags,triggerInfo&api-version=7.2-preview.7' \
          -H 'Authorization: Basic <PAT>'
        ```
      - **Ejemplo de valor en JSON:**
        ```json
        {
          "id": 456,
          "buildNumber": "20250804.1",
          "properties": { "customKey": "value" },
          "parameters": "{\"param1\":\"value1\"}",
          "tags": ["release", "hotfix"],
          "triggerInfo": { "ci.sourceBranch": "refs/heads/main" },
          ...
        }
        ```
    - `api-version` (string, requerido): Versión de la API.
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/builds/456?api-version=7.2-preview.7' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "id": 456,
      "buildNumber": "20250804.1",
      "status": "completed",
      "result": "succeeded",
      ...
    }
    ```

- **Update Build**: Actualiza un build existente.
  - **Endpoint:** `PATCH /_apis/build/builds/{buildId}`
  - **Parámetros:**
    - `buildId` (int, requerido): ID del build.
    - `api-version` (string, requerido): Versión de la API.
    - Body con los campos a actualizar. El cuerpo debe ser un objeto JSON con los campos que se desean modificar en el build. Por ejemplo, se puede actualizar el estado, las etiquetas, o propiedades personalizadas.
      - **Tipo:** objeto JSON
      - **Ejemplo de uso:**
        ```bash
        curl -X PATCH \
          'https://dev.azure.com/{organization}/{project}/_apis/build/builds/456?api-version=7.2-preview.7' \
          -H 'Content-Type: application/json' \
          -H 'Authorization: Basic <PAT>' \
          -d '{ "status": "cancelling", "tags": ["urgent"] }'
        ```
      - **Notas:** Solo los campos permitidos por la API pueden ser modificados. Consultar la documentación oficial para la lista completa de campos actualizables.
  - **Ejemplo de request:**
    ```bash
    curl -X PATCH \
      'https://dev.azure.com/{organization}/{project}/_apis/build/builds/456?api-version=7.2-preview.7' \
      -H 'Content-Type: application/json' \
      -H 'Authorization: Basic <PAT>' \
      -d '{ "status": "cancelling" }'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "id": 456,
      "status": "cancelling",
      ...
    }
    ```

- **Delete**: Elimina un build.
  - **Endpoint:** `DELETE /_apis/build/builds/{buildId}`
  - **Parámetros:**
    - `buildId` (int, requerido): ID del build.
    - `api-version` (string, requerido): Versión de la API.
    - `type` (string, opcional): Tipo de work item a retornar (por ejemplo, `Bug`, `Task`). Permite filtrar los work items asociados al build por tipo.
      - **Tipo:** string
      - **Ejemplo de uso:**
        ```bash
        curl -X GET \
          'https://dev.azure.com/{organization}/{project}/_apis/build/builds/456/workitems?type=Bug&api-version=7.2-preview.7' \
          -H 'Authorization: Basic <PAT>'
        ```
      - **Notas:** Si se omite, se retornan todos los tipos de work items asociados.
  - **Ejemplo de request:**
    ```bash
    curl -X DELETE \
      'https://dev.azure.com/{organization}/{project}/_apis/build/builds/456?api-version=7.2-preview.7' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "id": 456,
      "status": "deleted"
    }
    ```

- **Get Build Changes**: Obtiene los cambios asociados a un build.
  - **Endpoint:** `GET /_apis/build/builds/{buildId}/changes`
  - **Parámetros:**
    - `buildId` (int, requerido): ID del build.
    - `api-version` (string, requerido): Versión de la API.
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/builds/456/changes?api-version=7.2-preview.7' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "count": 1,
      "value": [
        {
          "id": "c1a2b3c",
          "message": "Commit message",
          ...
        }
      ]
    }
    ```

- **Get Build Logs**: Obtiene los logs de un build.
  - **Endpoint:** `GET /_apis/build/builds/{buildId}/logs`
  - **Parámetros:**
    - `buildId` (int, requerido): ID del build.
    - `api-version` (string, requerido): Versión de la API.
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/builds/456/logs?api-version=7.2-preview.7' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "count": 2,
      "value": [
        { "id": 1, "type": "build", "url": "https://dev.azure.com/..." },
        { "id": 2, "type": "test", "url": "https://dev.azure.com/..." }
      ]
    }
    ```

- **Get Build Work Items Refs**: Obtiene los work items asociados a un build.
  - **Endpoint:** `GET /_apis/build/builds/{buildId}/workitems`
  - **Parámetros:**
    - `buildId` (int, requerido): ID del build.
    - `api-version` (string, requerido): Versión de la API.
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/builds/456/workitems?api-version=7.2-preview.7' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "count": 1,
      "value": [
        { "id": 789, "url": "https://dev.azure.com/..." }
      ]
    }
    ```

- **Get Changes Between Builds**: Cambios entre dos builds.
  - **Endpoint:** `GET /_apis/build/changes?fromBuildId={fromBuildId}&toBuildId={toBuildId}`
  - **Parámetros:**
    - `fromBuildId` (int, requerido): ID del build inicial.
    - `toBuildId` (int, requerido): ID del build final.
    - `api-version` (string, requerido): Versión de la API.
    - `type` (string, opcional): Tipo de work item a retornar entre builds (por ejemplo, `Bug`, `Task`).
      - **Tipo:** string
      - **Ejemplo de uso:**
        ```bash
        curl -X GET \
          'https://dev.azure.com/{organization}/{project}/_apis/build/workitems?fromBuildId=123&toBuildId=456&type=Task&api-version=7.2-preview.7' \
          -H 'Authorization: Basic <PAT>'
        ```
      - **Notas:** Si se omite, se retornan todos los tipos de work items encontrados entre los builds indicados.
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/changes?fromBuildId=123&toBuildId=456&api-version=7.2-preview.7' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "count": 2,
      "value": [
        { "id": "c1", "message": "Commit 1" },
        { "id": "c2", "message": "Commit 2" }
      ]
    }
    ```

- **Get Work Items Between Builds**: Work items entre dos builds.
  - **Endpoint:** `GET /_apis/build/workitems?fromBuildId={fromBuildId}&toBuildId={toBuildId}`
  - **Parámetros:**
    - `fromBuildId` (int, requerido): ID del build inicial.
    - `toBuildId` (int, requerido): ID del build final.
    - `api-version` (string, requerido): Versión de la API.
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/workitems?fromBuildId=123&toBuildId=456&api-version=7.2-preview.7' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "count": 1,
      "value": [
        { "id": 789, "url": "https://dev.azure.com/..." }
      ]
    }
    ```


**Más información:** [Builds API Reference](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/builds?view=azure-devops-rest-7.2)

---




## Folders

La subsección **Folders** de la Build API permite organizar las definiciones de build en carpetas jerárquicas, facilitando la gestión y el acceso a pipelines en proyectos grandes.

**Operaciones principales:**

- **List**: Lista todas las carpetas de definiciones de build.
  - **Endpoint:** `GET /_apis/build/folders`
  - **Parámetros (query):**
    - `path` (string, opcional): Ruta de la carpeta raíz desde la cual listar (ejemplo: `/` o `/MyFolder`). Si se omite, lista desde la raíz.
    - `queryOrder` (string, opcional): Orden de los resultados. Valores: `none`, `folderAscending`, `folderDescending`.
    - `api-version` (string, requerido): Versión de la API (ejemplo: `7.2-preview.1`).
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/folders?path=/&api-version=7.2-preview.1' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "count": 2,
      "value": [
        { "path": "/", "createdBy": { "displayName": "admin" }, "createdOn": "2025-08-01T10:00:00Z" },
        { "path": "/MyFolder", "createdBy": { "displayName": "user@domain.com" }, "createdOn": "2025-08-02T12:00:00Z" }
      ]
    }
    ```

- **Get**: Obtiene los detalles de una carpeta específica.
  - **Endpoint:** `GET /_apis/build/folders/{*path}`
  - **Parámetros:**
    - `path` (string, requerido): Ruta completa de la carpeta (ejemplo: `/MyFolder`).
    - `api-version` (string, requerido): Versión de la API.
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/folders/MyFolder?api-version=7.2-preview.1' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "path": "/MyFolder",
      "createdBy": { "displayName": "user@domain.com" },
      "createdOn": "2025-08-02T12:00:00Z"
    }
    ```

- **Create/Update**: Crea o actualiza una carpeta de definiciones.
  - **Endpoint:** `PUT /_apis/build/folders/{*path}`
  - **Parámetros:**
    - `path` (string, requerido): Ruta completa de la carpeta a crear o actualizar.
    - `api-version` (string, requerido): Versión de la API.
    - Body: Objeto JSON con los metadatos de la carpeta (opcional, normalmente vacío o `{}`).
  - **Ejemplo de request:**
    ```bash
    curl -X PUT \
      'https://dev.azure.com/{organization}/{project}/_apis/build/folders/NewFolder?api-version=7.2-preview.1' \
      -H 'Content-Type: application/json' \
      -H 'Authorization: Basic <PAT>' \
      -d '{}'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "path": "/NewFolder",
      "createdBy": { "displayName": "user@domain.com" },
      "createdOn": "2025-08-04T15:00:00Z"
    }
    ```

- **Delete**: Elimina una carpeta de definiciones.
  - **Endpoint:** `DELETE /_apis/build/folders/{*path}`
  - **Parámetros:**
    - `path` (string, requerido): Ruta completa de la carpeta a eliminar.
    - `api-version` (string, requerido): Versión de la API.
  - **Ejemplo de request:**
    ```bash
    curl -X DELETE \
      'https://dev.azure.com/{organization}/{project}/_apis/build/folders/MyFolder?api-version=7.2-preview.1' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "path": "/MyFolder",
      "deleted": true
    }
    ```

**Notas:**
- Las carpetas permiten organizar y agrupar definiciones de build de manera jerárquica.
- Los nombres de las carpetas distinguen mayúsculas y minúsculas.

**Más información:** [Folders API Reference](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/folders?view=azure-devops-rest-7.2)

## Logs

La subsección **Logs** de la Build API permite acceder y gestionar los logs generados durante la ejecución de builds en Azure DevOps. Los logs son fundamentales para el diagnóstico, auditoría y monitoreo de los procesos de CI/CD.

**Operaciones principales:**

- **List**: Lista todos los logs asociados a un build.
  - **Endpoint:** `GET /_apis/build/builds/{buildId}/logs`
  - **Parámetros:**
    - `buildId` (int, requerido): ID del build.
    - `api-version` (string, requerido): Versión de la API (ejemplo: `7.2-preview.2`).
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/builds/456/logs?api-version=7.2-preview.2' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "count": 2,
      "value": [
        { "id": 1, "type": "build", "createdOn": "2025-08-04T12:00:00Z", "lastChangedOn": "2025-08-04T12:10:00Z", "lineCount": 120, "url": "https://dev.azure.com/.../logs/1" },
        { "id": 2, "type": "test", "createdOn": "2025-08-04T12:05:00Z", "lastChangedOn": "2025-08-04T12:15:00Z", "lineCount": 45, "url": "https://dev.azure.com/.../logs/2" }
      ]
    }
    ```

- **Get**: Obtiene los detalles de un log específico.
  - **Endpoint:** `GET /_apis/build/builds/{buildId}/logs/{logId}`
  - **Parámetros:**
    - `buildId` (int, requerido): ID del build.
    - `logId` (int, requerido): ID del log.
    - `api-version` (string, requerido): Versión de la API.
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/builds/456/logs/1?api-version=7.2-preview.2' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "id": 1,
      "type": "build",
      "createdOn": "2025-08-04T12:00:00Z",
      "lastChangedOn": "2025-08-04T12:10:00Z",
      "lineCount": 120,
      "url": "https://dev.azure.com/.../logs/1"
    }
    ```

- **Get Log Lines**: Obtiene líneas específicas de un log (útil para paginación o grandes volúmenes).
  - **Endpoint:** `GET /_apis/build/builds/{buildId}/logs/{logId}/lines`
  - **Parámetros:**
    - `buildId` (int, requerido): ID del build.
    - `logId` (int, requerido): ID del log.
    - `startLine` (int, opcional): Línea inicial a recuperar (por defecto 1).
    - `endLine` (int, opcional): Línea final a recuperar (por defecto última línea).
    - `api-version` (string, requerido): Versión de la API.
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/builds/456/logs/1/lines?startLine=1&endLine=20&api-version=7.2-preview.2' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```text
    Starting build...
    Restoring dependencies...
    ...
    Build succeeded.
    ```

**Notas:**
- Los logs pueden ser grandes; se recomienda usar la operación de líneas para paginar resultados.
- El campo `type` puede indicar el origen del log (por ejemplo, `build`, `test`, etc.).
- Los logs se pueden descargar como texto plano o consultar por fragmentos.
- Los logs son útiles para auditoría, troubleshooting y análisis de builds fallidos.

**Más información:** [Logs API Reference](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/logs?view=azure-devops-rest-7.2)

## Metrics

La subsección **Metrics** de la Build API permite consultar métricas agregadas sobre builds y pipelines, como duración promedio, éxito/fallo, cantidad de builds ejecutados, entre otros. Es útil para monitoreo, análisis de tendencias y optimización de pipelines.

**Operaciones principales:**

- **List**: Lista las métricas agregadas de builds para un proyecto o definición específica.
  - **Endpoint:** `GET /_apis/build/metrics`
  - **Parámetros (query):**
    - `definition` (int, opcional): ID de la definición de build para filtrar métricas de una pipeline específica.
    - `minMetricsTime` (string, opcional): Fecha mínima (ISO 8601) desde la cual calcular métricas (ejemplo: `2025-08-01T00:00:00Z`).
    - `api-version` (string, requerido): Versión de la API (ejemplo: `7.2-preview.2`).
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/metrics?definition=123&minMetricsTime=2025-08-01T00:00:00Z&api-version=7.2-preview.2' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "count": 1,
      "value": [
        {
          "definition": { "id": 123, "name": "MyPipeline" },
          "metrics": [
            { "name": "BuildsCompleted", "intValue": 10 },
            { "name": "BuildsSucceeded", "intValue": 8 },
            { "name": "BuildsFailed", "intValue": 2 },
            { "name": "AvgDuration", "duration": "00:05:30" }
          ]
        }
      ]
    }
    ```

**Notas:**
- Las métricas pueden incluir: cantidad de builds completados, exitosos, fallidos, duración promedio, etc.
- El campo `minMetricsTime` permite analizar tendencias en un rango de tiempo específico.
- El campo `metrics` es un arreglo de objetos con nombre y valor de la métrica.
- Útil para dashboards, reportes y análisis de eficiencia de pipelines.

**Más información:** [Metrics API Reference](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/metrics?view=azure-devops-rest-7.2)

## Phases

La subsección **Phases** de la Build API permite consultar información sobre las fases (phases) de ejecución de un build. Una fase representa una etapa lógica dentro de un pipeline, que puede contener uno o varios jobs y tareas.

**Operaciones principales:**

- **List**: Lista las fases de un build específico.
  - **Endpoint:** `GET /_apis/build/builds/{buildId}/phases`
  - **Parámetros:**
    - `buildId` (int, requerido): ID del build.
    - `api-version` (string, requerido): Versión de la API (ejemplo: `7.2-preview.1`).
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/builds/456/phases?api-version=7.2-preview.1' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "count": 1,
      "value": [
        {
          "phaseId": 1,
          "name": "Build",
          "status": "completed",
          "result": "succeeded",
          "jobs": [
            { "jobId": 101, "name": "Job1", "status": "completed", "result": "succeeded" }
          ]
        }
      ]
    }
    ```

**Notas:**
- Las fases permiten dividir el pipeline en etapas lógicas (por ejemplo, Build, Test, Deploy).
- Cada fase puede contener múltiples jobs y tareas.
- El campo `status` puede ser `pending`, `inProgress`, `completed`, etc.
- El campo `result` puede ser `succeeded`, `failed`, `canceled`, etc.
- Útil para monitoreo detallado y troubleshooting de pipelines complejos.

**Más información:** [Phases API Reference](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/phases?view=azure-devops-rest-7.2)

## Retention

La subsección **Retention** de la Build API permite consultar y administrar las políticas de retención de builds, es decir, cuánto tiempo se conservan los builds y sus artefactos antes de ser eliminados automáticamente. Es fundamental para controlar el almacenamiento y cumplimiento de políticas organizacionales.

**Operaciones principales:**

- **List**: Lista las reglas de retención configuradas para un proyecto o definición.
  - **Endpoint:** `GET /_apis/build/retention`
  - **Parámetros (query):**
    - `definitionId` (int, opcional): ID de la definición de build para filtrar reglas de retención específicas.
    - `api-version` (string, requerido): Versión de la API (ejemplo: `7.2-preview.1`).
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/retention?definitionId=123&api-version=7.2-preview.1' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "count": 1,
      "value": [
        {
          "id": 1,
          "definitionId": 123,
          "daysToKeep": 30,
          "minimumToKeep": 10,
          "deleteBuildRecord": true,
          "deleteTestResults": true,
          "applyTo": "all"
        }
      ]
    }
    ```

- **Update**: Actualiza una regla de retención existente.
  - **Endpoint:** `PATCH /_apis/build/retention/{retentionId}`
  - **Parámetros:**
    - `retentionId` (int, requerido): ID de la regla de retención.
    - `api-version` (string, requerido): Versión de la API.
    - Body: Objeto JSON con los campos a actualizar (por ejemplo, `daysToKeep`, `minimumToKeep`, etc.).
  - **Ejemplo de request:**
    ```bash
    curl -X PATCH \
      'https://dev.azure.com/{organization}/{project}/_apis/build/retention/1?api-version=7.2-preview.1' \
      -H 'Content-Type: application/json' \
      -H 'Authorization: Basic <PAT>' \
      -d '{ "daysToKeep": 60, "minimumToKeep": 5 }'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "id": 1,
      "definitionId": 123,
      "daysToKeep": 60,
      "minimumToKeep": 5,
      "deleteBuildRecord": true,
      "deleteTestResults": true,
      "applyTo": "all"
    }
    ```

**Notas:**
- Las reglas de retención permiten definir cuántos días y cuántos builds se conservan antes de ser eliminados.
- Los campos principales son: `daysToKeep`, `minimumToKeep`, `deleteBuildRecord`, `deleteTestResults`, `applyTo`.
- Es posible tener reglas diferentes por definición de build.
- Útil para optimizar almacenamiento y cumplir políticas de retención de datos.

**Más información:** [Retention API Reference](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/retention?view=azure-devops-rest-7.2)

## Tags

La subsección **Tags** de la Build API permite gestionar las etiquetas (tags) asociadas a builds y definiciones de build. Las tags facilitan la organización, búsqueda y categorización de builds en proyectos grandes.

**Operaciones principales:**

- **List Build Tags**: Lista las tags asociadas a un build específico.
  - **Endpoint:** `GET /_apis/build/builds/{buildId}/tags`
  - **Parámetros:**
    - `buildId` (int, requerido): ID del build.
    - `api-version` (string, requerido): Versión de la API (ejemplo: `7.2-preview.1`).
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/builds/456/tags?api-version=7.2-preview.1' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "count": 2,
      "value": ["release", "hotfix"]
    }
    ```

- **Add Build Tag**: Agrega una tag a un build.
  - **Endpoint:** `POST /_apis/build/builds/{buildId}/tags/{tag}`
  - **Parámetros:**
    - `buildId` (int, requerido): ID del build.
    - `tag` (string, requerido): Tag a agregar.
    - `api-version` (string, requerido): Versión de la API.
  - **Ejemplo de request:**
    ```bash
    curl -X POST \
      'https://dev.azure.com/{organization}/{project}/_apis/build/builds/456/tags/release?api-version=7.2-preview.1' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "count": 3,
      "value": ["release", "hotfix", "QA"]
    }
    ```

- **Delete Build Tag**: Elimina una tag de un build.
  - **Endpoint:** `DELETE /_apis/build/builds/{buildId}/tags/{tag}`
  - **Parámetros:**
    - `buildId` (int, requerido): ID del build.
    - `tag` (string, requerido): Tag a eliminar.
    - `api-version` (string, requerido): Versión de la API.
  - **Ejemplo de request:**
    ```bash
    curl -X DELETE \
      'https://dev.azure.com/{organization}/{project}/_apis/build/builds/456/tags/release?api-version=7.2-preview.1' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "count": 2,
      "value": ["hotfix", "QA"]
    }
    ```

- **List Definition Tags**: Lista las tags asociadas a una definición de build.
  - **Endpoint:** `GET /_apis/build/definitions/{definitionId}/tags`
  - **Parámetros:**
    - `definitionId` (int, requerido): ID de la definición.
    - `api-version` (string, requerido): Versión de la API.
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/definitions/123/tags?api-version=7.2-preview.1' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "count": 1,
      "value": ["CI"]
    }
    ```

- **Add Definition Tag**: Agrega una tag a una definición de build.
  - **Endpoint:** `POST /_apis/build/definitions/{definitionId}/tags/{tag}`
  - **Parámetros:**
    - `definitionId` (int, requerido): ID de la definición.
    - `tag` (string, requerido): Tag a agregar.
    - `api-version` (string, requerido): Versión de la API.
  - **Ejemplo de request:**
    ```bash
    curl -X POST \
      'https://dev.azure.com/{organization}/{project}/_apis/build/definitions/123/tags/CI?api-version=7.2-preview.1' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "count": 2,
      "value": ["CI", "release"]
    }
    ```

- **Delete Definition Tag**: Elimina una tag de una definición de build.
  - **Endpoint:** `DELETE /_apis/build/definitions/{definitionId}/tags/{tag}`
  - **Parámetros:**
    - `definitionId` (int, requerido): ID de la definición.
    - `tag` (string, requerido): Tag a eliminar.
    - `api-version` (string, requerido): Versión de la API.
  - **Ejemplo de request:**
    ```bash
    curl -X DELETE \
      'https://dev.azure.com/{organization}/{project}/_apis/build/definitions/123/tags/CI?api-version=7.2-preview.1' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "count": 1,
      "value": ["release"]
    }
    ```

**Notas:**
- Las tags permiten categorizar builds y definiciones para facilitar búsquedas y reportes.
- Se pueden agregar o eliminar tags dinámicamente según el ciclo de vida del build.
- Útil para filtrar builds por ambiente, release, tipo de cambio, etc.

**Más información:** [Tags API Reference](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/tags?view=azure-devops-rest-7.2)

## Work Items

La subsección **Work Items** de la Build API permite consultar los work items (elementos de trabajo) asociados a builds o a un rango de builds. Esto es útil para rastrear qué historias, bugs o tareas se relacionan con un build específico o con cambios entre builds.

**Operaciones principales:**

- **List Work Items for Build**: Lista los work items asociados a un build específico.
  - **Endpoint:** `GET /_apis/build/builds/{buildId}/workitems`
  - **Parámetros:**
    - `buildId` (int, requerido): ID del build.
    - `api-version` (string, requerido): Versión de la API (ejemplo: `7.2-preview.2`).
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/builds/456/workitems?api-version=7.2-preview.2' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "count": 2,
      "value": [
        { "id": 789, "url": "https://dev.azure.com/.../workitems/789" },
        { "id": 790, "url": "https://dev.azure.com/.../workitems/790" }
      ]
    }
    ```

- **List Work Items Between Builds**: Lista los work items asociados a los cambios entre dos builds.
  - **Endpoint:** `GET /_apis/build/workitems?fromBuildId={fromBuildId}&toBuildId={toBuildId}`
  - **Parámetros:**
    - `fromBuildId` (int, requerido): ID del build inicial.
    - `toBuildId` (int, requerido): ID del build final.
    - `api-version` (string, requerido): Versión de la API.
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/workitems?fromBuildId=123&toBuildId=456&api-version=7.2-preview.2' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "count": 1,
      "value": [
        { "id": 789, "url": "https://dev.azure.com/.../workitems/789" }
      ]
    }
    ```

**Notas:**
- Los work items pueden ser de tipo historia, bug, tarea, etc., según la configuración del proyecto.
- Permite rastrear el impacto de los builds sobre el backlog y la trazabilidad de cambios.
- Útil para reportes de auditoría, releases y análisis de cobertura de trabajo.

**Más información:** [Work Items API Reference](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/workitems?view=azure-devops-rest-7.2)

La subsección **Deployments** de la Build API permite consultar los despliegues (deployments) realizados a partir de builds en Azure DevOps. Es útil para rastrear qué builds han sido desplegados, en qué entornos y cuándo.

**Operaciones principales:**

- **List**: Lista los deployments asociados a un build.
  - **Endpoint:** `GET /_apis/build/builds/{buildId}/deployments`
  - **Parámetros:**
    - `buildId` (int, requerido): ID del build.
    - `api-version` (string, requerido): Versión de la API (ejemplo: `7.2-preview.1`).
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/builds/456/deployments?api-version=7.2-preview.1' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "count": 1,
      "value": [
        {
          "deploymentId": 789,
          "build": { "id": 456 },
          "environment": "Production",
          "deployedBy": { "displayName": "user@domain.com" },
          "deployedOn": "2025-08-04T12:34:56Z",
          "status": "succeeded"
        }
      ]
    }
    ```

**Notas:**
- Los deployments permiten rastrear la promoción de builds a diferentes entornos (por ejemplo, QA, Staging, Production).
- El campo `status` puede tener valores como `succeeded`, `failed`, `inProgress`, etc.

**Más información:** [Deployments API Reference](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/deployments?view=azure-devops-rest-7.2)

La subsección **Definition Templates** de la Build API permite trabajar con plantillas reutilizables de definiciones de build. Las plantillas facilitan la creación de nuevas definiciones a partir de configuraciones predefinidas.

**Operaciones principales:**

- **List**: Lista todas las plantillas de definición disponibles.
  - **Endpoint:** `GET /_apis/build/definitions/templates`
  - **Parámetros (query):**
    - `api-version` (string, requerido): Versión de la API (ejemplo: `7.2-preview.1`).
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/definitions/templates?api-version=7.2-preview.1' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "count": 1,
      "value": [
        {
          "templateId": "my-template",
          "name": "My Template",
          "description": "Reusable build template",
          "templateType": "build"
        }
      ]
    }
    ```

- **Get**: Obtiene los detalles de una plantilla específica.
  - **Endpoint:** `GET /_apis/build/definitions/templates/{templateId}`
  - **Parámetros:**
    - `templateId` (string, requerido): ID de la plantilla.
    - `api-version` (string, requerido): Versión de la API.
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/definitions/templates/my-template?api-version=7.2-preview.1' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "templateId": "my-template",
      "name": "My Template",
      "description": "Reusable build template",
      "templateType": "build",
      "template": {
        "process": { "yamlFilename": "azure-pipelines.yml" },
        "variables": { "var1": { "value": "abc" } }
      }
    }
    ```

**Notas:**
- Las plantillas pueden ser utilizadas al crear nuevas definiciones de build para estandarizar configuraciones.
- El campo `template` contiene la estructura reutilizable (por ejemplo, proceso, variables, triggers, etc.).

**Más información:** [Definition Templates API Reference](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/templates?view=azure-devops-rest-7.2)

La subsección **Definitions** de la Build API permite gestionar las definiciones de pipelines de build en Azure DevOps. Una definición describe el proceso de build, los repositorios, los triggers, las variables y los pasos a ejecutar.

**Operaciones principales:**

  - **Endpoint:** `GET /_apis/build/definitions`
  - **Parámetros (query):**
    - `name` (string, opcional): Filtra por nombre de definición.
    - `repositoryId` (string, opcional): Filtra por ID de repositorio.
    - `repositoryType` (string, opcional): Tipo de repositorio (por ejemplo, `TfsGit`, `GitHub`).
    - `queryOrder` (string, opcional): Orden de los resultados. Valores: `none`, `queueTimeAscending`, `queueTimeDescending`, `definitionNameAscending`, `lastModifiedDescending`.
    - `top` (int, opcional): Número máximo de definiciones a retornar.
    - `api-version` (string, requerido): Versión de la API.
    - `propertyFilters` (string, opcional): Filtros de propiedades adicionales a incluir en la respuesta. Permite especificar una lista separada por comas de nombres de propiedades que se desean obtener en el objeto definición, además de las propiedades estándar. Si se omite, solo se devuelven los campos básicos de la definición.
      - **Tipo:** string (separado por comas)
      - **Valores posibles y significado:**
        - `properties`: Incluye el objeto de propiedades personalizadas asociadas a la definición.
        - `triggers`: Incluye los triggers configurados en la definición.
        - `variables`: Incluye las variables definidas en la definición.
        - `repository`: Incluye detalles extendidos del repositorio fuente.
        - `process`: Incluye el proceso de build (pasos, fases, etc.).
        - `all`: Incluye todas las propiedades adicionales disponibles (puede aumentar el tamaño de la respuesta).
        - Puedes combinar varios valores separados por coma, por ejemplo: `properties,variables,triggers`.
      - **Ejemplo de uso:**
        ```bash
        curl -X GET \
          'https://dev.azure.com/{organization}/{project}/_apis/build/definitions?propertyFilters=properties,variables,triggers&api-version=7.2-preview.7' \
          -H 'Authorization: Basic <PAT>'
        ```
      - **Ejemplo de valor en JSON:**
        ```json
        {
          "count": 1,
          "value": [
            {
              "id": 123,
              "name": "MyPipeline",
              "properties": { "customKey": "value" },
              "variables": { "var1": { "value": "abc" } },
              "triggers": [ { "triggerType": "continuousIntegration" } ],
              ...
            }
          ]
        }
        ```
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/definitions?name=MyPipeline&api-version=7.2-preview.7' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "count": 1,
      "value": [
        {
          "id": 123,
          "name": "MyPipeline",
          "path": "\\",
          "type": "build",
          "queueStatus": "enabled",
          ...
        }
      ]
    }
    ```

  - **Endpoint:** `GET /_apis/build/definitions/{definitionId}`
  - **Parámetros:**
    - `definitionId` (int, requerido): ID de la definición.
    - `revision` (int, opcional): Número de revisión específico a obtener.
    - `propertyFilters` (string, opcional): Filtros de propiedades adicionales a incluir (ver explicación en sección Builds).
    - `api-version` (string, requerido): Versión de la API.
    - `propertyFilters` (string, opcional): Filtros de propiedades adicionales a incluir en la respuesta. Permite especificar una lista separada por comas de nombres de propiedades que se desean obtener en el objeto definición, además de las propiedades estándar. Ver explicación y ejemplos en la operación List.
  - **Ejemplo de request:**
    ```bash
    curl -X GET \
      'https://dev.azure.com/{organization}/{project}/_apis/build/definitions/123?api-version=7.2-preview.7' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "id": 123,
      "name": "MyPipeline",
      "revision": 4,
      "queueStatus": "enabled",
      "repository": { "id": "...", "type": "TfsGit" },
      ...
    }
    ```

  - **Endpoint:**
    - Crear: `POST /_apis/build/definitions`
    - Actualizar: `PUT /_apis/build/definitions/{definitionId}`
  - **Body:** Objeto JSON con la definición completa (ver ejemplo en la documentación oficial).
  - **Body:** Objeto JSON con la definición completa. Los campos principales incluyen:
    - `name` (string, requerido): Nombre de la definición.
    - `repository` (objeto, requerido): Información del repositorio fuente. Ejemplo:
      ```json
      {
        "id": "repoId",
        "type": "TfsGit",
        "name": "MyRepo",
        "defaultBranch": "refs/heads/main"
      }
      ```
    - `process` (objeto, requerido): Proceso de build (puede ser YAML o clásico). Ejemplo para YAML:
      ```json
      {
        "yamlFilename": "azure-pipelines.yml"
      }
      ```
    - `triggers` (array, opcional): Triggers configurados (por ejemplo, CI, PR, schedule).
    - `variables` (objeto, opcional): Variables de la definición. Ejemplo:
      ```json
      {
        "var1": { "value": "abc" },
        "var2": { "value": "123", "allowOverride": true }
      }
      ```
    - `queue` (objeto, opcional): Cola de agentes a utilizar.
    - `options`, `retentionRules`, `demands`, `buildNumberFormat`, etc. (ver documentación oficial para todos los campos posibles).
  - **Ejemplo de request (crear):**
    ```bash
    curl -X POST \
      'https://dev.azure.com/{organization}/{project}/_apis/build/definitions?api-version=7.2-preview.7' \
      -H 'Content-Type: application/json' \
      -H 'Authorization: Basic <PAT>' \
      -d '{
        "name": "MyPipeline",
        "repository": { "id": "repoId", "type": "TfsGit", "name": "MyRepo", "defaultBranch": "refs/heads/main" },
        "process": { "yamlFilename": "azure-pipelines.yml" },
        "triggers": [ { "triggerType": "continuousIntegration" } ],
        "variables": { "var1": { "value": "abc" } }
      }'
    ```
  - **Ejemplo de request (actualizar):**
    ```bash
    curl -X PUT \
      'https://dev.azure.com/{organization}/{project}/_apis/build/definitions/123?api-version=7.2-preview.7' \
      -H 'Content-Type: application/json' \
      -H 'Authorization: Basic <PAT>' \
      -d '{
        "name": "MyPipelineUpdated",
        "variables": { "var1": { "value": "xyz" } }
      }'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "id": 124,
      "name": "MyPipeline",
      "repository": { "id": "repoId", "type": "TfsGit" },
      "process": { "yamlFilename": "azure-pipelines.yml" },
      "variables": { "var1": { "value": "abc" } },
      ...
    }
    ```
  - **Notas:**
    - El cuerpo debe incluir todos los campos requeridos para la definición. Si se omite un campo obligatorio, la API retornará un error.
    - Para actualizaciones parciales, se debe enviar el objeto completo de la definición (PUT). No existe un PATCH parcial para definiciones.
    - Consultar la [documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/definitions/create?view=azure-devops-rest-7.2) para ejemplos completos de JSON.

- **Delete**: Elimina una definición de build.
  - **Endpoint:** `DELETE /_apis/build/definitions/{definitionId}`
  - **Parámetros:**
    - `definitionId` (int, requerido): ID de la definición.
    - `api-version` (string, requerido): Versión de la API.
  - **Ejemplo de request:**
    ```bash
    curl -X DELETE \
      'https://dev.azure.com/{organization}/{project}/_apis/build/definitions/123?api-version=7.2-preview.7' \
      -H 'Authorization: Basic <PAT>'
    ```
  - **Ejemplo de response:**
    ```json
    {
      "id": 123,
      "deleted": true
    }
    ```

**Más información:** [Definitions API Reference](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/definitions?view=azure-devops-rest-7.2)
