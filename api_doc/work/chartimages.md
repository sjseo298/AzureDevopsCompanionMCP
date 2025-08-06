---
# ChartImages

Documenta la API para obtener imágenes de gráficos de tableros e iteraciones.

## Endpoints principales

### Obtener imagen de gráfico de tablero
```
GET https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/chartimages/{name}?api-version=7.2-preview.1
```
### Obtener imagen de gráfico de iteración
```
GET https://dev.azure.com/{organization}/{project}/{team}/_apis/work/iterations/{iterationId}/chartimages/{name}?api-version=7.2-preview.1
```
### Obtener imagen de gráfico de múltiples iteraciones
```
GET https://dev.azure.com/{organization}/{project}/{team}/_apis/work/iterations/chartimages/{name}?api-version=7.2-preview.1
```

## Parámetros comunes

| Nombre         | Ubicación | Requerido | Tipo   | Descripción |
|--------------- |-----------|-----------|--------|-------------|
| organization   | path      | Sí        | string | Nombre de la organización |
| project        | path      | Sí        | string | ID o nombre del proyecto |
| team           | path      | Sí        | string | ID o nombre del equipo |
| board          | path      | Solo para board | string | ID o nombre del tablero |
| iterationId    | path      | Solo para iteración | string | ID de la iteración |
| name           | path      | Sí        | string | Nombre del gráfico (ej: CumulativeFlow, Burndown, Velocity) |
| api-version    | query     | Sí        | string | Versión de la API (usar '7.2-preview.1') |
| width          | query     | No        | int    | Ancho en píxeles (>0) |
| height         | query     | No        | int    | Alto en píxeles (>0) |
| showDetails    | query     | No        | bool   | Mostrar detalles (ejes, títulos, etc) |
| title          | query     | No        | string | Título del gráfico |
| iterationsNumber| query    | Solo para iteraciones | int | Número de iteraciones |

## Filtros y valores posibles

No hay filtros adicionales. Los nombres de gráficos válidos dependen del tipo de tablero/iteración.

## Ejemplo cURL

```bash
curl -X GET \
  -H "Authorization: Bearer <TOKEN>" \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/chartimages/{name}?width=800&height=400&showDetails=true&title=MiGrafico&api-version=7.2-preview.1"
```

## Permisos requeridos

* Lectura: `vso.work`
