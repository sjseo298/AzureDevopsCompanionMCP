---
# Capacities

Documenta la API para consultar y modificar la capacidad de equipos y miembros en Azure DevOps.

## Endpoints principales

### Obtener la capacidad de un equipo (con totales)
```
GET https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings/iterations/{iterationId}/capacities?api-version=7.2-preview.3
```
### Obtener la capacidad de un miembro
```
GET https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings/iterations/{iterationId}/capacities/{teamMemberId}?api-version=7.2-preview.3
```
### Reemplazar la capacidad de un equipo
```
PUT https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings/iterations/{iterationId}/capacities?api-version=7.2-preview.3
```
### Actualizar la capacidad de un miembro
```
PATCH https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings/iterations/{iterationId}/capacities/{teamMemberId}?api-version=7.2-preview.3
```

## Parámetros comunes

| Nombre         | Ubicación | Requerido | Tipo   | Descripción |
|--------------- |-----------|-----------|--------|-------------|
| organization   | path      | Sí        | string | Nombre de la organización |
| project        | path      | Sí        | string | ID o nombre del proyecto |
| team           | path      | No        | string | ID o nombre del equipo |
| iterationId    | path      | Sí        | string | ID de la iteración (uuid) |
| teamMemberId   | path      | Solo para miembro | string | ID del miembro (uuid) |
| api-version    | query     | Sí        | string | Versión de la API (usar '7.2-preview.3') |

## Filtros y valores posibles

No hay filtros adicionales, pero los cuerpos de las peticiones permiten definir actividades y días libres.

### Ejemplo de body para PUT/PATCH
```json
{
  "activities": [
    { "capacityPerDay": 5, "name": "Design" },
    { "capacityPerDay": 5, "name": "Development" }
  ],
  "daysOff": [
    { "start": "2025-08-01T00:00:00Z", "end": "2025-08-05T00:00:00Z" }
  ]
}
```

## Ejemplos cURL

### Obtener capacidad de equipo
```bash
curl -X GET \
  -H "Authorization: Bearer <TOKEN>" \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings/iterations/{iterationId}/capacities?api-version=7.2-preview.3"
```
### Obtener capacidad de miembro
```bash
curl -X GET \
  -H "Authorization: Bearer <TOKEN>" \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings/iterations/{iterationId}/capacities/{teamMemberId}?api-version=7.2-preview.3"
```
### Reemplazar capacidad de equipo
```bash
curl -X PUT \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '[{ "teamMember": {"id": "<teamMemberId>"}, "activities": [...], "daysOff": [...] }]' \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings/iterations/{iterationId}/capacities?api-version=7.2-preview.3"
```
### Actualizar capacidad de miembro
```bash
curl -X PATCH \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{ "activities": [...], "daysOff": [...] }' \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings/iterations/{iterationId}/capacities/{teamMemberId}?api-version=7.2-preview.3"
```

## Definiciones

* **activities**: Lista de actividades con capacidad diaria (`capacityPerDay`, `name`)
* **daysOff**: Rango de fechas (`start`, `end`)
* **teamMember**: Objeto de referencia a miembro

## Permisos requeridos

* Lectura: `vso.work`
* Escritura: `vso.work_write` (PUT/PATCH)
