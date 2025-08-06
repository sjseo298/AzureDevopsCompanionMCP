# Lists (Picklists)

## Overview
La API Lists (Picklists) permite crear, obtener, listar y actualizar listas de selección (picklists) en el proceso de Work Item Tracking.

**Operaciones disponibles:**
- Crear picklist (Create)
- Eliminar picklist (Delete)
- Obtener picklist (Get)
- Listar picklists (List)
- Actualizar picklist (Update)

**Ejemplo cURL para listar picklists:**
```bash
curl -X GET "https://dev.azure.com/{organization}/_apis/work/processes/lists?api-version=7.2-preview.1" -H "Authorization: Bearer {token}"
```

**Parámetros principales:**
- organization (string)
- picklistId (string)
- api-version (string)

**Request/Response:**
- El cuerpo de la petición para crear/actualizar incluye propiedades como id, name, type, items, etc.
- La respuesta es un objeto JSON con los detalles del picklist.

**Filtros y valores posibles:**
- Los tipos de picklist y valores posibles están en la documentación oficial.

[Documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/processes/lists?view=azure-devops-rest-7.2)
