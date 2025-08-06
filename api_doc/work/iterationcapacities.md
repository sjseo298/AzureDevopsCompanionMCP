---
# IterationCapacities

**Documentación en progreso.**

## Descripción
API para obtener la capacidad de iteración para todos los equipos en una iteración.

**Endpoint principal:**
```
GET https://dev.azure.com/{organization}/{project}/_apis/work/iterations/{iterationId}/capacities?api-version=7.2-preview.1
```

## Parámetros
- organization (string, requerido)
- project (string, requerido)
- iterationId (string, requerido)
- api-version (string, requerido, valor: 7.2-preview.1)

## Ejemplo de consumo (cURL)
```bash
curl -X GET \
  -H "Accept: application/json" \
  -u :<PAT> \
  "https://dev.azure.com/{organization}/{project}/_apis/work/iterations/{iterationId}/capacities?api-version=7.2-preview.1"
```

## Respuesta
La respuesta incluye la capacidad de cada miembro del equipo para la iteración especificada.

## Filtros y propiedades
No se documentan filtros adicionales en la referencia oficial. Consultar la [documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/iterationcapacities?view=azure-devops-rest-7.2) para detalles actualizados.

