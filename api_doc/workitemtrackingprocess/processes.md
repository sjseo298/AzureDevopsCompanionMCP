# Processes

## Overview
La API Processes permite crear, editar, obtener y listar procesos heredados en Azure DevOps. Los procesos definen la estructura y reglas de los work items.

**Operaciones disponibles:**
- Crear proceso (Create)
- Eliminar proceso (Delete)
- Editar proceso (Edit Process)
- Obtener proceso (Get)
- Listar procesos (List)

**Ejemplo cURL para listar procesos:**
```bash
curl -X GET "https://dev.azure.com/{organization}/_apis/work/processes?api-version=7.2-preview.1" -H "Authorization: Bearer {token}"
```

**Parámetros principales:**
- organization (string)
- processId (string)
- api-version (string)

**Request/Response:**
- El cuerpo de la petición para crear/editar incluye propiedades como name, description, type, etc.
- La respuesta es un objeto JSON con los detalles del proceso.

**Filtros y valores posibles:**
- Los tipos de proceso y valores posibles están en la documentación oficial.

[Documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/processes/processes?view=azure-devops-rest-7.2)
