
# Rows

API para gestionar las filas (rows) de un tablero Kanban en Azure DevOps Work.

## Operaciones principales
- Listar filas de un tablero
- Actualizar filas de un tablero

**Endpoints principales:**
```
GET    https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/rows?api-version=7.2-preview.1
PUT    https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/rows?api-version=7.2-preview.1
```

## Parámetros comunes
- organization (string, requerido)
- project (string, requerido)
- team (string, requerido)
- board (string, requerido)
- api-version (string, requerido, valor: 7.2-preview.1)

## Ejemplo de consumo (cURL)
```bash
# Listar filas de un tablero
curl -X GET \
  -H "Accept: application/json" \
  -u :<PAT> \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/rows?api-version=7.2-preview.1"

# Actualizar filas de un tablero
curl -X PUT \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -u :<PAT> \
  -d '[{"id": "41c6173f-13a2-42b8-ab75-d96eca02b0bc", "name": "Live Site"}]' \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/rows?api-version=7.2-preview.1"
```

## Filtros y propiedades
No existen filtros adicionales documentados para esta API.

## Respuesta
La respuesta incluye información sobre las filas del tablero:
```json
{
  "count": 3,
  "value": [
    { "id": "00000000-0000-0000-0000-000000000000", "name": null },
    { "id": "6dc43426-d087-4047-b0b7-44c03afed8df", "name": "Expedite" },
    { "id": "41c6173f-13a2-42b8-ab75-d96eca02b0bc", "name": "Live Site" }
  ]
}
```

## Definiciones
- BoardRow: { id: string (uuid), name: string, color: string }

## Permisos
- vso.work: Lectura de work items, queries, boards, etc.
- vso.work_write: Escritura y actualización de filas.

## Referencias
- [Documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/rows?view=azure-devops-rest-7.2)
