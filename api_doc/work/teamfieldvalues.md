
# TeamFieldValues

API para gestionar los valores de campo de equipo (team field values) en Azure DevOps Work.

## Operaciones principales
- Obtener valores de campo de equipo
- Actualizar valores de campo de equipo

**Endpoints principales:**
```
GET    https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings/teamfieldvalues?api-version=7.2-preview.1
PATCH  https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings/teamfieldvalues?api-version=7.2-preview.1
```

## Parámetros comunes
- organization (string, requerido)
- project (string, requerido)
- team (string, requerido)
- api-version (string, requerido, valor: 7.2-preview.1)

## Ejemplo de consumo (cURL)
```bash
# Obtener valores de campo de equipo
curl -X GET \
  -H "Accept: application/json" \
  -u :<PAT> \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings/teamfieldvalues?api-version=7.2-preview.1"

# Actualizar valores de campo de equipo
curl -X PATCH \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -u :<PAT> \
  -d '{"defaultValue": "Fabrikam-Fiber\\Auto", "values": [{"value": "Fabrikam-Fiber\\Auto", "includeChildren": true}]}' \
  "https://dev.azure.com/{organization}/{project}/{team}/_apis/work/teamsettings/teamfieldvalues?api-version=7.2-preview.1"
```

## Filtros y propiedades
No existen filtros adicionales documentados para esta API.

## Respuesta
La respuesta incluye información sobre los valores de campo de equipo:
```json
{
  "field": { "referenceName": "System.AreaPath", "url": "..." },
  "defaultValue": "Fabrikam-Fiber\\Auto",
  "values": [
    { "value": "Fabrikam-Fiber\\Auto", "includeChildren": true },
    { "value": "Fabrikam-Fiber\\Fiber", "includeChildren": false }
  ],
  "url": "https://dev.azure.com/{organization}/.../teamfieldvalues",
  "_links": { ... }
}
```

## Definiciones
- FieldReference: { referenceName: string, url: string }
- TeamFieldValue: { value: string, includeChildren: bool }
- TeamFieldValues: { field: FieldReference, defaultValue: string, values: TeamFieldValue[], url: string, _links: object }

## Permisos
- vso.work: Lectura de work items, queries, boards, etc.
- vso.work_write: Escritura y actualización de valores de campo de equipo.

## Referencias
- [Documentación oficial](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/teamfieldvalues?view=azure-devops-rest-7.2)
