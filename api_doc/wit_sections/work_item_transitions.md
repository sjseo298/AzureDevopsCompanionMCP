# Work Item Transitions

Permite consultar los posibles estados siguientes para uno o varios work items en Azure DevOps, según las reglas de transición definidas en el proceso. Útil para validar o automatizar flujos de trabajo.

**Documentación oficial:** [Work Item Transitions](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/work-item-transitions?view=azure-devops-rest-7.2)

## Operaciones principales

| Operación | Endpoint                                                                                                 | Método |
|-----------|----------------------------------------------------------------------------------------------------------|--------|
| List      | /{organization}/{project}/_apis/wit/workitemtransitions?ids={ids}&api-version=7.2-preview                | GET    |

## Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                 |
|----------------|-----------|-----------|--------|---------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps   |
| project        | path      | Sí        | string | Nombre o ID del proyecto                    |
| ids            | query     | Sí        | string | Lista de IDs de work items separados por coma|
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview'       |

## Ejemplo de consumo con cURL

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/workitemtransitions?ids=123,456&api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

## Ejemplo de respuesta

```json
{
  "count": 2,
  "value": [
    {
      "id": 123,
      "nextState": "Closed"
    },
    {
      "id": 456,
      "nextState": "Active"
    }
  ]
}
```

## Filtros y propiedades a incluir

  - `value`: array de transiciones, cada una con:
    - `id`: ID del work item (int)
    - `nextState`: siguiente estado posible (string)

## Notas

- Permite validar y automatizar flujos de transición de estados en work items.
- El scope mínimo requerido es `vso.work` (lectura de work items y metadatos).
