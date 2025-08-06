---
# Columns

Documenta la API para consultar y modificar columnas de tableros.

## Endpoints principales

### Listar columnas de un tablero
```
GET https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/columns?api-version=7.2-preview.1
```
### Actualizar columnas de un tablero
```
PUT https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/columns?api-version=7.2-preview.1
```

## Parámetros comunes

| Nombre         | Ubicación | Requerido | Tipo   | Descripción |
|--------------- |-----------|-----------|--------|-------------|
| organization   | path      | Sí        | string | Nombre de la organización |
| project        | path      | Sí        | string | ID o nombre del proyecto |
| team           | path      | No        | string | ID o nombre del equipo |
| board          | path      | Sí        | string | ID o nombre del tablero |
| api-version    | query     | Sí        | string | Versión de la API (usar '7.2-preview.1') |

## Filtros y valores posibles

No hay filtros adicionales. El body de PUT es un array de objetos columna.

### Ejemplo de body para PUT
```json
[
  {
    "id": "<uuid>",
    "name": "New",
    "itemLimit": 0,
    "stateMappings": { "Product Backlog Item": "New", "Bug": "New" },
    "columnType": "incoming"
  }
]
```

## Ejemplos cURL

### Listar columnas
```bash
curl -X GET \
  -H "Authorization: Bearer <TOKEN>" \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/columns?api-version=7.2-preview.1"
```
### Actualizar columnas
```bash
curl -X PUT \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '[{ "id": "<uuid>", "name": "New", "itemLimit": 0, "stateMappings": { "Product Backlog Item": "New" }, "columnType": "incoming" }]' \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{board}/columns?api-version=7.2-preview.1"
```

## Definiciones

* **columnType**: incoming, inProgress, outgoing
* **stateMappings**: Objeto con mapeo de tipos de work item a estado

## Permisos requeridos

* Lectura: `vso.work`
* Escritura: `vso.work_write` (PUT)
