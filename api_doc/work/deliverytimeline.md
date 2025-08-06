---
# DeliveryTimeline

## Descripción
API para obtener la vista de entrega (Delivery View Data) en Azure DevOps Work.

**Endpoint principal:**
```
GET https://dev.azure.com/{organization}/{project}/{team}/_apis/work/deliverytimeline?api-version=7.2-preview.1
```

## Parámetros
- organization (string, requerido)
- project (string, requerido)
- team (string, requerido)
- api-version (string, requerido, valor: 7.2-preview.1)

## Ejemplo de consumo (cURL)
```bash
curl -X GET \
  -H "Accept: application/json" \
  -u :<PAT> \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/deliverytimeline?api-version=7.2-preview.1"
```

## Respuesta
La respuesta incluye información sobre la vista de entrega, equipos, iteraciones y work items.

## Filtros y propiedades
No se documentan filtros adicionales en la referencia oficial. Consultar la [documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/deliverytimeline?view=azure-devops-rest-7.2) para detalles actualizados.

