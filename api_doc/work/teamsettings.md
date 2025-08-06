
# TeamSettings

API para gestionar la configuración de equipo (team settings) en Azure DevOps Work.

## Operaciones principales
- Obtener configuración de equipo
- Actualizar configuración de equipo

**Endpoints principales:**
```
GET    https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings?api-version=7.2-preview.1
PATCH  https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings?api-version=7.2-preview.1
```

## Parámetros comunes
- organization (string, requerido)
- project (string, requerido)
- team (string, requerido)
- api-version (string, requerido, valor: 7.2-preview.1)

## Ejemplo de consumo (cURL)
```bash
# Obtener configuración de equipo
curl -X GET \
  -H "Accept: application/json" \
  -u :<PAT> \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings?api-version=7.2-preview.1"

# Actualizar configuración de equipo
curl -X PATCH \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -u :<PAT> \
  -d '{"bugsBehavior": "AsTasks", "workingDays": ["monday", "tuesday", "wednesday", "thursday"], "defaultIteration": "8C2457E8-8936-4CDC-B3AA-17B20F56C76C"}' \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings?api-version=7.2-preview.1"
```

## Filtros y propiedades
No existen filtros adicionales documentados para esta API.

## Respuesta
La respuesta incluye información sobre la configuración de equipo:
```json
{
  "backlogIteration": { "id": "uuid", "name": "Iteration", "path": "", "url": "..." },
  "bugsBehavior": "asTasks",
  "workingDays": ["monday", "tuesday", "wednesday", "thursday"],
  "backlogVisibilities": { "Microsoft.EpicCategory": false, "Microsoft.FeatureCategory": true },
  "defaultIteration": { "id": "uuid", "name": "Iteration 3", "path": "\\Iteration 3", "url": "..." },
  "defaultIterationMacro": null,
  "url": "https://dev.azure.com/{organization}/.../teamsettings",
  "_links": { ... }
}
```

## Definiciones
- TeamSetting: { backlogIteration, bugsBehavior, workingDays, backlogVisibilities, defaultIteration, defaultIterationMacro, url, _links }
- TeamSettingsIteration: { id, name, path, url, attributes }
- BugsBehavior: ["off", "asRequirements", "asTasks"]
- DayOfWeek: ["sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"]
- TimeFrame: ["past", "current", "future"]

## Permisos
- vso.work: Lectura de work items, queries, boards, etc.
- vso.work_write: Escritura y actualización de configuración de equipo.

## Referencias
- [Documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/teamsettings?view=azure-devops-rest-7.2)
