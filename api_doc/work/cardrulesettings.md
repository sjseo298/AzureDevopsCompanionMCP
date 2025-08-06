---
# CardRuleSettings

Documenta la API para consultar y modificar las reglas de tarjetas de tableros y taskboards.

## Endpoints principales

### Obtener reglas de tarjetas de un tablero
```
GET https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/cardrulesettings?api-version=7.2-preview.2
```
### Actualizar reglas de tarjetas de un tablero
```
PATCH https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/cardrulesettings?api-version=7.2-preview.2
```
### Actualizar reglas de tarjetas de un taskboard
```
PATCH https://dev.azure.com/{organization}/{project}/{team}/_apis/work/taskboard/cardrulesettings?api-version=7.2-preview.2
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

El objeto `rules` es un JSON con las reglas de tarjetas. No hay filtros adicionales.

## Ejemplos cURL

### Obtener reglas de tarjetas de un tablero
```bash
curl -X GET \
  -H "Authorization: Bearer <TOKEN>" \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/cardrulesettings?api-version=7.2-preview.2"
```
### Actualizar reglas de tarjetas de un tablero
```bash
curl -X PATCH \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{ "rules": { ... } }' \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/cardrulesettings?api-version=7.2-preview.2"
```
### Actualizar reglas de tarjetas de un taskboard
```bash
curl -X PATCH \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{ "rules": { ... } }' \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/taskboard/cardrulesettings?api-version=7.2-preview.2"
```

## Definiciones

* **rules**: Objeto JSON con las reglas de tarjetas.

## Permisos requeridos

* Lectura: `vso.work`
* Escritura: `vso.work_write` (PATCH)
