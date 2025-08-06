---
# BoardRows

Documenta la API para obtener las filas disponibles en un tablero de Azure DevOps.

## Endpoint

```
GET https://dev.azure.com/{organization}/{project}/_apis/work/boardrows?api-version=7.2-preview.1
```

## Parámetros

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                 |
|--------------- |-----------|-----------|--------|---------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps    |
| project        | path      | Sí        | string | ID o nombre del proyecto                    |
| api-version    | query     | Sí        | string | Versión de la API (usar '7.2-preview.1')    |

## Filtros y valores posibles

No hay filtros adicionales ni valores especiales para este endpoint.

## Ejemplo de consumo con cURL

```bash
curl -X GET \
  -H "Authorization: Bearer <TOKEN>" \
  "https://dev.azure.com/{organization}/{project}/_apis/work/boardrows?api-version=7.2-preview.1"
```

## Respuesta de ejemplo

```json
{
  "count": 2,
  "value": [
    { "name": "Default" },
    { "name": "Expedite" }
  ]
}
```

## Definiciones

| Campo | Tipo   | Descripción |
|-------|--------|-------------|
| name  | string | Nombre de la fila |

## Permisos requeridos

`vso.work` (lectura de tableros y work items)
