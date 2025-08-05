# Classification Nodes

Permite crear, obtener, actualizar y eliminar nodos de clasificación (áreas y iteraciones) en un proyecto de Azure DevOps. Es fundamental para estructurar el trabajo y organizar los work items por áreas funcionales o por sprints/iteraciones.

**Documentación oficial:** [Classification Nodes](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/classification-nodes?view=azure-devops-rest-7.2)

## Operaciones principales

| Operación                | Endpoint                                                                                                              | Método |
|--------------------------|-----------------------------------------------------------------------------------------------------------------------|--------|
| Create Or Update         | /{organization}/{project}/_apis/wit/classificationnodes/{structureGroup}/{path}?api-version=7.2-preview               | PUT    |
| Delete                   | /{organization}/{project}/_apis/wit/classificationnodes/{structureGroup}/{path}?api-version=7.2-preview               | DELETE |
| Get                      | /{organization}/{project}/_apis/wit/classificationnodes/{structureGroup}/{path}?api-version=7.2-preview               | GET    |
| Get Classification Nodes | /{organization}/{project}/_apis/wit/classificationnodes?ids={ids}&api-version=7.2-preview                            | GET    |
| Get Root Nodes           | /{organization}/{project}/_apis/wit/classificationnodes?api-version=7.2-preview                                      | GET    |
| Update                   | /{organization}/{project}/_apis/wit/classificationnodes/{structureGroup}/{path}?api-version=7.2-preview               | PATCH  |

### Parámetros de URI

| Nombre          | Ubicación | Requerido | Tipo    | Descripción                                                                 |
|-----------------|-----------|-----------|---------|-----------------------------------------------------------------------------|
| organization    | path      | Sí        | string  | Nombre de la organización de Azure DevOps                                   |
| project         | path      | Sí        | string  | Nombre o ID del proyecto                                                    |
| structureGroup  | path      | Sí        | string  | 'areas' o 'iterations'                                                      |
| path            | path      | No        | string  | Ruta del nodo (ejemplo: 'Area1/Area2')                                      |
| ids             | query     | No        | string  | Lista de IDs de nodos separados por coma (para Get Classification Nodes)    |
| api-version     | query     | Sí        | string  | Versión de la API. Usar '7.2-preview'                                       |

### Cuerpo de la petición (Request Body)

- Para Create Or Update y Update: objeto JSON con los datos del nodo (por ejemplo, name, attributes, children).

Ejemplo de body para crear un área:
```json
{
  "name": "NuevaArea"
}
```

### Ejemplo de consumo con cURL

**Crear o actualizar un nodo:**
```bash
curl -X PUT \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/classificationnodes/areas/NuevaArea?api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>' \
  -H 'Content-Type: application/json' \
  -d '{"name": "NuevaArea"}'
```

**Obtener un nodo:**
```bash
curl -X GET \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/classificationnodes/areas/NuevaArea?api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

**Eliminar un nodo:**
```bash
curl -X DELETE \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/classificationnodes/areas/NuevaArea?api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

**Obtener nodos raíz:**
```bash
curl -X GET \
  'https://dev.azure.com/{organization}/{project}/_apis/wit/classificationnodes?api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

### Ejemplo de respuesta (Get)

```json
{
  "id": 1,
  "name": "NuevaArea",
  "structureType": "area",
  "path": "NuevaArea",
  "children": []
}
```

### Filtros y propiedades a incluir

- `structureGroup` puede ser 'areas' o 'iterations'.
- El parámetro `ids` permite filtrar por IDs específicos de nodos.
- El resultado puede incluir propiedades como `id`, `name`, `structureType`, `path`, `children`, y atributos adicionales según el tipo de nodo.
