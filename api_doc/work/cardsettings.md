---
# CardSettings

Documenta la API para consultar y modificar la configuración de tarjetas de tableros y taskboards.

## Endpoints principales

### Obtener configuración de tarjetas de un tablero
```
GET https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/cardsettings?api-version=7.2-preview.2
```
### Actualizar configuración de tarjetas de un tablero
```
PUT https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/cardsettings?api-version=7.2-preview.2
```
### Actualizar configuración de tarjetas de un taskboard
```
PUT https://dev.azure.com/{organization}/{project}/{team}/_apis/work/taskboard/cardsettings?api-version=7.2-preview.2
```

## Parámetros comunes

| Nombre         | Ubicación | Requerido | Tipo   | Descripción |
|--------------- |-----------|-----------|--------|-------------|
| organization   | path      | Sí        | string | Nombre de la organización |
| project        | path      | Sí        | string | ID o nombre del proyecto |
| team           | path      | Sí        | string | ID o nombre del equipo |
| board          | path      | Solo para board | string | ID o nombre del tablero |
| api-version    | query     | Sí        | string | Versión de la API (usar '7.2-preview.2') |

## Filtros y valores posibles

El objeto `cards` es un JSON con la configuración de tarjetas. No hay filtros adicionales.

## Ejemplos cURL

### Obtener configuración de tarjetas de un tablero
```bash
curl -X GET \
  -H "Authorization: Bearer <TOKEN>" \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/cardsettings?api-version=7.2-preview.2"
```
### Actualizar configuración de tarjetas de un tablero
```bash
curl -X PUT \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{ "cards": { ... } }' \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/cardsettings?api-version=7.2-preview.2"
```
### Actualizar configuración de tarjetas de un taskboard
```bash
curl -X PUT \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{ "cards": { ... } }' \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/taskboard/cardsettings?api-version=7.2-preview.2"
```

## Definiciones

* **cards**: Objeto JSON con la configuración de tarjetas.

## Permisos requeridos

* Lectura: `vso.work`
* Escritura: `vso.work_write` (PUT)
