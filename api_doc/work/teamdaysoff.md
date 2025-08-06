
# TeamDaysOff

API para gestionar los días libres (days off) de un equipo en una iteración de Azure DevOps Work.

## Operaciones principales
- Obtener días libres de un equipo para una iteración
- Actualizar días libres de un equipo para una iteración

**Endpoints principales:**
```
GET    https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings/iterations/{iterationId}/teamdaysoff?api-version=7.2-preview.1
PATCH  https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings/iterations/{iterationId}/teamdaysoff?api-version=7.2-preview.1
```

## Parámetros comunes
- organization (string, requerido)
- project (string, requerido)
- team (string, requerido)
- iterationId (string, requerido)
- api-version (string, requerido, valor: 7.2-preview.1)

## Ejemplo de consumo (cURL)
```bash
# Obtener días libres de un equipo para una iteración
curl -X GET \
  -H "Accept: application/json" \
  -u :<PAT> \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings/iterations/{iterationId}/teamdaysoff?api-version=7.2-preview.1"

# Actualizar días libres de un equipo para una iteración
curl -X PATCH \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -u :<PAT> \
  -d '{"daysOff": [{"start": "2025-08-10T00:00:00Z", "end": "2025-08-12T00:00:00Z"}]}' \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings/iterations/{iterationId}/teamdaysoff?api-version=7.2-preview.1"
```

## Filtros y propiedades
No existen filtros adicionales documentados para esta API.

## Respuesta
La respuesta incluye información sobre los días libres:
```json
{
  "daysOff": [
    { "start": "2025-08-10T00:00:00Z", "end": "2025-08-12T00:00:00Z" }
  ],
  "url": "https://dev.azure.com/{organization}/.../teamdaysoff",
  "_links": { ... }
}
```

## Definiciones
- DateRange: { start: string (date-time), end: string (date-time) }
- TeamSettingsDaysOff: { daysOff: DateRange[], url: string, _links: object }

## Permisos
- vso.work: Lectura de work items, queries, boards, etc.
- vso.work_write: Escritura y actualización de días libres.

## Referencias
- [Documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/teamdaysoff?view=azure-devops-rest-7.2)
