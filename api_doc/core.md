---

# Azure DevOps Core API Documentation (v7.2)

## Progreso de la documentación

| Sección / Operación         | Documentado | Notas |
|----------------------------|:-----------:|-------|
| Avatar                     |     ✅      |       |
| Categorized Teams          |     ✅      |       |
| Processes                  |     ✅      |       |
| Projects                   |     ✅      |       |
| Teams                      |     ✅      |       |

Este documento cubre todas las operaciones del área Core de la API REST de Azure DevOps (v7.2), incluyendo las subsecciones:

- [Avatar](https://learn.microsoft.com/en-us/rest/api/azure/devops/core/avatar?view=azure-devops-rest-7.2)
- [Categorized Teams](https://learn.microsoft.com/en-us/rest/api/azure/devops/core/categorized-teams?view=azure-devops-rest-7.2)
- [Processes](https://learn.microsoft.com/en-us/rest/api/azure/devops/core/processes?view=azure-devops-rest-7.2)
- [Projects](https://learn.microsoft.com/en-us/rest/api/azure/devops/core/projects?view=azure-devops-rest-7.2)
- [Teams](https://learn.microsoft.com/en-us/rest/api/azure/devops/core/teams?view=azure-devops-rest-7.2)

Cada sección incluirá explicación funcional, parámetros, ejemplos cURL, request/response y todos los valores posibles para filtros y enums.

---

*La documentación detallada de cada subsección y operación se agregará en las siguientes secciones.*

---

## Avatar

La API de Avatar permite obtener y actualizar los avatares (imágenes de perfil) asociados a usuarios, equipos y proyectos en Azure DevOps. Es útil para personalizar la experiencia visual y para identificar rápidamente entidades en la interfaz.

**Documentación oficial:** [Avatar](https://learn.microsoft.com/en-us/rest/api/azure/devops/core/avatar?view=azure-devops-rest-7.2)

### Operaciones principales

| Operación         | Endpoint                                                                                 | Método |
|-------------------|------------------------------------------------------------------------------------------|--------|
| Get Avatar        | /_apis/graph/avatars/{subjectDescriptor}                                                  | GET    |
| Set Avatar        | /_apis/graph/avatars/{subjectDescriptor}                                                  | PUT    |

#### Notas


## Categorized Teams

## Processes

## Projects

## Teams

La API de Teams permite crear, consultar, actualizar y eliminar equipos dentro de un proyecto de Azure DevOps, así como listar sus miembros y obtener información detallada. Es fundamental para la gestión de la colaboración y la organización de los equipos de trabajo.

**Documentación oficial:** [Teams](https://learn.microsoft.com/en-us/rest/api/azure/devops/core/teams?view=azure-devops-rest-7.2)

### Operaciones principales

| Operación                              | Endpoint                                                                                                 | Método |
|-----------------------------------------|----------------------------------------------------------------------------------------------------------|--------|
| Create                                 | /_apis/projects/{projectId}/teams?api-version=7.2-preview.3                                              | POST   |
| Delete                                 | /_apis/projects/{projectId}/teams/{teamId}?api-version=7.2-preview.3                                     | DELETE |
| Get                                    | /_apis/projects/{projectId}/teams/{teamId}?api-version=7.2-preview.3                                     | GET    |
| Get All Teams                          | /_apis/teams?api-version=7.2-preview.3                                                                  | GET    |
| Get Team Members With Extended Properties | /_apis/projects/{projectId}/teams/{teamId}/members?api-version=7.2-preview.3                            | GET    |
| Get Teams                              | /_apis/projects/{projectId}/teams?api-version=7.2-preview.3                                             | GET    |
| Update                                 | /_apis/projects/{projectId}/teams/{teamId}?api-version=7.2-preview.3                                     | PATCH  |

---

### List Teams (Get All Teams)

Obtiene la lista de todos los equipos de la organización.

**Endpoint:**
`GET https://dev.azure.com/{organization}/_apis/teams?api-version=7.2-preview.3`

#### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                |
|----------------|-----------|-----------|--------|--------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps  |
| mine           | query     | No        | bool   | Si es true, solo equipos donde el usuario es miembro |
| top            | query     | No        | int    | Número máximo de equipos a retornar        |
| skip           | query     | No        | int    | Número de equipos a omitir (paginación)    |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.3'    |

#### Ejemplo de consumo con cURL

```bash
curl -X GET \
  'https://dev.azure.com/fabrikam/_apis/teams?api-version=7.2-preview.3' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

#### Ejemplo de respuesta

```json
{
  "count": 2,
  "value": [
    {
      "id": "f3a1b1c2-1234-5678-9abc-def012345678",
      "name": "Team Alpha",
      "description": "Development team",
      "url": "https://dev.azure.com/fabrikam/_apis/teams/f3a1b1c2-1234-5678-9abc-def012345678"
    },
    {
      "id": "a2b3c4d5-2345-6789-abcd-ef0123456789",
      "name": "Team Beta",
      "description": "QA team",
      "url": "https://dev.azure.com/fabrikam/_apis/teams/a2b3c4d5-2345-6789-abcd-ef0123456789"
    }
  ]
}
```

---

### Get Team

Obtiene la información de un equipo específico por su ID.

**Endpoint:**
`GET https://dev.azure.com/{organization}/_apis/projects/{projectId}/teams/{teamId}?api-version=7.2-preview.3`

#### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                |
|----------------|-----------|-----------|--------|--------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps  |
| projectId      | path      | Sí        | string | ID o nombre del proyecto                   |
| teamId         | path      | Sí        | string | ID o nombre del equipo                     |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.3'    |

#### Ejemplo de consumo con cURL

```bash
curl -X GET \
  'https://dev.azure.com/fabrikam/_apis/projects/ProjectA/teams/f3a1b1c2-1234-5678-9abc-def012345678?api-version=7.2-preview.3' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

#### Ejemplo de respuesta

```json
{
  "id": "f3a1b1c2-1234-5678-9abc-def012345678",
  "name": "Team Alpha",
  "description": "Development team",
  "url": "https://dev.azure.com/fabrikam/_apis/projects/ProjectA/teams/f3a1b1c2-1234-5678-9abc-def012345678"
}
```

---

### Create Team

Crea un nuevo equipo en un proyecto.

**Endpoint:**
`POST https://dev.azure.com/{organization}/_apis/projects/{projectId}/teams?api-version=7.2-preview.3`

#### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                |
|----------------|-----------|-----------|--------|--------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps  |
| projectId      | path      | Sí        | string | ID o nombre del proyecto                   |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.3'    |

#### Cuerpo de la petición (Request Body)

| Campo         | Tipo     | Requerido | Descripción                                 |
|---------------|----------|-----------|---------------------------------------------|
| name          | string   | Sí        | Nombre del equipo                           |
| description   | string   | No        | Descripción del equipo                      |

#### Ejemplo de consumo con cURL

```bash
curl -X POST \
  'https://dev.azure.com/fabrikam/_apis/projects/ProjectA/teams?api-version=7.2-preview.3' \
  -H 'Authorization: Basic <BASE64_PAT>' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "NuevoEquipo",
    "description": "Equipo de desarrollo"
  }'
```

#### Ejemplo de respuesta

```json
{
  "id": "b2c3d4e5-6789-4abc-def0-1234567890ab",
  "name": "NuevoEquipo",
  "description": "Equipo de desarrollo",
  "url": "https://dev.azure.com/fabrikam/_apis/projects/ProjectA/teams/b2c3d4e5-6789-4abc-def0-1234567890ab"
}
```

---

### Update Team

Actualiza el nombre o la descripción de un equipo existente.

**Endpoint:**
`PATCH https://dev.azure.com/{organization}/_apis/projects/{projectId}/teams/{teamId}?api-version=7.2-preview.3`

#### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                |
|----------------|-----------|-----------|--------|--------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps  |
| projectId      | path      | Sí        | string | ID o nombre del proyecto                   |
| teamId         | path      | Sí        | string | ID o nombre del equipo                     |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.3'    |

#### Cuerpo de la petición (Request Body)

| Campo         | Tipo     | Requerido | Descripción                                 |
|---------------|----------|-----------|---------------------------------------------|
| name          | string   | No        | Nuevo nombre del equipo                     |
| description   | string   | No        | Nueva descripción                           |

#### Ejemplo de consumo con cURL

```bash
curl -X PATCH \
  'https://dev.azure.com/fabrikam/_apis/projects/ProjectA/teams/b2c3d4e5-6789-4abc-def0-1234567890ab?api-version=7.2-preview.3' \
  -H 'Authorization: Basic <BASE64_PAT>' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "EquipoActualizado",
    "description": "Descripción actualizada"
  }'
```

#### Ejemplo de respuesta

```json
{
  "id": "b2c3d4e5-6789-4abc-def0-1234567890ab",
  "name": "EquipoActualizado",
  "description": "Descripción actualizada",
  "url": "https://dev.azure.com/fabrikam/_apis/projects/ProjectA/teams/b2c3d4e5-6789-4abc-def0-1234567890ab"
}
```

---

### Delete Team

Elimina un equipo por su ID.

**Endpoint:**
`DELETE https://dev.azure.com/{organization}/_apis/projects/{projectId}/teams/{teamId}?api-version=7.2-preview.3`

#### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                |
|----------------|-----------|-----------|--------|--------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps  |
| projectId      | path      | Sí        | string | ID o nombre del proyecto                   |
| teamId         | path      | Sí        | string | ID o nombre del equipo                     |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.3'    |

#### Ejemplo de consumo con cURL

```bash
curl -X DELETE \
  'https://dev.azure.com/fabrikam/_apis/projects/ProjectA/teams/b2c3d4e5-6789-4abc-def0-1234567890ab?api-version=7.2-preview.3' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

#### Respuesta
- **Status code:** 204 (No Content)

---

### Get Team Members With Extended Properties

Obtiene la lista de miembros de un equipo, incluyendo propiedades extendidas.

**Endpoint:**
`GET https://dev.azure.com/{organization}/_apis/projects/{projectId}/teams/{teamId}/members?api-version=7.2-preview.3`

#### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                |
|----------------|-----------|-----------|--------|--------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps  |
| projectId      | path      | Sí        | string | ID o nombre del proyecto                   |
| teamId         | path      | Sí        | string | ID o nombre del equipo                     |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.3'    |

#### Ejemplo de consumo con cURL

```bash
curl -X GET \
  'https://dev.azure.com/fabrikam/_apis/projects/ProjectA/teams/b2c3d4e5-6789-4abc-def0-1234567890ab/members?api-version=7.2-preview.3' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

#### Ejemplo de respuesta

```json
{
  "count": 2,
  "value": [
    {
      "id": "1a2b3c4d-5678-9abc-def0-1234567890ab",
      "displayName": "Juan Pérez",
      "uniqueName": "juan.perez@fabrikam.com",
      "url": "https://dev.azure.com/fabrikam/_apis/identities/1a2b3c4d-5678-9abc-def0-1234567890ab"
    },
    {
      "id": "2b3c4d5e-6789-4abc-def0-1234567890ab",
      "displayName": "Ana Gómez",
      "uniqueName": "ana.gomez@fabrikam.com",
      "url": "https://dev.azure.com/fabrikam/_apis/identities/2b3c4d5e-6789-4abc-def0-1234567890ab"
    }
  ]
}
```

#### Seguridad y permisos
- Scope requerido: `vso.project` o `vso.team`

---

La API de Projects permite crear, consultar, actualizar y eliminar proyectos de equipo en Azure DevOps, así como gestionar sus propiedades y estados. Es fundamental para la administración de la estructura organizacional y la automatización de la gestión de proyectos.

**Documentación oficial:** [Projects](https://learn.microsoft.com/en-us/rest/api/azure/devops/core/projects?view=azure-devops-rest-7.2)

### Operaciones principales

| Operación                | Endpoint                                                                                                 | Método |
|--------------------------|----------------------------------------------------------------------------------------------------------|--------|
| Create                   | /_apis/projects?api-version=7.2-preview.4                                                                | POST   |
| Delete                   | /_apis/projects/{projectId}?api-version=7.2-preview.4                                                    | DELETE |
| Get                      | /_apis/projects/{projectId}?api-version=7.2-preview.4                                                    | GET    |
| Get Project Properties   | /_apis/projects/{projectId}/properties?api-version=7.2-preview.1                                          | GET    |
| List                     | /_apis/projects?api-version=7.2-preview.4                                                                | GET    |
| Set Project Properties   | /_apis/projects/{projectId}/properties?api-version=7.2-preview.1                                          | PATCH  |
| Update                   | /_apis/projects/{projectId}?api-version=7.2-preview.4                                                    | PATCH  |

---

### List Projects

Obtiene la lista de proyectos de la organización.

**Endpoint:**
`GET https://dev.azure.com/{organization}/_apis/projects?api-version=7.2-preview.4`

#### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                |
|----------------|-----------|-----------|--------|--------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps  |
| stateFilter    | query     | No        | string | Filtra por estado: WellFormed, Creating, Deleting, New |
| top            | query     | No        | int    | Número máximo de proyectos a retornar      |
| continuationToken | query  | No        | string | Token para paginación                     |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.4'    |

#### Ejemplo de consumo con cURL

```bash
curl -X GET \
  'https://dev.azure.com/fabrikam/_apis/projects?api-version=7.2-preview.4' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

#### Ejemplo de respuesta

```json
{
  "count": 2,
  "value": [
    {
      "id": "6ce954b1-ce1f-45d1-b94d-e6bf2464ba2c",
      "name": "Fabrikam-Fiber-Git",
      "description": "Git project for Fabrikam Fiber team.",
      "url": "https://dev.azure.com/fabrikam/_apis/projects/6ce954b1-ce1f-45d1-b94d-e6bf2464ba2c",
      "state": "wellFormed",
      "revision": 24,
      "visibility": "private",
      "lastUpdateTime": "2023-01-01T12:00:00.000Z"
    },
    {
      "id": "b6a330a1-5c1e-4e6a-8e2e-2b7b5b1c1e2e",
      "name": "Fabrikam-Fiber-TFVC",
      "description": "TFVC project for Fabrikam Fiber team.",
      "url": "https://dev.azure.com/fabrikam/_apis/projects/b6a330a1-5c1e-4e6a-8e2e-2b7b5b1c1e2e",
      "state": "wellFormed",
      "revision": 12,
      "visibility": "private",
      "lastUpdateTime": "2023-01-02T12:00:00.000Z"
    }
  ]
}
```

---

### Get Project

Obtiene la información de un proyecto específico por su ID o nombre.

**Endpoint:**
`GET https://dev.azure.com/{organization}/_apis/projects/{projectId}?api-version=7.2-preview.4`

#### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                |
|----------------|-----------|-----------|--------|--------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps  |
| projectId      | path      | Sí        | string | ID o nombre del proyecto                   |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.4'    |

#### Ejemplo de consumo con cURL

```bash
curl -X GET \
  'https://dev.azure.com/fabrikam/_apis/projects/6ce954b1-ce1f-45d1-b94d-e6bf2464ba2c?api-version=7.2-preview.4' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

#### Ejemplo de respuesta

```json
{
  "id": "6ce954b1-ce1f-45d1-b94d-e6bf2464ba2c",
  "name": "Fabrikam-Fiber-Git",
  "description": "Git project for Fabrikam Fiber team.",
  "url": "https://dev.azure.com/fabrikam/_apis/projects/6ce954b1-ce1f-45d1-b94d-e6bf2464ba2c",
  "state": "wellFormed",
  "revision": 24,
  "visibility": "private",
  "lastUpdateTime": "2023-01-01T12:00:00.000Z"
}
```

---

### Create Project

Crea un nuevo proyecto de equipo.

**Endpoint:**
`POST https://dev.azure.com/{organization}/_apis/projects?api-version=7.2-preview.4`

#### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                |
|----------------|-----------|-----------|--------|--------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps  |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.4'    |

#### Cuerpo de la petición (Request Body)

| Campo         | Tipo     | Requerido | Descripción                                 |
|---------------|----------|-----------|---------------------------------------------|
| name          | string   | Sí        | Nombre del proyecto                         |
| description   | string   | No        | Descripción del proyecto                    |
| capabilities  | object   | Sí        | Configuración de repositorio y proceso      |
| visibility    | string   | No        | private, public                            |

Ejemplo de capabilities:

```json
{
  "versioncontrol": { "sourceControlType": "Git" },
  "processTemplate": { "templateTypeId": "adcc42ab-9882-485e-a3ed-7678f01f66bc" }
}
```

#### Ejemplo de consumo con cURL

```bash
curl -X POST \
  'https://dev.azure.com/fabrikam/_apis/projects?api-version=7.2-preview.4' \
  -H 'Authorization: Basic <BASE64_PAT>' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "NuevoProyecto",
    "description": "Proyecto de ejemplo",
    "capabilities": {
      "versioncontrol": { "sourceControlType": "Git" },
      "processTemplate": { "templateTypeId": "adcc42ab-9882-485e-a3ed-7678f01f66bc" }
    },
    "visibility": "private"
  }'
```

#### Ejemplo de respuesta

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef0123456789",
  "name": "NuevoProyecto",
  "description": "Proyecto de ejemplo",
  "url": "https://dev.azure.com/fabrikam/_apis/projects/a1b2c3d4-e5f6-7890-abcd-ef0123456789",
  "state": "creating",
  "revision": 1,
  "visibility": "private",
  "lastUpdateTime": "2023-01-10T12:00:00.000Z"
}
```

---

### Delete Project

Elimina un proyecto por su ID.

**Endpoint:**
`DELETE https://dev.azure.com/{organization}/_apis/projects/{projectId}?api-version=7.2-preview.4`

#### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                |
|----------------|-----------|-----------|--------|--------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps  |
| projectId      | path      | Sí        | string | ID o nombre del proyecto                   |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.4'    |

#### Ejemplo de consumo con cURL

```bash
curl -X DELETE \
  'https://dev.azure.com/fabrikam/_apis/projects/a1b2c3d4-e5f6-7890-abcd-ef0123456789?api-version=7.2-preview.4' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

#### Respuesta
- **Status code:** 202 (Accepted)
- El borrado es asíncrono, consultar estado con GetOperation.

---

### Update Project

Actualiza el nombre, descripción o visibilidad de un proyecto existente.

**Endpoint:**
`PATCH https://dev.azure.com/{organization}/_apis/projects/{projectId}?api-version=7.2-preview.4`

#### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                |
|----------------|-----------|-----------|--------|--------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps  |
| projectId      | path      | Sí        | string | ID o nombre del proyecto                   |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.4'    |

#### Cuerpo de la petición (Request Body)

| Campo         | Tipo     | Requerido | Descripción                                 |
|---------------|----------|-----------|---------------------------------------------|
| name          | string   | No        | Nuevo nombre del proyecto                   |
| description   | string   | No        | Nueva descripción                           |
| visibility    | string   | No        | private, public                            |

#### Ejemplo de consumo con cURL

```bash
curl -X PATCH \
  'https://dev.azure.com/fabrikam/_apis/projects/a1b2c3d4-e5f6-7890-abcd-ef0123456789?api-version=7.2-preview.4' \
  -H 'Authorization: Basic <BASE64_PAT>' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "NuevoNombre",
    "description": "Descripción actualizada",
    "visibility": "public"
  }'
```

#### Ejemplo de respuesta

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef0123456789",
  "name": "NuevoNombre",
  "description": "Descripción actualizada",
  "url": "https://dev.azure.com/fabrikam/_apis/projects/a1b2c3d4-e5f6-7890-abcd-ef0123456789",
  "state": "wellFormed",
  "revision": 2,
  "visibility": "public",
  "lastUpdateTime": "2023-01-11T12:00:00.000Z"
}
```

---

### Get Project Properties

Obtiene las propiedades personalizadas de un proyecto.

**Endpoint:**
`GET https://dev.azure.com/{organization}/_apis/projects/{projectId}/properties?api-version=7.2-preview.1`

#### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                |
|----------------|-----------|-----------|--------|--------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps  |
| projectId      | path      | Sí        | string | ID o nombre del proyecto                   |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.1'    |

#### Ejemplo de consumo con cURL

```bash
curl -X GET \
  'https://dev.azure.com/fabrikam/_apis/projects/a1b2c3d4-e5f6-7890-abcd-ef0123456789/properties?api-version=7.2-preview.1' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

#### Ejemplo de respuesta

```json
{
  "count": 1,
  "value": [
    {
      "name": "System.Description",
      "value": "Proyecto de ejemplo"
    }
  ]
}
```

---

### Set Project Properties

Crea, actualiza o elimina propiedades personalizadas de un proyecto.

**Endpoint:**
`PATCH https://dev.azure.com/{organization}/_apis/projects/{projectId}/properties?api-version=7.2-preview.1`

#### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                |
|----------------|-----------|-----------|--------|--------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps  |
| projectId      | path      | Sí        | string | ID o nombre del proyecto                   |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.1'    |

#### Cuerpo de la petición (Request Body)

Array de operaciones tipo JSON Patch (add, replace, remove):

```json
[
  {
    "op": "add",
    "path": "/System.Description",
    "value": "Nuevo valor de descripción"
  }
]
```

#### Ejemplo de consumo con cURL

```bash
curl -X PATCH \
  'https://dev.azure.com/fabrikam/_apis/projects/a1b2c3d4-e5f6-7890-abcd-ef0123456789/properties?api-version=7.2-preview.1' \
  -H 'Authorization: Basic <BASE64_PAT>' \
  -H 'Content-Type: application/json-patch+json' \
  -d '[{"op": "add", "path": "/System.Description", "value": "Nuevo valor de descripción"}]'
```

#### Ejemplo de respuesta

```json
{
  "count": 1,
  "value": [
    {
      "name": "System.Description",
      "value": "Nuevo valor de descripción"
    }
  ]
}
```

#### Seguridad y permisos
- Scope requerido: `vso.project_manage`

---

La API de Processes permite consultar los procesos (metodologías) disponibles en la organización, como Agile, Scrum, CMMI, etc. Es útil para conocer y gestionar los procesos que definen la estructura de los work items y flujos de trabajo en los proyectos de Azure DevOps.

**Documentación oficial:** [Processes](https://learn.microsoft.com/en-us/rest/api/azure/devops/core/processes?view=azure-devops-rest-7.2)

### Operaciones principales

| Operación | Endpoint                                                                                 | Método |
|-----------|------------------------------------------------------------------------------------------|--------|
| Get       | /_apis/process/processes/{processId}?api-version=7.2-preview.1                           | GET    |
| List      | /_apis/process/processes?api-version=7.2-preview.1                                       | GET    |

---

### List Processes

Obtiene la lista de procesos/metodologías disponibles en la organización.

**Endpoint:**
`GET https://dev.azure.com/{organization}/_apis/process/processes?api-version=7.2-preview.1`

#### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                |
|----------------|-----------|-----------|--------|--------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps  |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.1'    |

#### Ejemplo de consumo con cURL

```bash
curl -X GET \
  'https://dev.azure.com/fabrikam/_apis/process/processes?api-version=7.2-preview.1' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

#### Ejemplo de respuesta

```json
{
  "count": 2,
  "value": [
    {
      "id": "adcc42ab-9882-485e-a3ed-7678f01f66bc",
      "name": "Agile",
      "description": "Agile process",
      "isDefault": true,
      "type": "system"
    },
    {
      "id": "6b724908-ef14-45cf-84f8-768b5384da45",
      "name": "Scrum",
      "description": "Scrum process",
      "isDefault": false,
      "type": "system"
    }
  ]
}
```

---

### Get Process

Obtiene la información de un proceso específico por su ID.

**Endpoint:**
`GET https://dev.azure.com/{organization}/_apis/process/processes/{processId}?api-version=7.2-preview.1`

#### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                |
|----------------|-----------|-----------|--------|--------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps  |
| processId      | path      | Sí        | string | ID del proceso                            |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.1'    |

#### Ejemplo de consumo con cURL

```bash
curl -X GET \
  'https://dev.azure.com/fabrikam/_apis/process/processes/adcc42ab-9882-485e-a3ed-7678f01f66bc?api-version=7.2-preview.1' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

#### Ejemplo de respuesta

```json
{
  "id": "adcc42ab-9882-485e-a3ed-7678f01f66bc",
  "name": "Agile",
  "description": "Agile process",
  "isDefault": true,
  "type": "system"
}
```

#### Seguridad y permisos
- Scope requerido: `vso.process`

---

La API de Categorized Teams permite obtener la lista de equipos legibles por el usuario en un proyecto, así como los equipos de los que el usuario es miembro (aunque estén excluidos de la lista legible). Es útil para construir experiencias personalizadas y filtrar equipos según la visibilidad y pertenencia del usuario.

**Documentación oficial:** [Categorized Teams](https://learn.microsoft.com/en-us/rest/api/azure/devops/core/categorized-teams?view=azure-devops-rest-7.2)

### Operaciones principales

| Operación | Endpoint                                                                                                 | Método |
|-----------|----------------------------------------------------------------------------------------------------------|--------|
| Get       | /{organization}/_apis/projects/{projectId}/teams?mine={mine}&api-version=7.2-preview.1                   | GET    |

#### Notas
- El parámetro `mine` permite filtrar los equipos a los que pertenece el usuario autenticado.

---

### Get Categorized Teams

Obtiene la lista de equipos legibles por el usuario y los equipos de los que es miembro (aunque no sean legibles).

**Endpoint:**
`GET https://dev.azure.com/{organization}/_apis/projects/{projectId}/teams?mine={mine}&api-version=7.2-preview.1`

#### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo    | Descripción                                                        |
|----------------|-----------|-----------|---------|--------------------------------------------------------------------|
| organization   | path      | Sí        | string  | Nombre de la organización de Azure DevOps                          |
| projectId      | path      | Sí        | string  | ID o nombre del proyecto                                           |
| mine           | query     | No        | boolean | Si es true, solo retorna equipos donde el usuario es miembro       |
| api-version    | query     | Sí        | string  | Versión de la API. Usar '7.2-preview.1'                            |

#### Ejemplo de consumo con cURL

```bash
curl -X GET \
  'https://dev.azure.com/fabrikam/_apis/projects/ProjectA/teams?mine=true&api-version=7.2-preview.1' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

#### Ejemplo de respuesta

```json
{
  "count": 2,
  "value": [
    {
      "id": "f3a1b1c2-1234-5678-9abc-def012345678",
      "name": "Team Alpha",
      "description": "Development team",
      "url": "https://dev.azure.com/fabrikam/_apis/projects/ProjectA/teams/f3a1b1c2-1234-5678-9abc-def012345678"
    },
    {
      "id": "a2b3c4d5-2345-6789-abcd-ef0123456789",
      "name": "Team Beta",
      "description": "QA team",
      "url": "https://dev.azure.com/fabrikam/_apis/projects/ProjectA/teams/a2b3c4d5-2345-6789-abcd-ef0123456789"
    }
  ]
}
```

#### Seguridad y permisos
- Scope requerido: `vso.project` o `vso.team`

---
### Get Avatar

Obtiene el avatar de un usuario, equipo o grupo.

**Endpoint:**
`GET https://vssps.dev.azure.com/{organization}/_apis/graph/avatars/{subjectDescriptor}?api-version=7.2-preview.1`

#### Parámetros de URI

| Nombre             | Ubicación | Requerido | Tipo   | Descripción                                                        |
|--------------------|-----------|-----------|--------|--------------------------------------------------------------------|
| organization       | path      | Sí        | string | Nombre de la organización de Azure DevOps                          |
| subjectDescriptor  | path      | Sí        | string | Descriptor del usuario, equipo o grupo                             |
| api-version        | query     | Sí        | string | Versión de la API. Usar '7.2-preview.1'                            |

#### Ejemplo de consumo con cURL

```bash
curl -X GET \
  'https://vssps.dev.azure.com/fabrikam/_apis/graph/avatars/vss.dscl.1234567890abcdef?api-version=7.2-preview.1' \
  -H 'Authorization: Basic <BASE64_PAT>' \
  -o avatar.png
```

> El resultado es un archivo binario (imagen). Usa `-o` para guardar la imagen.

#### Respuesta
- **Status code:** 200 (OK)
- **Content-Type:** image/png, image/jpeg, etc.

---

### Set Avatar

Actualiza el avatar de un usuario, equipo o grupo.

**Endpoint:**
`PUT https://vssps.dev.azure.com/{organization}/_apis/graph/avatars/{subjectDescriptor}?api-version=7.2-preview.1`

#### Parámetros de URI

| Nombre             | Ubicación | Requerido | Tipo   | Descripción                                                        |
|--------------------|-----------|-----------|--------|--------------------------------------------------------------------|
| organization       | path      | Sí        | string | Nombre de la organización de Azure DevOps                          |
| subjectDescriptor  | path      | Sí        | string | Descriptor del usuario, equipo o grupo                             |
| api-version        | query     | Sí        | string | Versión de la API. Usar '7.2-preview.1'                            |

#### Cuerpo de la petición (Request Body)
- Binario de la imagen (PNG, JPEG, etc.)

#### Ejemplo de consumo con cURL

```bash
curl -X PUT \
  'https://vssps.dev.azure.com/fabrikam/_apis/graph/avatars/vss.dscl.1234567890abcdef?api-version=7.2-preview.1' \
  -H 'Authorization: Basic <BASE64_PAT>' \
  -H 'Content-Type: image/png' \
  --data-binary '@nuevo_avatar.png'
```

#### Respuesta
- **Status code:** 200 (OK)
- **Content-Type:** application/json

```json
{
  "value": true
}
```

#### Seguridad y permisos
- Scope requerido: `vso.graph_manage`

---
  'https://dev.azure.com/{organization}/_apis/projects?api-version=7.2-preview.4' \
  -H 'Authorization: Basic <PAT>'
```

## Additional Resources
- [Official documentation](https://learn.microsoft.com/en-us/rest/api/azure/devops/core/?view=azure-devops-rest-7.2)
- [About projects and scaling your organization](https://learn.microsoft.com/en-us/azure/devops/organizations/projects/about-projects)
- [Create a project](https://learn.microsoft.com/en-us/azure/devops/organizations/projects/create-project)

---

This documentation covers the main operations and provides links to further details for each endpoint.
