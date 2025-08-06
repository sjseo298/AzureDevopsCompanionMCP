# Groups

## Overview
La API Groups permite gestionar los grupos en el formulario de work item de un proceso. Los grupos organizan controles y campos en secciones y páginas.

**Operaciones disponibles:**
- Añadir grupo (Add)
- Mover grupo a página (Move Group To Page)
- Mover grupo a sección (Move Group To Section)
- Eliminar grupo (Remove Group)
- Actualizar grupo (Update)

**Ejemplo cURL para añadir un grupo:**
```bash
curl -X POST "https://dev.azure.com/{organization}/_apis/work/processes/{processId}/workItemTypes/{workItemType}/layout/pages/{pageId}/sections/{sectionId}/groups?api-version=7.2-preview.1" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"id":"{groupId}", "label":"{label}", "order":0}'
```

**Parámetros principales:**
- organization (string)
- processId (string)
- workItemType (string)
- pageId (string)
- sectionId (string)
- groupId (string)
- api-version (string)

**Request/Response:**
- El cuerpo de la petición para crear/actualizar incluye propiedades como id, label, order, etc.
- La respuesta es un objeto JSON con los detalles del grupo.

**Filtros y valores posibles:**
- Los valores posibles para los campos están en la documentación oficial.

[Documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/processes/groups?view=azure-devops-rest-7.2)
