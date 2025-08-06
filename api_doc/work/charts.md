---
# Charts

Documenta la API para consultar y modificar gráficos de tableros.

## Endpoints principales

### Obtener un gráfico de tablero
```
GET https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/charts/{name}?api-version=7.2-preview.1
```
### Listar gráficos de tablero
```
GET https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/charts?api-version=7.2-preview.1
```
### Actualizar un gráfico de tablero
```
PATCH https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/charts/{name}?api-version=7.2-preview.1
```

## Parámetros comunes

| Nombre         | Ubicación | Requerido | Tipo   | Descripción |
|--------------- |-----------|-----------|--------|-------------|
| organization   | path      | Sí        | string | Nombre de la organización |
| project        | path      | Sí        | string | ID o nombre del proyecto |
| team           | path      | No        | string | ID o nombre del equipo |
| board          | path      | Sí        | string | ID o nombre del tablero |
| name           | path      | Solo para GET/PATCH | string | Nombre del gráfico |
| api-version    | query     | Sí        | string | Versión de la API (usar '7.2-preview.1') |

## Filtros y valores posibles

No hay filtros adicionales. El objeto `settings` en PATCH permite modificar la configuración del gráfico.

## Ejemplos cURL

### Obtener gráfico
```bash
curl -X GET \
  -H "Authorization: Bearer <TOKEN>" \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/charts/{name}?api-version=7.2-preview.1"
```
### Listar gráficos
```bash
curl -X GET \
  -H "Authorization: Bearer <TOKEN>" \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/charts?api-version=7.2-preview.1"
```
### Actualizar gráfico
```bash
curl -X PATCH \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{ "settings": { ... } }' \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/charts/{name}?api-version=7.2-preview.1"
```

## Definiciones

* **settings**: Objeto JSON con la configuración del gráfico.

## Permisos requeridos

* Lectura: `vso.work`
* Escritura: `vso.work_write` (PATCH)
