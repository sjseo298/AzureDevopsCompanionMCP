
# Taskboard Work Items

API para gestionar los work items del tablero de tareas (Taskboard) en Azure DevOps Work.

## Operaciones principales
- Listar work items del tablero de tareas
- Actualizar work items del tablero de tareas

**Endpoints principales:**
```
GET    https://dev.azure.com/{organization}/{project}/{team}/_apis/work/taskboardworkitems?api-version=7.2-preview.1
PUT    https://dev.azure.com/{organization}/{project}/{team}/_apis/work/taskboardworkitems?api-version=7.2-preview.1
```

## Parámetros comunes
- organization (string, requerido)
- project (string, requerido)
- team (string, requerido)
- api-version (string, requerido, valor: 7.2-preview.1)

## Ejemplo de consumo (cURL)
```bash
# Listar work items del tablero de tareas
curl -X GET \
  -H "Accept: application/json" \
  -u :<PAT> \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/taskboardworkitems?api-version=7.2-preview.1"

# Actualizar work items del tablero de tareas
curl -X PUT \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -u :<PAT> \
  -d '[{"id": "123", "state": "In Progress"}]' \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/taskboardworkitems?api-version=7.2-preview.1"
```

## Filtros y propiedades
No existen filtros adicionales documentados para esta API.

## Respuesta
La respuesta incluye información sobre los work items del tablero:
```json
{
  "count": 2,
  "value": [
    { "id": "123", "state": "To Do" },
    { "id": "456", "state": "In Progress" }
  ]
}
```

## Definiciones
- TaskboardWorkItem: { id: string, state: string }

## Permisos
- vso.work: Lectura de work items, queries, boards, etc.
- vso.work_write: Escritura y actualización de work items.

## Referencias
- [Documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/taskboard-work-items?view=azure-devops-rest-7.2)
