# Work Items

Permite crear, consultar, actualizar y eliminar work items en Azure DevOps. Es la API principal para gestionar elementos de trabajo, soportando operaciones individuales y por lotes.

**Documentación oficial:** [Work Items](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/work-items?view=azure-devops-rest-7.2)

## Operaciones principales

| Operación              | Endpoint                                                                                                 | Método |
|------------------------|--------------------------------------------------------------------------------------------------------|--------|
| Create                 | /{organization}/{project}/_apis/wit/workitems/${type}?api-version=7.2-preview                            | POST   |
| Get Work Item          | /{organization}/{project}/_apis/wit/workitems/{id}?api-version=7.2-preview                               | GET    |
| Get Work Items Batch   | /{organization}/{project}/_apis/wit/workitemsbatch?api-version=7.2-preview                               | POST   |
| List                   | /{organization}/{project}/_apis/wit/workitems?ids={ids}&api-version=7.2-preview                          | GET    |
| Update                 | /{organization}/{project}/_apis/wit/workitems/{id}?api-version=7.2-preview                               | PATCH  |
| Delete                 | /{organization}/{project}/_apis/wit/workitems/{id}?api-version=7.2-preview                               | DELETE |
| Delete Work Items      | /{organization}/{project}/_apis/wit/workitems?ids={ids}&api-version=7.2-preview                          | DELETE |
| Get Work Item Template | /{organization}/{project}/_apis/wit/workitems/{id}/templates/{template}?api-version=7.2-preview          | GET    |

Notas verificación (2025-08-09): La documentación oficial (API 7.2-preview) mantiene estos endpoints. Se confirman parámetros opcionales adicionales (ver tablas siguientes) que no estaban listados inicialmente.

## Parámetros de URI y Query (consolidados)

| Nombre         | Ubicación     | Requerido                               | Tipo        | Aplica a                                  | Descripción |
|----------------|---------------|------------------------------------------|-------------|--------------------------------------------|-------------|
| organization   | path          | Sí                                       | string      | Todos                                      | Organización Azure DevOps |
| project        | path          | Sí                                       | string      | Todos                                      | Nombre o ID del proyecto |
| id             | path          | Sí (Get/Update/Delete individual)        | int         | Get/Update/Delete/Template                 | ID del work item |
| ids            | query/body    | Sí (List / Delete Work Items / Batch)    | lista(int)  | List (query), Delete Work Items (query), Work Items Batch (body) | IDs (máx 200 en List/Batch) |
| type           | path          | Sí (Create)                              | string      | Create                                    | Tipo (ej: Bug, 'User Story'); embebido como $Tipo en URL |
| api-version    | query         | Sí                                       | string      | Todos                                      | '7.2-preview' (actual) |
| fields         | query/body    | No                                       | lista(string)| Get/List/Batch (query o body en batch)     | Limita campos retornados |
| asOf           | query/body    | No                                       | datetime    | Get/List/Batch                            | Estado a fecha/hora específica |
| expand         | query         | No                                       | enum        | Get/List                                  | None, Relations, Fields, Links, All |
| errorPolicy    | body          | No (Batch)                               | enum        | Batch (POST workitemsbatch)               | Omit o Fail (comportamiento ante errores) |
| destroy        | query         | No                                       | bool        | Delete (individual / múltiple)            | true = eliminación permanente (salta papelera) |
| validateOnly   | query         | No                                       | bool        | Create / Update                           | Valida sin persistir (dry-run) |
| bypassRules    | query         | No                                       | bool        | Create / Update / Batch                   | Ignora reglas del proceso (restringido) |
| suppressNotifications | query  | No                                      | bool        | Create / Update                           | No envía notificaciones |
| template       | path          | Sí (Get Work Item Template)             | string      | Get Work Item Template                    | Nombre/ID de la plantilla |

Observación: Para operaciones Batch (`workitemsbatch`) los parámetros (ids, fields, asOf, errorPolicy) van en el cuerpo JSON.

## Ejemplo de consumo con cURL (Crear)

```bash
curl -X POST \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/workitems/$Bug?api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>' \
  -H 'Content-Type: application/json-patch+json' \
  -d '[{"op": "add", "path": "/fields/System.Title", "value": "Nuevo bug"}]'
```

## Ejemplo de consumo con cURL (Consultar)

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/workitems/123?api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

## Ejemplo de respuesta (Get Work Item)

```json
{
  "id": 123,
  "rev": 1,
  "fields": {
    "System.Title": "Nuevo bug",
    "System.State": "New"
  },
  "url": "https://dev.azure.com/org/proj/_apis/wit/workitems/123"
}
```

## Filtros y propiedades a incluir

  - `id`: identificador del work item (int)
  - `fields`: objeto con los campos y valores del work item
  - `rev`: número de revisión (int)
  - `url`: URL del recurso (string)
  - `relations`: (si expand=Relations o All) enlaces a otros artefactos
  - `_links`: (si expand=Links o All) hipervínculos enriquecidos (self, updates, html)

### Notas sobre `expand`
| Valor      | Incluye adicionalmente |
|------------|------------------------|
| None       | Solo campos solicitados |
| Relations  | + relations            |
| Fields     | (Histórico de campo? - ya vienen en None; Fields útil en escenarios de batch) |
| Links      | + _links               |
| All        | + relations + _links   |

### Políticas de Error (Batch)
| errorPolicy | Comportamiento |
|-------------|---------------|
| Omit        | Omite items con error, retorna el resto |
| Fail        | Aborta toda la operación al primer error |

### Operaciones JSON Patch (Create / Update)
Content-Type: `application/json-patch+json`

Cada elemento: `{ "op": "add|replace|remove|test|move|copy", "path": "/fields/System.Title", "value": "Nuevo título" }`

Ejemplo Update:
```json
[
  {"op":"add","path":"/fields/System.Title","value":"Título actualizado"},
  {"op":"replace","path":"/fields/System.State","value":"Active"}
]
```

### Eliminación permanente
Usar `?destroy=true` en Delete (individual o múltiple) para saltar la papelera. Requiere permisos elevados; no reversible.

## Notas

- Permite gestionar el ciclo de vida completo de los work items.
- El scope mínimo requerido es `vso.work` (lectura y escritura de work items).
- Para minimizar payload, siempre especificar `fields` cuando solo se necesitan campos clave (ej: System.Id,System.Title,System.State).
- Límite de IDs en List / Batch: 200.
- Recomendado usar Batch para escenarios de latencia y limitar campos.
