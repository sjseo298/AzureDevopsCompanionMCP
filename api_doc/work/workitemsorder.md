
# WorkItemsOrder

API para reordenar work items en el backlog de producto o en el backlog de iteración/sprint en Azure DevOps Work.

## Operaciones principales
- Reordenar work items en el backlog de producto/tablero
- Reordenar work items en el backlog de iteración/sprint

**Endpoints principales:**
```
PATCH  https://dev.azure.com/{organization}/{project}/{team}/_apis/work/workitemsorder?api-version=7.2-preview.1
PATCH  https://dev.azure.com/{organization}/{project}/{team}/_apis/work/iterations/{iterationId}/workitemsorder?api-version=7.2-preview.1
```

## Parámetros comunes
- organization (string, requerido)
- project (string, requerido)
- team (string, requerido)
- iterationId (string, requerido para reordenar en iteración)
- api-version (string, requerido, valor: 7.2-preview.1)

## Ejemplo de consumo (cURL)
```bash
# Reordenar work items en el backlog de producto/tablero
curl -X PATCH \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -u :<PAT> \
  -d '{"parentId": 0, "previousId": 4, "nextId": 5, "ids": [1,2,3]}' \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/workitemsorder?api-version=7.2-preview.1"

# Reordenar work items en el backlog de iteración/sprint
curl -X PATCH \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -u :<PAT> \
  -d '{"parentId": 1, "previousId": 12, "nextId": 13, "ids": [11]}' \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/iterations/{iterationId}/workitemsorder?api-version=7.2-preview.1"
```

## Filtros y propiedades
No existen filtros adicionales documentados para esta API.

## Respuesta
La respuesta incluye información sobre el nuevo orden de los work items:
```json
{
  "count": 3,
  "value": [
    { "id": 1, "order": 1000102770 },
    { "id": 2, "order": 1000110675 },
    { "id": 3, "order": 1000118580 }
  ]
}
```

## Definiciones
- ReorderOperation: { ids: int[], iterationPath: string, nextId: int, parentId: int, previousId: int }
- ReorderResult: { id: int, order: double }

## Permisos
- vso.work_write: Lectura, escritura y actualización de work items y queries, metadatos de tableros, áreas e iteraciones.

## Referencias
- [Documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/workitemsorder?view=azure-devops-rest-7.2)
