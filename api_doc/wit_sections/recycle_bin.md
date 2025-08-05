---

# Recycle Bin

Permite listar, obtener, restaurar y eliminar permanentemente work items que han sido enviados a la papelera de reciclaje. Todas las operaciones requieren permisos de escritura sobre work items.

**Documentación oficial:** [Recycle Bin](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/recyclebin?view=azure-devops-rest-7.2)

## Operaciones principales

| Operación                                 | Endpoint                                                                                                 | Método |
|-------------------------------------------|----------------------------------------------------------------------------------------------------------|--------|
| Destroy Work Item (eliminación permanente)| /{organization}/{project}/_apis/wit/recyclebin/{id}?api-version=7.2-preview                              | DELETE |
| Get Deleted Work Item                     | /{organization}/{project}/_apis/wit/recyclebin/{id}?api-version=7.2-preview                              | GET    |
| Get Deleted Work Items                    | /{organization}/{project}/_apis/wit/recyclebin?ids={ids}&api-version=7.2-preview                         | GET    |
| Get Deleted Work Item Shallow References  | /{organization}/{project}/_apis/wit/recyclebin?api-version=7.2-preview                                   | GET    |
| Restore Work Item                         | /{organization}/{project}/_apis/wit/recyclebin/{id}?api-version=7.2-preview                              | PATCH  |

---

## Parámetros de URI comunes

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                 |
|----------------|-----------|-----------|--------|---------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps   |
| project        | path      | Sí        | string | Nombre o ID del proyecto                    |
| id             | path      | Sí (GET, PATCH, DELETE) | int | ID del work item eliminado                 |
| ids            | query     | No (solo para batch GET) | string | Lista de IDs separados por coma            |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview'       |

---

## Ejemplos de uso

### Destroy Work Item (eliminación permanente)

**Endpoint:**
```
DELETE /{organization}/{project}/_apis/wit/recyclebin/{id}?api-version=7.2-preview
```

**Ejemplo cURL:**
```bash
curl -X DELETE \
  -H "Authorization: Basic <PAT>" \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/recyclebin/12345?api-version=7.2-preview'
```

**Respuesta:**
Status 204 No Content (eliminado permanentemente)

---

### Get Deleted Work Item

**Endpoint:**
```
GET /{organization}/{project}/_apis/wit/recyclebin/{id}?api-version=7.2-preview
```

**Ejemplo cURL:**
```bash
curl -X GET \
  -H "Authorization: Basic <PAT>" \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/recyclebin/12345?api-version=7.2-preview'
```

**Ejemplo de respuesta:**
```json
{
  "id": 12345,
  "name": "Bug: Error en login",
  "deletedDate": "2025-08-01T12:34:56.789Z",
  "deletedBy": {
    "id": "a1b2c3d4-5678-90ab-cdef-1234567890ab",
    "displayName": "Juan Pérez"
  },
  "url": "https://dev.azure.com/org/proj/_apis/wit/recyclebin/12345"
}
```

---

### Get Deleted Work Items (batch)

**Endpoint:**
```
GET /{organization}/{project}/_apis/wit/recyclebin?ids={ids}&api-version=7.2-preview
```

**Ejemplo cURL:**
```bash
curl -X GET \
  -H "Authorization: Basic <PAT>" \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/recyclebin?ids=12345,67890&api-version=7.2-preview'
```

**Ejemplo de respuesta:**
```json
{
  "count": 2,
  "value": [
    { "id": 12345, "name": "Bug: Error en login", ... },
    { "id": 67890, "name": "Tarea: Actualizar doc", ... }
  ]
}
```

---

### Get Deleted Work Item Shallow References

**Endpoint:**
```
GET /{organization}/{project}/_apis/wit/recyclebin?api-version=7.2-preview
```

**Ejemplo cURL:**
```bash
curl -X GET \
  -H "Authorization: Basic <PAT>" \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/recyclebin?api-version=7.2-preview'
```

**Ejemplo de respuesta:**
```json
{
  "count": 2,
  "value": [
    { "id": 12345, "url": "https://dev.azure.com/org/proj/_apis/wit/recyclebin/12345" },
    { "id": 67890, "url": "https://dev.azure.com/org/proj/_apis/wit/recyclebin/67890" }
  ]
}
```

---

### Restore Work Item

**Endpoint:**
```
PATCH /{organization}/{project}/_apis/wit/recyclebin/{id}?api-version=7.2-preview
```

**Request Body:** (vacío)

**Ejemplo cURL:**
```bash
curl -X PATCH \
  -H "Authorization: Basic <PAT>" \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/recyclebin/12345?api-version=7.2-preview'
```

**Ejemplo de respuesta:**
```json
{
  "id": 12345,
  "name": "Bug: Error en login",
  "restoredDate": "2025-08-05T10:00:00.000Z",
  "restoredBy": {
    "id": "a1b2c3d4-5678-90ab-cdef-1234567890ab",
    "displayName": "Juan Pérez"
  },
  "url": "https://dev.azure.com/org/proj/_apis/wit/workitems/12345"
}
```

---

## Filtros y propiedades a incluir

- `ids`: lista de IDs separados por coma (para batch GET)
- `id`: ID individual (para GET, PATCH, DELETE)
- El resultado incluye propiedades como `id`, `name`, `deletedDate`, `deletedBy`, `restoredDate`, `restoredBy`, `url`.

---
