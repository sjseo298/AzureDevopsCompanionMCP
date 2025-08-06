---
# BoardUserSettings

Documenta la API para consultar y modificar la configuración de usuario de un tablero.

## Endpoints

### Obtener configuración de usuario

```
GET https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/boardusersettings?api-version=7.2-preview.1
```

#### Parámetros

| Nombre         | Ubicación | Requerido | Tipo   | Descripción |
|--------------- |-----------|-----------|--------|-------------|
| organization   | path      | Sí        | string | Nombre de la organización |
| project        | path      | Sí        | string | ID o nombre del proyecto |
| team           | path      | No        | string | ID o nombre del equipo |
| board          | path      | Sí        | string | ID o nombre del tablero |
| api-version    | query     | Sí        | string | Versión de la API (usar '7.2-preview.1') |

#### Ejemplo cURL

```bash
curl -X GET \
  -H "Authorization: Bearer <TOKEN>" \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/boardusersettings?api-version=7.2-preview.1"
```

### Modificar configuración de usuario

```
PATCH https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/boardusersettings?api-version=7.2-preview.1
```

#### Ejemplo cURL

```bash
curl -X PATCH \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{ "autoRefreshState": true }' \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/boardusersettings?api-version=7.2-preview.1"
```

## Filtros y valores posibles

| Campo             | Tipo    | Valores posibles |
|-------------------|---------|------------------|
| autoRefreshState  | boolean | true, false      |

## Respuesta de ejemplo

```json
{
  "autoRefreshState": true
}
```

## Permisos requeridos

* Lectura: `vso.work`
* Escritura: `vso.work_write` (para PATCH)
