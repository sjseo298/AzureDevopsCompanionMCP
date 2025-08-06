---
# Iterations

**Documentación en progreso.**

## Descripción
API para gestionar iteraciones de equipo en Azure DevOps Work.

## Operaciones principales
- Eliminar una iteración de equipo por iterationId
- Obtener una iteración de equipo por iterationId
- Obtener work items de una iteración
- Listar iteraciones de un equipo usando filtro de timeframe
- Agregar una iteración al equipo

**Endpoints principales:**
```
GET    https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings/iterations?api-version=7.2-preview.1
POST   https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings/iterations?api-version=7.2-preview.1
GET    https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings/iterations/{iterationId}?api-version=7.2-preview.1
DELETE https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings/iterations/{iterationId}?api-version=7.2-preview.1
GET    https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings/iterations/{iterationId}/workitems?api-version=7.2-preview.1
```

## Parámetros comunes
- organization (string, requerido)
- project (string, requerido)
- team (string, requerido)
- iterationId (string, requerido para operaciones sobre una iteración específica)
- api-version (string, requerido, valor: 7.2-preview.1)
- timeframe (string, opcional, valores: past, current, future)

## Ejemplo de consumo (cURL)
```bash
# Listar iteraciones de un equipo
curl -X GET \
  -H "Accept: application/json" \
  -u :<PAT> \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings/iterations?api-version=7.2-preview.1"

# Obtener work items de una iteración
curl -X GET \
  -H "Accept: application/json" \
  -u :<PAT> \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings/iterations/{iterationId}/workitems?api-version=7.2-preview.1"
```

## Filtros y propiedades
- timeframe: permite filtrar por iteraciones pasadas, actuales o futuras. Valores posibles: `past`, `current`, `future`.

## Respuesta
La respuesta varía según la operación, incluyendo información de iteraciones, work items, etc.

## Referencias
- [Documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/iterations?view=azure-devops-rest-7.2)

