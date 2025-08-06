# Work Item Types

## Overview
La API Work Item Types permite crear, eliminar, obtener, listar y actualizar tipos de work item en un proceso. Los tipos definen la estructura y campos de los work items.

**Operaciones disponibles:**
- Crear tipo de work item (Create)
- Eliminar tipo de work item (Delete)
- Obtener tipo de work item (Get)
- Listar tipos de work item (List)
- Actualizar tipo de work item (Update)

**Ejemplo cURL para listar tipos de work item:**
```bash
curl -X GET "https://dev.azure.com/{organization}/_apis/work/processes/{processId}/workItemTypes?api-version=7.2-preview.1" -H "Authorization: Bearer {token}"
```

**Parámetros principales:**
- organization (string)
- processId (string)
- workItemTypeId (string)
- api-version (string)

**Request/Response:**
- El cuerpo de la petición para crear/actualizar incluye propiedades como id, name, description, fields, etc.
- La respuesta es un objeto JSON con los detalles del tipo de work item.

**Filtros y valores posibles:**
- Los tipos y valores posibles están en la documentación oficial.

[Documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/processes/work-item-types?view=azure-devops-rest-7.2)
