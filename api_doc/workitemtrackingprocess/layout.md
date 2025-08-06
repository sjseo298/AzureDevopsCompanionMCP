# Layout

## Overview
La API Layout permite obtener el layout completo del formulario de work item para un proceso. El layout define la estructura visual y lógica del formulario.

**Operaciones disponibles:**
- Obtener layout (Get)

**Ejemplo cURL para obtener el layout:**
```bash
curl -X GET "https://dev.azure.com/{organization}/_apis/work/processes/{processId}/workItemTypes/{workItemType}/layout?api-version=7.2-preview.1" -H "Authorization: Bearer {token}"
```

**Parámetros principales:**
- organization (string)
- processId (string)
- workItemType (string)
- api-version (string)

**Request/Response:**
- La respuesta es un objeto JSON con la estructura completa del layout.

**Filtros y valores posibles:**
- Los valores posibles para los campos están en la documentación oficial.

[Documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/processes/layout?view=azure-devops-rest-7.2)
