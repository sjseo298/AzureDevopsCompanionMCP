# Work Item Icons

Permite obtener los íconos asociados a los tipos de work item definidos en el proceso de un proyecto. Útil para mostrar visualmente el tipo de work item en interfaces de usuario.

**Documentación oficial:** [Work Item Icons](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/work-item-icons?view=azure-devops-rest-7.2)

## Operaciones principales

| Operación | Endpoint                                                                                                 | Método |
|-----------|----------------------------------------------------------------------------------------------------------|--------|
| List     | /{organization}/_apis/wit/workitemicons?api-version=7.2-preview.1                                         | GET    |
| Get      | /{organization}/_apis/wit/workitemicons/{icon}?api-version=7.2-preview.1                                  | GET    |

## Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                 |
|----------------|-----------|-----------|--------|---------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps   |
| icon           | path      | No (solo para Get) | string | Nombre del ícono a consultar              |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.1'     |

## Ejemplo de consumo con cURL (List)

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/_apis/wit/workitemicons?api-version=7.2-preview.1' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

## Ejemplo de consumo con cURL (Get)

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/_apis/wit/workitemicons/icon_bug?api-version=7.2-preview.1' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

## Ejemplo de respuesta (List)

```json
{
  "count": 2,
  "value": [
    {
      "id": "icon_bug",
      "url": "https://dev.azure.com/org/_apis/wit/workitemicons/icon_bug",
      "updatedTime": "2025-08-01T12:34:56.789Z"
    },
    {
      "id": "icon_task",
      "url": "https://dev.azure.com/org/_apis/wit/workitemicons/icon_task",
      "updatedTime": "2025-08-01T12:34:56.789Z"
    }
  ]
}
```

## Filtros y propiedades a incluir

  - `value`: array de íconos, cada uno con:
    - `id`: identificador del ícono (string)
    - `url`: URL del recurso del ícono (string)
    - `updatedTime`: fecha/hora de última actualización (string, date-time)

## Notas

- Los íconos pueden ser utilizados para personalizar la experiencia visual en tableros y vistas de work items.
- El scope mínimo requerido es `vso.work` (lectura de work items y metadatos).

## Parámetros de URI

| Nombre         | Ubicación | Requerido | Tipo   | Descripción                                 |
|----------------|-----------|-----------|--------|---------------------------------------------|
| organization   | path      | Sí        | string | Nombre de la organización de Azure DevOps   |
| api-version    | query     | Sí        | string | Versión de la API. Usar '7.2-preview.1'     |

---

## Ejemplo de consumo con cURL

```bash
curl -X GET \
  'https://dev.azure.com/{organization}/_apis/wit/workitemicons?api-version=7.2-preview.1' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

---

## Ejemplo de respuesta

```json
{
  "count": 2,
  "value": [
    {
      "id": "icon_bug",
      "url": "https://dev.azure.com/org/_apis/wit/workitemicons/icon_bug"
    },
    {
      "id": "icon_task",
      "url": "https://dev.azure.com/org/_apis/wit/workitemicons/icon_task"
    }
  ]
}
```

---

## Filtros y propiedades a incluir

- `id`: identificador del ícono (string)
- `url`: URL del recurso del ícono (string)

---

## Notas

- Los íconos pueden ser utilizados para personalizar la experiencia visual en tableros y vistas de work items.
- El scope mínimo requerido es `vso.work` (lectura de work items y metadatos).
