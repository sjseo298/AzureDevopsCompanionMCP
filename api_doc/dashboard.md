# Azure DevOps Dashboard API Documentation (v7.2)

## Progreso de la documentación

| Sección / Operación                | Documentado | Notas |
|------------------------------------|:-----------:|-------|
| Dashboards - Create                |     ✅      |       |
| Dashboards - Delete                |     ✅      |       |
| Dashboards - Get                   |     ✅      |       |
| Dashboards - List                  |     ✅      |       |
| Dashboards - Replace Dashboard     |     ✅      |       |
| Dashboards - Replace Dashboards    |     ✅      |       |
| Widget Types - Get Widget Metadata |     ✅      |       |
| Widget Types - Get Widget Types    |     ✅      |       |
| Widgets - Create                   |     ✅      |       |
| Widgets - Delete                   |     ✅      |       |
| Widgets - Get Widget               |     ✅      |       |
| Widgets - Get Widget               |     ✅      |       |
| Widgets - Get Widgets              |     ✅      |       |
| Widgets - Replace Widget           |     ✅      |       |
| Widgets - Replace Widgets          |     ✅      |       |
| Widgets - Update Widget            |     ✅      |       |
| Widgets - Update Widgets           |     ✅      |       |

This section provides a comprehensive reference for the Azure DevOps Dashboard REST API (version 7.2). It covers all operations, parameters, and sub-sections, including cURL examples, input parameters, and request/response samples. Data versioning and concurrency handling are also explained.

## Overview

The Dashboard API allows you to manage team dashboards and their widgets in Azure DevOps. Each team can have multiple dashboards, and each dashboard contains a set of widgets. The API supports multi-user concurrency with eTag versioning for both dashboard lists and individual widgets.

- [Official documentation](https://learn.microsoft.com/en-us/rest/api/azure/devops/dashboard/?view=azure-devops-rest-7.2)
- [About dashboards, charts, reports, & widgets](https://learn.microsoft.com/en-us/azure/devops/report/dashboards/overview)
- [Widget catalog](https://learn.microsoft.com/en-us/azure/devops/report/dashboards/widget-catalog)

## Data Versioning

- Lists of widgets are versioned using the `eTag` header in the list APIs. You must retrieve the current `eTag` and provide it when updating the list.
- The widget contract also contains the `eTag` property, which versions the settings of a single widget. This version is separate from the list version and must be provided when updating a single widget.

## Main Operations



*Detailed documentation for each operation, including parameters, request/response examples, and property filters, will be added in the following sections.*
---

## Dashboards - Get

Obtiene un dashboard por su ID.

**Endpoint:**  
`GET https://dev.azure.com/{organization}/{project}/{team}/_apis/dashboard/dashboards/{dashboardId}?api-version=7.2-preview.3`

### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                               |
|----------------|-----------|-----------|--------|-----------------------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps                 |
| project        | path      | Sí        | string | ID o nombre del proyecto                                  |
| team           | path      | No        | string | ID o nombre del equipo                                    |
| dashboardId    | path      | Sí        | string (uuid) | ID del dashboard a obtener                         |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.3'                   |

### Ejemplo de consumo con cURL

```bash
curl -X GET \
  'https://dev.azure.com/fabrikam/MyProject/_apis/dashboard/dashboards/29653dd2-c18a-4d19-8333-e556c5b8d025?api-version=7.2-preview.3' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

### Ejemplo de respuesta

```json
{
  "id": "759b486a-e22a-4692-bf40-e2deec0de530",
  "name": "Overview",
  "refreshInterval": 0,
  "position": 1,
  "eTag": "2",
  "widgets": [ ... ]
}
```

### Seguridad y permisos

- Scope requerido: `vso.dashboards`

---

## Dashboards - List

Obtiene la lista de dashboards de un proyecto.

**Endpoint:**  
`GET https://dev.azure.com/{organization}/{project}/{team}/_apis/dashboard/dashboards?api-version=7.2-preview.3`

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                               |
|----------------|-----------|-----------|--------|-----------------------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps                 |
| project        | path      | Sí        | string | ID o nombre del proyecto                                  |
| team           | path      | No        | string | ID o nombre del equipo                                    |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.3'                   |

### Ejemplo de consumo con cURL

```bash
curl -X GET \
  'https://dev.azure.com/fabrikam/MyProject/_apis/dashboard/dashboards?api-version=7.2-preview.3' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

### Ejemplo de respuesta

```json
{
  "count": 2,
  "value": [
    {
      "id": "29653dd2-c18a-4d19-8333-e556c5b8d025",
      "name": "Overview",
      "refreshInterval": 0,
      "position": 1,
      "groupId": "091a34b6-9a63-40fd-acae-7716353dde05",
      "ownerId": "091a34b6-9a63-40fd-acae-7716353dde05",
      "dashboardScope": "project_Team",
      "url": "https://dev.azure.com/fabrikam/99755c45-cb33-4ab4-9f36-e5920cec7ba9/_apis/Dashboard/Dashboards/29653dd2-c18a-4d19-8333-e556c5b8d025"
    },
    {
      "id": "48ab8210-0463-48b7-aa0f-75d74fdfee95",
      "name": "p2",
      "refreshInterval": 0,
      "position": 2,
      "eTag": "1",
      "ownerId": "99c3b95a-7504-4652-8609-59af5637d920",
      "dashboardScope": "project",
      "url": "https://dev.azure.com/fabrikam/99755c45-cb33-4ab4-9f36-e5920cec7ba9/_apis/Dashboard/Dashboards/ad633947-f99b-4813-bfc6-b67b7e687b0e"
    }
  ]
}
```

### Seguridad y permisos

- Scope requerido: `vso.dashboards`

---

## Dashboards - Replace Dashboard

Reemplaza la configuración de un dashboard específico. Si se provee la propiedad widgets, reemplaza la lista de widgets.

`PUT https://dev.azure.com/{organization}/{project}/{team}/_apis/dashboard/dashboards/{dashboardId}?api-version=7.2-preview.3`

### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                               |
|----------------|-----------|-----------|--------|-----------------------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps                 |
| project        | path      | Sí        | string | ID o nombre del proyecto                                  |
| team           | path      | No        | string | ID o nombre del equipo                                    |
| dashboardId    | path      | Sí        | string (uuid) | ID del dashboard a reemplazar                      |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.3'                   |

### Cuerpo de la petición (Request Body)

Mismo formato que en la creación de dashboard (ver sección Create), incluyendo todos los campos posibles.

### Ejemplo de consumo con cURL

```bash
curl -X PUT \
  'https://dev.azure.com/fabrikam/MyProject/_apis/dashboard/dashboards/29653dd2-c18a-4d19-8333-e556c5b8d025?api-version=7.2-preview.3' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Basic <BASE64_PAT>' \
  -d '{
    "name": "Overview Updated",
    "widgets": [ ... ]
```

### Ejemplo de respuesta

```json
{
  "id": "29653dd2-c18a-4d19-8333-e556c5b8d025",
  "name": "Overview Updated",
  "position": 1,
  "eTag": "3",
  "widgets": [ ... ]
}
```

### Seguridad y permisos

- Scope requerido: `vso.dashboards_manage`

## Dashboards - Replace Dashboards

Actualiza el nombre y posición de los dashboards en el grupo suministrado y elimina los omitidos. No modifica el contenido de los dashboards.

**Endpoint:**  
`PUT https://dev.azure.com/{organization}/{project}/{team}/_apis/dashboard/dashboards?api-version=7.2-preview.3`

### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                               |
|----------------|-----------|-----------|--------|-----------------------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps                 |
| project        | path      | Sí        | string | ID o nombre del proyecto                                  |
| team           | path      | No        | string | ID o nombre del equipo                                    |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.3'                   |

### Cuerpo de la petición (Request Body)

| Campo                  | Tipo                       | Descripción                                                                 |
|------------------------|----------------------------|-----------------------------------------------------------------------------|
| dashboardEntries       | DashboardGroupEntry[]      | Lista de dashboards del grupo                                               |
| permission             | GroupMemberPermission      | (Obsoleto) Nivel de permisos del equipo                                     |
| teamDashboardPermission| TeamDashboardPermission    | Permisos de dashboard del equipo (bitmask)                                  |

#### DashboardGroupEntry
Incluye todos los campos de Dashboard (ver sección Create), más:
- dashboardScope: enum (project, project_Team, collection_User)

#### Valores posibles para permisos
- GroupMemberPermission: none, edit, manage, managePermissions
- TeamDashboardPermission: none, read, create, edit, delete, managePermissions

### Ejemplo de consumo con cURL

```bash
curl -X PUT \
  'https://dev.azure.com/fabrikam/MyProject/_apis/dashboard/dashboards?api-version=7.2-preview.3' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Basic <BASE64_PAT>' \
  -d '{
    "dashboardEntries": [ ... ]
  }'
```

### Ejemplo de respuesta

```json
{
  "dashboardEntries": [ ... ],
  "url": "https://dev.azure.com/fabrikam/MyProject/_apis/Dashboard/groups/999EF3B4-93E1-4086-86C2-5E8BD24512C7/Dashboards/7562562a-4f9f-4258-94a9-b048b1c817cf"
}
```

### Seguridad y permisos

- Scope requerido: `vso.dashboards_manage`

## Dashboards - Delete

Elimina un dashboard por su ID. También elimina los widgets asociados a ese dashboard.
**Endpoint:**  

### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                               |
|----------------|-----------|-----------|--------|-----------------------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps                 |
| project        | path      | Sí        | string | ID o nombre del proyecto                                  |
| team           | path      | No        | string | ID o nombre del equipo                                    |
| dashboardId    | path      | Sí        | string (uuid) | ID del dashboard a eliminar                        |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.3'                   |

### Ejemplo de consumo con cURL

```bash
curl -X DELETE \
  'https://dev.azure.com/fabrikam/Fabrikam-Fiber-Git/_apis/dashboard/dashboards/7562562a-4f9f-4258-94a9-b048b1c817cf?api-version=7.2-preview.3' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

> Reemplaza `<BASE64_PAT>` por tu token de acceso personal codificado en base64 (usuario:pat).

### Respuesta

- **Status code:** 204 (No Content)

### Seguridad y permisos

- Scope requerido: `vso.dashboards_manage`

---

# Dashboards

APIs para crear, listar, obtener, actualizar y eliminar dashboards de equipo.

## Operaciones principales

- **Create**: Crear un dashboard.
- **Delete**: Eliminar un dashboard por ID (también elimina sus widgets).
- **Get**: Obtener un dashboard por ID.
- **List**: Listar dashboards de un proyecto.
- **Replace Dashboard**: Reemplazar la configuración de un dashboard (incluye widgets si se proveen).
- **Replace Dashboards**: Actualizar nombre/posición de dashboards en grupo y eliminar los omitidos (no modifica contenido).

# Widget Types

APIs para consultar los tipos de widgets disponibles para dashboards.

## Operaciones principales

- **Get Widget Metadata**: Obtener metadatos de widget por contribution ID.
- **Get Widget Types**: Listar todos los tipos de widgets disponibles (incluye los ocultos en el catálogo).

# Widgets

APIs para agregar, obtener, actualizar y eliminar widgets en dashboards.

## Operaciones principales

- **Create**: Crear un widget en un dashboard.
- **Delete**: Eliminar un widget específico.
- **Get Widget**: Obtener el estado de un widget específico.
- **Get Widgets**: Listar widgets de un dashboard.
- **Replace Widget**: Sobrescribir el estado de un widget.
- **Replace Widgets**: Reemplazar todos los widgets de un dashboard.
- **Update Widget**: Actualizar parcialmente un widget.
- **Update Widgets**: Actualizar múltiples widgets en el dashboard (los no incluidos se preservan).

*En las siguientes secciones se documentará cada operación con sus parámetros, ejemplos de request/response y filtros avanzados.*

---

## Dashboards - Create

Crea un nuevo dashboard para un equipo o proyecto.

**Endpoint:**  
`POST https://dev.azure.com/{organization}/{project}/{team}/_apis/dashboard/dashboards?api-version=7.2-preview.3`

### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                               |
|----------------|-----------|-----------|--------|-----------------------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps                 |
| project        | path      | Sí        | string | ID o nombre del proyecto                                  |
| team           | path      | No        | string | ID o nombre del equipo                                    |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.3'                   |

### Cuerpo de la petición (Request Body)

| Campo                   | Tipo                | Descripción                                                                                   |
|-------------------------|---------------------|-----------------------------------------------------------------------------------------------|
| name                    | string              | Nombre del dashboard                                                                          |
| description             | string              | Descripción del dashboard                                                                     |
| position                | integer             | Posición dentro del grupo de dashboards                                                       |
| widgets                 | Widget[]            | Lista de widgets a incluir en el dashboard                                                    |
| dashboardScope          | enum                | project, project_Team, collection_User (DEPRECATED)                                           |
| groupId                 | string (uuid)       | ID del grupo (para dashboards de equipo)                                                      |
| ownerId                 | string (uuid)       | ID del propietario (equipo o usuario)                                                         |
| refreshInterval         | integer             | Intervalo de refresco automático (minutos)                                                    |
| globalParametersConfig  | string              | Configuración global de parámetros                                                            |
| eTag                    | string              | Valor de control de concurrencia (opcional, para edición concurrente)                         |

#### Widget (objeto dentro de widgets)

- name: string
- position: { row: int, column: int }
- size: { rowSpan: int, columnSpan: int }
- settings: string/null
- settingsVersion: { major: int, minor: int, patch: int }
- contributionId: string

### Ejemplo de petición

```bash
curl -X POST \
  'https://dev.azure.com/fabrikam/MyProject/_apis/dashboard/dashboards?api-version=7.2-preview.3' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Basic <BASE64_PAT>' \
  -d '{
    "name": "test",
    "position": 5,
    "widgets": [
      {
        "name": "Team Members renamed",
        "position": { "row": 2, "column": 2 },
        "size": { "rowSpan": 1, "columnSpan": 2 },
        "settings": null,
        "settingsVersion": { "major": 1, "minor": 0, "patch": 0 },
        "contributionId": "ms.vss-dashboards-web.Microsoft.VisualStudioOnline.Dashboards.TeamMembersWidget"
      }
    ]
  }'
```

> Reemplaza `<BASE64_PAT>` por tu token de acceso personal codificado en base64 (usuario:pat).

### Ejemplo de respuesta

```json
{
  "id": "29653dd2-c18a-4d19-8333-e556c5b8d025",
  "name": "test",
  "position": 5,
  "eTag": "1",
  "widgets": [
    {
      "name": "Team Members renamed",
      "position": { "row": 2, "column": 2 },
      "size": { "rowSpan": 1, "columnSpan": 2 },
      "settings": null,
      "settingsVersion": { "major": 1, "minor": 0, "patch": 0 },
      "contributionId": "ms.vss-dashboards-web.Microsoft.VisualStudioOnline.Dashboards.TeamMembersWidget"
    }
  ],
  "url": "https://dev.azure.com/fabrikam/99755c45-cb33-4ab4-9f36-e5920cec7ba9/dc0d32d0-be95-4385-9df6-1176d698be22/_apis/Dashboard/Dashboards/29653dd2-c18a-4d19-8333-e556c5b8d025"
}
```

### Valores posibles para dashboardScope

- `project_Team`: Dashboard asociado a un equipo
- `project`: Dashboard asociado al proyecto
- `collection_User`: (Obsoleto) Dashboard asociado a usuario de la colección

### Seguridad y permisos

- Scope requerido: `vso.dashboards_manage`

---

## Widgets - Create

Crea un widget en el dashboard especificado.

**Endpoint:**  
`POST https://dev.azure.com/{organization}/{project}/{team}/_apis/dashboard/dashboards/{dashboardId}/widgets?api-version=7.2-preview.2`

### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                               |
|----------------|-----------|-----------|--------|-----------------------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps                 |
| project        | path      | Sí        | string | ID o nombre del proyecto                                  |
| team           | path      | No        | string | ID o nombre del equipo                                    |
| dashboardId    | path      | Sí        | string (uuid) | ID del dashboard donde se agregará el widget        |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.2'                   |

### Cuerpo de la petición (Request Body)

| Campo                       | Tipo                | Descripción                                                                                   |
|-----------------------------|---------------------|-----------------------------------------------------------------------------------------------|
| name                        | string              | Nombre del widget                                                                             |
| position                    | WidgetPosition      | Posición del widget en el dashboard ({ row, column })                                         |
| size                        | WidgetSize          | Tamaño del widget ({ rowSpan, columnSpan })                                                   |
| settings                    | string/null         | Configuración específica del widget                                                           |
| settingsVersion             | SemanticVersion     | Versión de configuración (major, minor, patch)                                                |
| dashboard                   | Dashboard           | (Opcional) ETag del dashboard para control de concurrencia                                    |
| contributionId              | string              | Contribution ID del widget                                                                    |

#### WidgetPosition
- row: int
- column: int

#### WidgetSize
- rowSpan: int
- columnSpan: int

#### SemanticVersion
- major: int
- minor: int
- patch: int

### Ejemplo de consumo con cURL

```bash
curl -X POST \
  'https://dev.azure.com/fabrikam/MyProject/_apis/dashboard/dashboards/e17c2ae9-c35c-4ddd-9d4b-6aa47aa4d01a/widgets?api-version=7.2-preview.2' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Basic <BASE64_PAT>' \
  -d '{
    "name": "Other Links",
    "position": { "row": 1, "column": 1 },
    "size": { "rowSpan": 1, "columnSpan": 2 },
    "settings": null,
    "settingsVersion": { "major": 1, "minor": 0, "patch": 0 },
    "dashboard": { "eTag": "18" },
    "contributionId": "ms.vss-dashboards-web.Microsoft.VisualStudioOnline.Dashboards.OtherLinksWidget"
  }'
```

### Ejemplo de respuesta

```json
{
  "id": "69f6c5b7-0eb0-4067-b75f-6edff74d0fcf",
  "eTag": "1",
  "name": "Other Links",
  "position": { "row": 1, "column": 1 },
  "size": { "rowSpan": 1, "columnSpan": 2 },
  "settings": null,
  "settingsVersion": { "major": 1, "minor": 0, "patch": 0 },
  "dashboard": { "eTag": "19" },
  "contributionId": "ms.vss-dashboards-web.Microsoft.VisualStudioOnline.Dashboards.OtherLinksWidget",
  "url": "https://dev.azure.com/fabrikam/.../widgets/69f6c5b7-0eb0-4067-b75f-6edff74d0fcf"
}
```

### Seguridad y permisos

- Scope requerido: `vso.dashboards_manage`

---

## Widgets - Delete

Elimina un widget específico de un dashboard.

**Endpoint:**  
`DELETE https://dev.azure.com/{organization}/{project}/{team}/_apis/dashboard/dashboards/{dashboardId}/widgets/{widgetId}?api-version=7.2-preview.2`

### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                               |
|----------------|-----------|-----------|--------|-----------------------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps                 |
| project        | path      | Sí        | string | ID o nombre del proyecto                                  |
| team           | path      | No        | string | ID o nombre del equipo                                    |
| dashboardId    | path      | Sí        | string (uuid) | ID del dashboard que contiene el widget             |
| widgetId       | path      | Sí        | string (uuid) | ID del widget a eliminar                           |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.2'                   |

### Ejemplo de consumo con cURL

```bash
curl -X DELETE \
  'https://dev.azure.com/fabrikam/Fabrikam-Fiber-Git/_apis/dashboard/dashboards/7562562a-4f9f-4258-94a9-b048b1c817cf/widgets/f453fa65-6bcc-4534-9210-8c3cfa72cb1a?api-version=7.2-preview.2' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

### Respuesta

- **Status code:** 204 (No Content)

### Seguridad y permisos

- Scope requerido: `vso.dashboards_manage`

---

## Widgets - Get Widget (patrón estándar)

Obtiene el estado de un widget específico en un dashboard.

**Endpoint:**  
`GET https://dev.azure.com/{organization}/{project}/{team}/_apis/dashboard/dashboards/{dashboardId}/widgets/{widgetId}?api-version=7.2-preview.2`

### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                               |
|----------------|-----------|-----------|--------|-----------------------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps                 |
| project        | path      | Sí        | string | ID o nombre del proyecto                                  |
| team           | path      | No        | string | ID o nombre del equipo                                    |
| dashboardId    | path      | Sí        | string (uuid) | ID del dashboard que contiene el widget             |
| widgetId       | path      | Sí        | string (uuid) | ID del widget a consultar                           |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.2'                   |

### Ejemplo de consumo con cURL

```bash
curl -X GET \
  'https://dev.azure.com/fabrikam/MyProject/_apis/dashboard/dashboards/e17c2ae9-c35c-4ddd-9d4b-6aa47aa4d01a/widgets/69f6c5b7-0eb0-4067-b75f-6edff74d0fcf?api-version=7.2-preview.2' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

### Ejemplo de respuesta

```json
{
  "id": "69f6c5b7-0eb0-4067-b75f-6edff74d0fcf",
  "eTag": "1",
  "name": "Other Links",
  "position": { "row": 1, "column": 1 },
  "size": { "rowSpan": 1, "columnSpan": 2 },
  "settings": null,
  "settingsVersion": { "major": 1, "minor": 0, "patch": 0 },
  "dashboard": { "eTag": "19" },
  "contributionId": "ms.vss-dashboards-web.Microsoft.VisualStudioOnline.Dashboards.OtherLinksWidget",
  "url": "https://dev.azure.com/fabrikam/.../widgets/69f6c5b7-0eb0-4067-b75f-6edff74d0fcf"
}
```

### Seguridad y permisos

- Scope requerido: `vso.dashboards`

---

## Widgets - Get Widgets (patrón estándar)

Lista todos los widgets de un dashboard.

**Endpoint:**  
`GET https://dev.azure.com/{organization}/{project}/{team}/_apis/dashboard/dashboards/{dashboardId}/widgets?api-version=7.2-preview.2`

### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                               |
|----------------|-----------|-----------|--------|-----------------------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps                 |
| project        | path      | Sí        | string | ID o nombre del proyecto                                  |
| team           | path      | No        | string | ID o nombre del equipo                                    |
| dashboardId    | path      | Sí        | string (uuid) | ID del dashboard                                 |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.2'                   |

### Ejemplo de consumo con cURL

```bash
curl -X GET \
  'https://dev.azure.com/fabrikam/MyProject/_apis/dashboard/dashboards/e17c2ae9-c35c-4ddd-9d4b-6aa47aa4d01a/widgets?api-version=7.2-preview.2' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

### Ejemplo de respuesta

```json
{
  "count": 1,
  "value": [
    {
      "id": "69f6c5b7-0eb0-4067-b75f-6edff74d0fcf",
      "eTag": "1",
      "name": "Other Links",
      "position": { "row": 1, "column": 1 },
      "size": { "rowSpan": 1, "columnSpan": 2 },
      "settings": null,
      "settingsVersion": { "major": 1, "minor": 0, "patch": 0 },
      "dashboard": { "eTag": "19" },
      "contributionId": "ms.vss-dashboards-web.Microsoft.VisualStudioOnline.Dashboards.OtherLinksWidget",
      "url": "https://dev.azure.com/fabrikam/.../widgets/69f6c5b7-0eb0-4067-b75f-6edff74d0fcf"
    }
  ]
}
```

### Seguridad y permisos

- Scope requerido: `vso.dashboards`

---

## Widgets - Replace Widget (patrón estándar)

Sobrescribe el estado de un widget específico.

**Endpoint:**  
`PUT https://dev.azure.com/{organization}/{project}/{team}/_apis/dashboard/dashboards/{dashboardId}/widgets/{widgetId}?api-version=7.2-preview.2`

### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                               |
|----------------|-----------|-----------|--------|-----------------------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps                 |
| project        | path      | Sí        | string | ID o nombre del proyecto                                  |
| team           | path      | No        | string | ID o nombre del equipo                                    |
| dashboardId    | path      | Sí        | string (uuid) | ID del dashboard                                 |
| widgetId       | path      | Sí        | string (uuid) | ID del widget                                     |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.2'                   |

### Cuerpo de la petición (Request Body)

Mismo formato que en la creación de widget (ver sección Create), incluyendo todos los campos posibles.

### Ejemplo de consumo con cURL

```bash
curl -X PUT \
  'https://dev.azure.com/fabrikam/MyProject/_apis/dashboard/dashboards/e17c2ae9-c35c-4ddd-9d4b-6aa47aa4d01a/widgets/69f6c5b7-0eb0-4067-b75f-6edff74d0fcf?api-version=7.2-preview.2' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Basic <BASE64_PAT>' \
  -d '{
    "name": "Other Links Updated",
    "position": { "row": 1, "column": 1 },
    "size": { "rowSpan": 1, "columnSpan": 2 },
    "settings": null,
    "settingsVersion": { "major": 1, "minor": 0, "patch": 1 },
    "dashboard": { "eTag": "20" },
    "contributionId": "ms.vss-dashboards-web.Microsoft.VisualStudioOnline.Dashboards.OtherLinksWidget"
  }'
```

### Ejemplo de respuesta

```json
{
  "id": "69f6c5b7-0eb0-4067-b75f-6edff74d0fcf",
  "eTag": "2",
  "name": "Other Links Updated",
  "position": { "row": 1, "column": 1 },
  "size": { "rowSpan": 1, "columnSpan": 2 },
  "settings": null,
  "settingsVersion": { "major": 1, "minor": 0, "patch": 1 },
  "dashboard": { "eTag": "20" },
  "contributionId": "ms.vss-dashboards-web.Microsoft.VisualStudioOnline.Dashboards.OtherLinksWidget",
  "url": "https://dev.azure.com/fabrikam/.../widgets/69f6c5b7-0eb0-4067-b75f-6edff74d0fcf"
}
```

### Seguridad y permisos

- Scope requerido: `vso.dashboards_manage`

---

## Widgets - Replace Widgets (patrón estándar)

Reemplaza todos los widgets de un dashboard.

**Endpoint:**  
`PUT https://dev.azure.com/{organization}/{project}/{team}/_apis/dashboard/dashboards/{dashboardId}/widgets?api-version=7.2-preview.2`

### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                               |
|----------------|-----------|-----------|--------|-----------------------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps                 |
| project        | path      | Sí        | string | ID o nombre del proyecto                                  |
| team           | path      | No        | string | ID o nombre del equipo                                    |
| dashboardId    | path      | Sí        | string (uuid) | ID del dashboard                                 |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.2'                   |

### Cuerpo de la petición (Request Body)

Array de objetos Widget (ver sección Create).

### Ejemplo de consumo con cURL

```bash
curl -X PUT \
  'https://dev.azure.com/fabrikam/MyProject/_apis/dashboard/dashboards/e17c2ae9-c35c-4ddd-9d4b-6aa47aa4d01a/widgets?api-version=7.2-preview.2' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Basic <BASE64_PAT>' \
  -d '[
    {
      "name": "Other Links",
      "position": { "row": 1, "column": 1 },
      "size": { "rowSpan": 1, "columnSpan": 2 },
      "settings": null,
      "settingsVersion": { "major": 1, "minor": 0, "patch": 0 },
      "dashboard": { "eTag": "19" },
      "contributionId": "ms.vss-dashboards-web.Microsoft.VisualStudioOnline.Dashboards.OtherLinksWidget"
    }
  ]'
```

### Ejemplo de respuesta

```json
[
  {
    "id": "69f6c5b7-0eb0-4067-b75f-6edff74d0fcf",
    "eTag": "2",
    "name": "Other Links",
    "position": { "row": 1, "column": 1 },
    "size": { "rowSpan": 1, "columnSpan": 2 },
    "settings": null,
    "settingsVersion": { "major": 1, "minor": 0, "patch": 0 },
    "dashboard": { "eTag": "19" },
    "contributionId": "ms.vss-dashboards-web.Microsoft.VisualStudioOnline.Dashboards.OtherLinksWidget",
    "url": "https://dev.azure.com/fabrikam/.../widgets/69f6c5b7-0eb0-4067-b75f-6edff74d0fcf"
  }
]
```

### Seguridad y permisos

- Scope requerido: `vso.dashboards_manage`

---

## Widgets - Update Widget (patrón estándar)

Actualiza parcialmente un widget específico.

**Endpoint:**  
`PATCH https://dev.azure.com/{organization}/{project}/{team}/_apis/dashboard/dashboards/{dashboardId}/widgets/{widgetId}?api-version=7.2-preview.2`

### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                               |
|----------------|-----------|-----------|--------|-----------------------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps                 |
| project        | path      | Sí        | string | ID o nombre del proyecto                                  |
| team           | path      | No        | string | ID o nombre del equipo                                    |
| dashboardId    | path      | Sí        | string (uuid) | ID del dashboard                                 |
| widgetId       | path      | Sí        | string (uuid) | ID del widget                                     |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.2'                   |

### Cuerpo de la petición (Request Body)

Solo los campos a actualizar (por ejemplo, settings, position, size, etc).

### Ejemplo de consumo con cURL

```bash
curl -X PATCH \
  'https://dev.azure.com/fabrikam/MyProject/_apis/dashboard/dashboards/e17c2ae9-c35c-4ddd-9d4b-6aa47aa4d01a/widgets/69f6c5b7-0eb0-4067-b75f-6edff74d0fcf?api-version=7.2-preview.2' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Basic <BASE64_PAT>' \
  -d '{
    "settings": "{\"title\":\"Nuevo título\"}"
  }'
```

### Ejemplo de respuesta

```json
{
  "id": "69f6c5b7-0eb0-4067-b75f-6edff74d0fcf",
  "eTag": "3",
  "name": "Other Links",
  "position": { "row": 1, "column": 1 },
  "size": { "rowSpan": 1, "columnSpan": 2 },
  "settings": "{\"title\":\"Nuevo título\"}",
  "settingsVersion": { "major": 1, "minor": 0, "patch": 2 },
  "dashboard": { "eTag": "21" },
  "contributionId": "ms.vss-dashboards-web.Microsoft.VisualStudioOnline.Dashboards.OtherLinksWidget",
  "url": "https://dev.azure.com/fabrikam/.../widgets/69f6c5b7-0eb0-4067-b75f-6edff74d0fcf"
}
```

### Seguridad y permisos

- Scope requerido: `vso.dashboards_manage`

---

## Widgets - Update Widgets (patrón estándar)

Actualiza múltiples widgets en el dashboard (los no incluidos se preservan).

**Endpoint:**  
`PATCH https://dev.azure.com/{organization}/{project}/{team}/_apis/dashboard/dashboards/{dashboardId}/widgets?api-version=7.2-preview.2`

### Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                               |
|----------------|-----------|-----------|--------|-----------------------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps                 |
| project        | path      | Sí        | string | ID o nombre del proyecto                                  |
| team           | path      | No        | string | ID o nombre del equipo                                    |
| dashboardId    | path      | Sí        | string (uuid) | ID del dashboard                                 |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.2'                   |

### Cuerpo de la petición (Request Body)

Array de objetos Widget con los campos a actualizar.

### Ejemplo de consumo con cURL

```bash
curl -X PATCH \
  'https://dev.azure.com/fabrikam/MyProject/_apis/dashboard/dashboards/e17c2ae9-c35c-4ddd-9d4b-6aa47aa4d01a/widgets?api-version=7.2-preview.2' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Basic <BASE64_PAT>' \
  -d '[
    {
      "id": "69f6c5b7-0eb0-4067-b75f-6edff74d0fcf",
      "settings": "{\"title\":\"Nuevo título\"}"
    }
  ]'
```

### Ejemplo de respuesta

```json
[
  {
    "id": "69f6c5b7-0eb0-4067-b75f-6edff74d0fcf",
    "eTag": "4",
    "name": "Other Links",
    "position": { "row": 1, "column": 1 },
    "size": { "rowSpan": 1, "columnSpan": 2 },
    "settings": "{\"title\":\"Nuevo título\"}",
    "settingsVersion": { "major": 1, "minor": 0, "patch": 3 },
    "dashboard": { "eTag": "22" },
    "contributionId": "ms.vss-dashboards-web.Microsoft.VisualStudioOnline.Dashboards.OtherLinksWidget",
    "url": "https://dev.azure.com/fabrikam/.../widgets/69f6c5b7-0eb0-4067-b75f-6edff74d0fcf"
  }
]
```

### Seguridad y permisos

- Scope requerido: `vso.dashboards_manage`

---
