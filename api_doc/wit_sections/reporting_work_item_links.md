---

# Reporting Work Item Links

Permite obtener lotes de vínculos (links) entre work items para construir un almacén de datos o sincronizar integraciones. Devuelve los enlaces de trabajo, soporta paginación por lotes y permite filtrar por tipo de vínculo, tipo de work item y fecha de cambio.

**Documentación oficial:** [Reporting Work Item Links](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/reporting-work-item-links?view=azure-devops-rest-7.2)

## Operaciones principales

| Operación | Endpoint                                                                                                 | Método |
|-----------|----------------------------------------------------------------------------------------------------------|--------|
| Get       | /{organization}/{project}/_apis/wit/reporting/workitemlinks?api-version=7.2-preview.3                    | GET    |

---

## Parámetros de URI

| Nombre             | Ubicación | Requerido | Tipo                | Descripción                                                                                 |
|--------------------|-----------|-----------|---------------------|---------------------------------------------------------------------------------------------|
| organization       | path      | Sí        | string              | Nombre de la organización de Azure DevOps                                                   |
| project            | path      | No        | string              | Nombre o ID del proyecto (opcional, para toda la colección omitir)                          |
| api-version        | query     | Sí        | string              | Versión de la API. Usar '7.2-preview.3'                                                     |
| continuationToken  | query     | No        | string              | Token de continuación para paginación. Omitir para el primer lote.                          |
| linkTypes          | query     | No        | array(string)       | Lista de tipos de vínculo a incluir (ej: 'System.LinkTypes.Hierarchy').                     |
| types              | query     | No        | array(string)       | Lista de tipos de work item a incluir (ej: 'Bug', 'Task').                                  |
| startDateTime      | query     | No        | string (date-time)  | Fecha/hora de inicio para filtrar cambios de vínculos posteriores a ese momento.            |

---

## Ejemplo de consumo con cURL

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/reporting/workitemlinks?api-version=7.2-preview.3' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

**Con filtros:**
```bash
curl -X GET \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/reporting/workitemlinks?linkTypes=System.LinkTypes.Hierarchy&types=Bug,Task&startDateTime=2025-01-01T00:00:00Z&api-version=7.2-preview.3' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

---

## Ejemplo de respuesta

```json
{
  "values": [
    {
      "rel": "System.LinkTypes.Hierarchy",
      "sourceId": 7,
      "targetId": 8,
      "changedDate": "2014-03-18T17:17:52.02Z",
      "isActive": true
    },
    {
      "rel": "System.LinkTypes.Hierarchy",
      "sourceId": 7,
      "targetId": 9,
      "changedDate": "2014-03-18T17:18:03.007Z",
      "isActive": true
    }
  ],
  "nextLink": "https://dev.azure.com/{organization}/{project}/_apis/wit/reporting/workitemlinks?continuationToken=6281123&api-version=7.2-preview.3",
  "isLastBatch": true
}
```

---

## Filtros y propiedades a incluir

- `linkTypes`: lista de tipos de vínculo (ejemplo: 'System.LinkTypes.Hierarchy', 'System.LinkTypes.Related', etc.).
- `types`: lista de tipos de work item (ejemplo: 'Bug', 'Task', 'User Story', etc.).
- `continuationToken`: para paginación, úsalo en la siguiente llamada usando el valor de `nextLink`.
- `startDateTime`: filtra solo vínculos cambiados después de la fecha/hora indicada.
- El resultado incluye:
  - `values`: array de vínculos, cada uno con:
    - `rel`: tipo de vínculo (string)
    - `sourceId`: ID del work item origen (int)
    - `targetId`: ID del work item destino (int)
    - `changedDate`: fecha/hora de cambio (string, date-time)
    - `isActive`: si el vínculo está activo (bool)
  - `nextLink`: URL para obtener el siguiente lote (string)
  - `isLastBatch`: indica si es el último lote disponible (bool)

---

## Notas de paginación y uso

- Para obtener todos los vínculos, realiza llamadas sucesivas usando el `nextLink` hasta que `isLastBatch` sea `true`.
- Si omites `project`, la consulta es a nivel de toda la colección.
- El scope mínimo requerido es `vso.work` (lectura de work items y metadatos).

---
