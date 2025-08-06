# System Controls

## Overview
La API System Controls permite gestionar controles predeterminados como "Area Path", "Iteration Path" y "Reason" en el formulario de work item. Permite ocultar controles y editar etiquetas.

**Operaciones disponibles:**
- Eliminar modificación de control (Delete)
- Listar controles editados (List)
- Actualizar/agregar control (Update)

**Ejemplo cURL para listar controles editados:**
```bash
curl -X GET "https://dev.azure.com/{organization}/_apis/work/processes/{processId}/workItemTypes/{workItemType}/systemcontrols?api-version=7.2-preview.1" -H "Authorization: Bearer {token}"
```

**Parámetros principales:**
- organization (string)
- processId (string)
- workItemType (string)
- controlId (string)
- api-version (string)

**Request/Response:**
- El cuerpo de la petición para actualizar/agregar incluye propiedades como id, label, hidden, etc.
- La respuesta es un objeto JSON con los detalles del control.

**Filtros y valores posibles:**
- Los tipos de control y valores posibles están en la documentación oficial.

[Documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/processes/system-controls?view=azure-devops-rest-7.2)
