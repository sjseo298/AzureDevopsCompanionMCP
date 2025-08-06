---
# ProcessConfiguration

## Descripción
API para obtener la configuración de proceso de un proyecto en Azure DevOps Work. Devuelve la configuración de backlogs, mapeo de estados, campos y reglas de proceso.

## Endpoint principal
```
GET https://dev.azure.com/{organization}/{project}/_apis/work/processconfiguration?api-version=7.2-preview.1
```

## Parámetros
| Nombre        | En    | Tipo   | Requerido | Descripción                                 |
|-------------- |-------|--------|-----------|---------------------------------------------|
| organization  | path  | string | Sí        | Nombre de la organización Azure DevOps      |
| project       | path  | string | Sí        | Nombre o ID del proyecto                    |
| api-version   | query | string | Sí        | Versión de la API (`7.2-preview.1`)         |

## Ejemplo de consumo (cURL)
```bash
curl -X GET \
  -H "Accept: application/json" \
  -u :<PAT> \
  "https://dev.azure.com/{organization}/{project}/_apis/work/processconfiguration?api-version=7.2-preview.1"
```

## Propiedades de la respuesta

```json
{
  "backlogFields": {
    "typeFields": {
      "Order": "Microsoft.VSTS.Common.StackRank",
      "Effort": "Microsoft.VSTS.Scheduling.StoryPoints",
      "RemainingWork": "Microsoft.VSTS.Scheduling.RemainingWork",
      "Activity": "Microsoft.VSTS.Common.Activity"
    }
  },
  "hiddenBacklogs": ["Microsoft.EpicCategory"],
  "isBugsBehaviorConfigured": true,
  "bugsBehavior": "asTasks",
  "workItemTypeMappedStates": [
    {
      "workItemTypeName": "User Story",
      "states": [
        { "name": "New", "category": "Proposed" },
        { "name": "Active", "category": "InProgress" },
        { "name": "Closed", "category": "Complete" }
      ]
    }
  ],
  "portfolioBacklogs": [ /* ... */ ],
  "requirementBacklog": { /* ... */ },
  "taskBacklog": { /* ... */ },
  "url": "https://dev.azure.com/{organization}/{project}/_apis/work/processconfiguration"
}
```

### Descripción de propiedades principales
- `backlogFields`: Mapeo de campos clave para backlog (Order, Effort, etc.)
- `hiddenBacklogs`: Categorías de backlog ocultas
- `isBugsBehaviorConfigured`: Indica si el comportamiento de bugs está configurado
- `bugsBehavior`: Cómo se gestionan los bugs (`off`, `asRequirements`, `asTasks`)
- `workItemTypeMappedStates`: Estados mapeados por tipo de work item
- `portfolioBacklogs`, `requirementBacklog`, `taskBacklog`: Configuración de cada nivel de backlog
- `url`: URL de la API

## Valores posibles (Enums)

### BugsBehavior
- `off`
- `asRequirements`
- `asTasks`

### Categorías de estado
- `Proposed`
- `InProgress`
- `Complete`

## Filtros y propiedades adicionales
No existen filtros adicionales documentados para este endpoint. La respuesta incluye toda la configuración de proceso relevante para el proyecto.

## Referencias
- [Documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/processconfiguration?view=azure-devops-rest-7.2)

---
