# Fields

## Overview
La API Fields permite añadir, obtener, listar y eliminar campos de un tipo de work item en un proceso. Los campos definen la información que puede almacenar un work item.

**Operaciones disponibles:**
- Añadir campo (Add)
- Obtener campo (Get)
- Listar campos (List)
- Eliminar campo de tipo de work item (Remove Work Item Type Field)
- Actualizar campo (Update)

**Ejemplo cURL para listar campos:**
```bash
curl -X GET "https://dev.azure.com/{organization}/_apis/work/processes/{processId}/workItemTypes/{workItemType}/fields?api-version=7.2-preview.1" -H "Authorization: Bearer {token}"
```

**Parámetros principales:**
- organization (string)
- processId (string)
- workItemType (string)
- fieldId (string)
- api-version (string)

**Request/Response:**
- El cuerpo de la petición para crear/actualizar incluye propiedades como id, name, type, etc.
- La respuesta es un objeto JSON con los detalles del campo.

**Filtros y valores posibles:**
- Los tipos de campo y valores posibles están en la documentación oficial.

[Documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/processes/fields?view=azure-devops-rest-7.2)
