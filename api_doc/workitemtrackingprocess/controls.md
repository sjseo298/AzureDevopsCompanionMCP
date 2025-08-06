# Controls

## Overview
La API Controls permite gestionar los controles en el formulario de work item de un proceso. Los controles son los elementos visuales y de entrada en el formulario.

**Operaciones disponibles:**
- Crear control (Create)
- Mover control a grupo (Move Control To Group)
- Eliminar control de grupo (Remove Control From Group)
- Actualizar control (Update)

**Ejemplo cURL para crear un control:**
```bash
curl -X POST "https://dev.azure.com/{organization}/_apis/work/processes/{processId}/workItemTypes/{workItemType}/layout/groups/{groupId}/controls?api-version=7.2-preview.1" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"id":"{controlId}", "label":"{label}", "type":"{type}"}'
```

**Parámetros principales:**
- organization (string)
- processId (string)
- workItemType (string)
- groupId (string)
- controlId (string)
- api-version (string)

**Request/Response:**
- El cuerpo de la petición para crear/actualizar incluye propiedades como id, label, type, etc.
- La respuesta es un objeto JSON con los detalles del control.

**Filtros y valores posibles:**
- Los tipos de control y valores posibles están en la documentación oficial.

[Documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/processes/controls?view=azure-devops-rest-7.2)
