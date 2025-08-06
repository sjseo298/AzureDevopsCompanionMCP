---
# Predefined Queries

## Descripción
API para obtener y listar queries predefinidas en Azure DevOps Work. Permite consultar queries estándar como AssignedToMe, FollowedWorkItems, etc.

## Operaciones principales
- Obtener una query predefinida (incluyendo resultados)
- Listar queries conocidas

## Endpoints principales
```
GET https://dev.azure.com/{organization}/{project}/_apis/work/predefinedqueries/{query}?api-version=7.2-preview.1
GET https://dev.azure.com/{organization}/{project}/_apis/work/predefinedqueries?api-version=7.2-preview.1
```

## Parámetros
| Nombre        | En    | Tipo   | Requerido | Descripción                                 |
|-------------- |-------|--------|-----------|---------------------------------------------|
| organization  | path  | string | Sí        | Nombre de la organización Azure DevOps      |
| project       | path  | string | Sí        | Nombre o ID del proyecto                    |
| query         | path  | string | No*       | Nombre de la query predefinida (ver abajo)  |
| api-version   | query | string | Sí        | Versión de la API (`7.2-preview.1`)         |

\* Solo requerido para obtener una query específica.

## Ejemplo de consumo (cURL)
```bash
# Listar queries predefinidas
curl -X GET \
  -H "Accept: application/json" \
  -u :<PAT> \
  "https://dev.azure.com/{organization}/{project}/_apis/work/predefinedqueries?api-version=7.2-preview.1"

# Obtener resultados de una query específica
curl -X GET \
  -H "Accept: application/json" \
  -u :<PAT> \
  "https://dev.azure.com/{organization}/{project}/_apis/work/predefinedqueries/AssignedToMe?api-version=7.2-preview.1"
```

## Ejemplo de respuesta
```json
{
  "count": 2,
  "value": [
    {
      "id": "AssignedToMe",
      "name": "Assigned to me",
      "wiql": "SELECT ...",
      "results": [
        { "id": 123, "title": "Tarea 1", "state": "Active" },
        { "id": 456, "title": "Tarea 2", "state": "New" }
      ]
    },
    {
      "id": "FollowedWorkItems",
      "name": "Followed work items",
      "wiql": "SELECT ...",
      "results": []
    }
  ]
}
```

## Propiedades principales
- `id`: Identificador de la query predefinida (ej: AssignedToMe)
- `name`: Nombre legible de la query
- `wiql`: Consulta WIQL asociada
- `results`: Array de work items devueltos por la query

## Valores posibles para `query`
- `AssignedToMe`
- `FollowedWorkItems`
- `MyActivity`
- `RecentlyCreated`
- `RecentlyCompleted`
- `RecentlyUpdated`

## Filtros y propiedades adicionales
No existen filtros adicionales documentados para este endpoint. La respuesta incluye los resultados de las queries estándar.

## Referencias
- [Documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/predefined-queries?view=azure-devops-rest-7.2)

---
