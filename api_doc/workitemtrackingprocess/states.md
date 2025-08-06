# States

## Overview
La API States permite crear, obtener, listar, ocultar y actualizar estados de un tipo de work item en el proceso. Los estados definen el ciclo de vida de los work items.

**Operaciones disponibles:**
- Crear estado (Create)
- Eliminar estado (Delete)
- Obtener estado (Get)
- Ocultar estado (Hide State Definition)
- Listar estados (List)
- Actualizar estado (Update)

**Ejemplo cURL para listar estados:**
```bash
curl -X GET "https://dev.azure.com/{organization}/_apis/work/processes/{processId}/workItemTypes/{workItemType}/states?api-version=7.2-preview.1" -H "Authorization: Bearer {token}"
```

**Parámetros principales:**
- organization (string)
- processId (string)
- workItemType (string)
- stateId (string)
- api-version (string)

**Request/Response:**
- El cuerpo de la petición para crear/actualizar incluye propiedades como id, name, color, category, etc.
- La respuesta es un objeto JSON con los detalles del estado.

**Filtros y valores posibles:**
- Los tipos de estado y valores posibles están en la documentación oficial.

[Documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/processes/states?view=azure-devops-rest-7.2)
