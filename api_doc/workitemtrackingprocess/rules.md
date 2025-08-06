# Rules

## Overview
La API Rules permite añadir, obtener, listar, eliminar y actualizar reglas de work item en el proceso. Las reglas definen acciones automáticas y validaciones.

**Operaciones disponibles:**
- Añadir regla (Add)
- Eliminar regla (Delete)
- Obtener regla (Get)
- Listar reglas (List)
- Actualizar regla (Update)

**Ejemplo cURL para listar reglas:**
```bash
curl -X GET "https://dev.azure.com/{organization}/_apis/work/processes/{processId}/workItemTypes/{workItemType}/rules?api-version=7.2-preview.1" -H "Authorization: Bearer {token}"
```

**Parámetros principales:**
- organization (string)
- processId (string)
- workItemType (string)
- ruleId (string)
- api-version (string)

**Request/Response:**
- El cuerpo de la petición para crear/actualizar incluye propiedades como id, name, conditions, actions, etc.
- La respuesta es un objeto JSON con los detalles de la regla.

**Filtros y valores posibles:**
- Los tipos de regla y valores posibles están en la documentación oficial.

[Documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/processes/rules?view=azure-devops-rest-7.2)
