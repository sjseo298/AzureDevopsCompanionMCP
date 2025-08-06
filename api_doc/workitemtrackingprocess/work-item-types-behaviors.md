# Work Item Types Behaviors

## Overview
La API Work Item Types Behaviors permite añadir, obtener, listar, eliminar y actualizar comportamientos asociados a un tipo de work item en el proceso.

**Operaciones disponibles:**
- Añadir comportamiento (Add)
- Obtener comportamiento (Get)
- Listar comportamientos (List)
- Eliminar comportamiento (Remove Behavior From Work Item Type)
- Actualizar comportamiento (Update)

**Ejemplo cURL para listar comportamientos:**
```bash
curl -X GET "https://dev.azure.com/{organization}/_apis/work/processes/{processId}/workItemTypes/{workItemType}/behaviors?api-version=7.2-preview.1" -H "Authorization: Bearer {token}"
```

**Parámetros principales:**
- organization (string)
- processId (string)
- workItemType (string)
- behaviorId (string)
- api-version (string)

**Request/Response:**
- El cuerpo de la petición para crear/actualizar incluye propiedades como id, name, description, etc.
- La respuesta es un objeto JSON con los detalles del comportamiento.

**Filtros y valores posibles:**
- Los tipos y valores posibles están en la documentación oficial.

[Documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/processes/work-item-types-behaviors?view=azure-devops-rest-7.2)
