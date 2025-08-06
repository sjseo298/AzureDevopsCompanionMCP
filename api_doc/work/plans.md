---
# Plans

## Descripción
API para gestionar planes (Scaled Agile plans) en Azure DevOps Work. Permite crear, consultar, actualizar y eliminar planes de equipo.

## Operaciones principales
- Crear un plan para el equipo
- Eliminar un plan
- Obtener información de un plan
- Listar todos los planes configurados para un equipo
- Actualizar información de un plan

## Endpoints principales
```
GET    https://dev.azure.com/{organization}/{project}/{team}/_apis/work/plans?api-version=7.2-preview.1
POST   https://dev.azure.com/{organization}/{project}/{team}/_apis/work/plans?api-version=7.2-preview.1
GET    https://dev.azure.com/{organization}/{project}/{team}/_apis/work/plans/{id}?api-version=7.2-preview.1
DELETE https://dev.azure.com/{organization}/{project}/{team}/_apis/work/plans/{id}?api-version=7.2-preview.1
PATCH  https://dev.azure.com/{organization}/{project}/{team}/_apis/work/plans/{id}?api-version=7.2-preview.1
```

## Parámetros
| Nombre        | En    | Tipo   | Requerido | Descripción                                 |
|-------------- |-------|--------|-----------|---------------------------------------------|
| organization  | path  | string | Sí        | Nombre de la organización Azure DevOps      |
| project       | path  | string | Sí        | Nombre o ID del proyecto                    |
| team          | path  | string | Sí        | Nombre o ID del equipo                      |
| id            | path  | string | No*       | ID del plan (solo para operaciones sobre uno específico) |
| api-version   | query | string | Sí        | Versión de la API (`7.2-preview.1`)         |

\* Solo requerido para GET/DELETE/PATCH de un plan específico.

## Ejemplo de consumo (cURL)
```bash
# Listar planes
curl -X GET \
  -H "Accept: application/json" \
  -u :<PAT> \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/plans?api-version=7.2-preview.1"

# Obtener un plan específico
curl -X GET \
  -H "Accept: application/json" \
  -u :<PAT> \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/plans/{id}?api-version=7.2-preview.1"
```

## Ejemplo de respuesta
```json
{
  "count": 1,
  "value": [
    {
      "id": "1",
      "name": "Plan de Iteración",
      "type": "deliveryTimeline",
      "createdByIdentity": {
        "id": "...",
        "displayName": "Usuario Ejemplo"
      },
      "createdDate": "2025-08-01T12:00:00Z",
      "ownerIdentity": {
        "id": "...",
        "displayName": "Usuario Ejemplo"
      },
      "revision": 2,
      "url": "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/plans/1"
    }
  ]
}
```

## Propiedades principales
- `id`: Identificador del plan
- `name`: Nombre del plan
- `type`: Tipo de plan (`deliveryTimeline`, etc.)
- `createdByIdentity`: Usuario que creó el plan
- `createdDate`: Fecha de creación
- `ownerIdentity`: Usuario propietario
- `revision`: Número de revisión
- `url`: URL del recurso

## Valores posibles (Enums)

### type
- `deliveryTimeline`
- `custom`

## Filtros y propiedades adicionales
No existen filtros adicionales documentados para este endpoint. La respuesta incluye la información completa del plan.

## Referencias
- [Documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/plans?view=azure-devops-rest-7.2)

---
