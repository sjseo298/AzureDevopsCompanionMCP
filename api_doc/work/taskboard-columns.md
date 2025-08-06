
# Taskboard Columns

API para gestionar las columnas del tablero de tareas (Taskboard) en Azure DevOps Work.

## Operaciones principales
- Obtener columnas del tablero de tareas
- Actualizar columnas del tablero de tareas

**Endpoints principales:**
```
GET    https://dev.azure.com/{organization}/{project}/{team}/_apis/work/taskboardcolumns?api-version=7.2-preview.1
PUT    https://dev.azure.com/{organization}/{project}/{team}/_apis/work/taskboardcolumns?api-version=7.2-preview.1
```

## Parámetros comunes
- organization (string, requerido)
- project (string, requerido)
- team (string, requerido)
- api-version (string, requerido, valor: 7.2-preview.1)

## Ejemplo de consumo (cURL)
```bash
# Obtener columnas del tablero de tareas
curl -X GET \
  -H "Accept: application/json" \
  -u :<PAT> \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/taskboardcolumns?api-version=7.2-preview.1"

# Actualizar columnas del tablero de tareas
curl -X PUT \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -u :<PAT> \
  -d '[{"name": "New Column", "order": 1}]' \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/taskboardcolumns?api-version=7.2-preview.1"
```

## Filtros y propiedades
No existen filtros adicionales documentados para esta API.

## Respuesta
La respuesta incluye información sobre las columnas del tablero:
```json
{
  "columns": [
    { "id": "uuid", "name": "To Do", "order": 1, "mappings": [] },
    { "id": "uuid", "name": "In Progress", "order": 2, "mappings": [] }
  ],
  "isCustomized": true,
  "isValid": true,
  "validationMesssage": ""
}
```

## Definiciones
- TaskboardColumn: { id: string (uuid), name: string, order: int, mappings: array }
- TaskboardColumns: { columns: array, isCustomized: bool, isValid: bool, validationMesssage: string }

## Permisos
- vso.work: Lectura de work items, queries, boards, etc.
- vso.work_write: Escritura y actualización de columnas.

## Referencias
- [Documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/taskboard-columns?view=azure-devops-rest-7.2)
