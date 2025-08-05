# Work Item Types Field

Permite consultar los campos definidos para un tipo de work item en un proyecto de Azure DevOps, incluyendo detalles y referencias. Útil para construir formularios dinámicos y validaciones de datos.

**Documentación oficial:** [Work Item Types Field](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/work-item-types-field?view=azure-devops-rest-7.2)

## Operaciones principales

| Operación | Endpoint                                                                                                 | Método |
|-----------|----------------------------------------------------------------------------------------------------------|--------|
| List      | /{organization}/{project}/_apis/wit/workitemtypes/{type}/fields?api-version=7.2-preview                  | GET    |
| Get       | /{organization}/{project}/_apis/wit/workitemtypes/{type}/fields/{field}?api-version=7.2-preview          | GET    |

## Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                 |
|----------------|-----------|-----------|--------|---------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps   |
| project        | path      | Sí        | string | Nombre o ID del proyecto                    |
| type           | path      | Sí        | string | Nombre del tipo de work item                |
| field          | path      | No (solo para Get) | string | Nombre del campo a consultar             |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview'       |

## Ejemplo de consumo con cURL (List)

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/workitemtypes/Bug/fields?api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

## Ejemplo de consumo con cURL (Get)

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/workitemtypes/Bug/fields/System.Title?api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

## Ejemplo de respuesta (List)

```json
{
  "count": 2,
  "value": [
    {
      "referenceName": "System.Title",
      "name": "Title",
      "type": "string",
      "readOnly": false
    },
    {
      "referenceName": "System.State",
      "name": "State",
      "type": "string",
      "readOnly": true
    }
  ]
}
```

## Filtros y propiedades a incluir

  - `value`: array de campos, cada uno con:
    - `referenceName`: nombre de referencia (string)
    - `name`: nombre visible (string)
    - `type`: tipo de dato (string)
    - `readOnly`: si es de solo lectura (bool)

## Notas

- Los campos definen la estructura de datos de cada tipo de work item.
- El scope mínimo requerido es `vso.work` (lectura de work items y metadatos).
