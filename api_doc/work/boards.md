---
# Boards

Documenta la API para consultar y modificar tableros de Azure DevOps.

## Endpoints

### Obtener un tablero

```
GET https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{id}?api-version=7.2-preview.1
```

#### Parámetros

| Nombre         | Ubicación | Requerido | Tipo   | Descripción |
|--------------- |-----------|-----------|--------|-------------|
| organization   | path      | Sí        | string | Nombre de la organización |
| project        | path      | Sí        | string | ID o nombre del proyecto |
| team           | path      | No        | string | ID o nombre del equipo |
| id             | path      | Sí        | string | ID o nombre del tablero (por backlog level o GUID) |
| api-version    | query     | Sí        | string | Versión de la API (usar '7.2-preview.1') |

#### Ejemplo cURL

```bash
curl -X GET \
  -H "Authorization: Bearer <TOKEN>" \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{id}?api-version=7.2-preview.1"
```

### Listar tableros

```
GET https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards?api-version=7.2-preview.1
```

#### Ejemplo cURL

```bash
curl -X GET \
  -H "Authorization: Bearer <TOKEN>" \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards?api-version=7.2-preview.1"
```

### Modificar opciones de tablero

```
PUT https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{id}?api-version=7.2-preview.1
```

#### Ejemplo cURL

```bash
curl -X PUT \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{ /* opciones a modificar */ }' \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/boards/{id}?api-version=7.2-preview.1"
```

## Filtros y valores posibles

No hay filtros adicionales, pero los IDs pueden ser nombre de backlog o GUID.

## Respuestas y definiciones

### Board

| Campo           | Tipo              | Descripción |
|-----------------|-------------------|-------------|
| id              | string (uuid)     | ID del tablero |
| name            | string            | Nombre del tablero |
| columns         | BoardColumn[]     | Columnas del tablero |
| rows            | BoardRow[]        | Filas del tablero |
| fields          | BoardFields       | Campos asociados |
| allowedMappings | object            | Mapeos permitidos |
| canEdit         | boolean           | Se puede editar |
| isValid         | boolean           | Es válido |
| revision        | integer           | Revisión |
| url             | string            | URL completa |

#### BoardColumn

| Campo        | Tipo    | Descripción |
|--------------|---------|-------------|
| id           | string  | ID columna  |
| name         | string  | Nombre      |
| columnType   | enum    | incoming, inProgress, outgoing |
| isSplit      | boolean | Split       |
| itemLimit    | int     | Límite      |
| stateMappings| object  | Mapeos      |

#### BoardRow

| Campo | Tipo   | Descripción |
|-------|--------|-------------|
| id    | string | ID de la fila |
| name  | string | Nombre de la fila |
| color | string | Color        |

#### BoardFields

| Campo        | Tipo           | Descripción |
|--------------|----------------|-------------|
| columnField  | FieldReference | Referencia a campo columna |
| doneField    | FieldReference | Referencia a campo done |
| rowField     | FieldReference | Referencia a campo fila |

#### FieldReference

| Campo         | Tipo   | Descripción |
|---------------|--------|-------------|
| referenceName | string | Nombre de referencia del campo |
| url           | string | URL a la definición del campo |

## Permisos requeridos

* Lectura: `vso.work`
* Escritura: `vso.work_write` (para PUT)
