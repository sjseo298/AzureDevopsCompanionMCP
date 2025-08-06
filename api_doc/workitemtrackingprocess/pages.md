# Pages

## Overview
La API Pages permite añadir, eliminar y actualizar páginas en el formulario de work item de un proceso. Las páginas organizan los grupos y controles en el formulario.

**Operaciones disponibles:**
- Añadir página (Add)
- Eliminar página (Remove Page)
- Actualizar página (Update)

**Ejemplo cURL para añadir una página:**
```bash
curl -X POST "https://dev.azure.com/{organization}/_apis/work/processes/{processId}/workItemTypes/{workItemType}/layout/pages?api-version=7.2-preview.1" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"id":"{pageId}", "label":"{label}", "order":0}'
```

**Parámetros principales:**
- organization (string)
- processId (string)
- workItemType (string)
- pageId (string)
- api-version (string)

**Request/Response:**
- El cuerpo de la petición para crear/actualizar incluye propiedades como id, label, order, etc.
- La respuesta es un objeto JSON con los detalles de la página.

**Filtros y valores posibles:**
- Los valores posibles para los campos están en la documentación oficial.

[Documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/processes/pages?view=azure-devops-rest-7.2)
