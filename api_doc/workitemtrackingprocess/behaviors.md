# Behaviors

## Overview
La API Behaviors permite gestionar comportamientos en el proceso de Work Item Tracking. Los comportamientos definen reglas y acciones automáticas para los work items.

**Operaciones disponibles:**
- Crear comportamiento (Create)
- Eliminar comportamiento (Delete)
- Obtener comportamiento (Get)
- Listar comportamientos (List)
- Actualizar comportamiento (Update)

**Ejemplo cURL para listar comportamientos:**
```bash
curl -X GET "https://dev.azure.com/{organization}/_apis/work/processes/{processId}/behaviors?api-version=7.2-preview.1" -H "Authorization: Bearer {token}"
```

**Parámetros principales:**
- organization (string): Nombre de la organización.
- processId (string): ID del proceso.
- behaviorId (string): ID del comportamiento (para Get, Update, Delete).
- api-version (string): Versión de la API (ejemplo: 7.2-preview.1).

**Request/Response:**
- El cuerpo de la petición para crear/actualizar incluye propiedades como name, description, color, etc.
- La respuesta es un objeto JSON con los detalles del comportamiento.

**Filtros y valores posibles:**
- Todos los valores posibles para los campos se encuentran en la documentación oficial.

[Documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/processes/behaviors?view=azure-devops-rest-7.2)
