# Attachments

Permite crear, obtener y eliminar permanentemente archivos adjuntos asociados a work items. Soporta carga directa y carga en fragmentos (chunked upload) para archivos grandes.

**Documentación oficial:** [Attachments](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/attachments?view=azure-devops-rest-7.2)

## Operaciones principales

| Operación      | Endpoint                                                                                 | Método |
|--------------- |------------------------------------------------------------------------------------------|--------|
| Create         | /_apis/wit/attachments?fileName={fileName}&api-version=7.2-preview                       | POST   |
| Get            | /_apis/wit/attachments/{id}?api-version=7.2-preview                                      | GET    |
| Delete         | /_apis/wit/attachments/{id}?api-version=7.2-preview                                      | DELETE |
| Upload Chunk   | /_apis/wit/attachments/{id}/chunks?api-version=7.2-preview                               | POST   |

### Parámetros de URI

| Nombre       | Ubicación | Requerido | Tipo   | Descripción                                 |
|--------------|-----------|-----------|--------|---------------------------------------------|
| organization | path      | Sí        | string | Nombre de la organización de Azure DevOps   |
| id           | path      | Sí (Get, Delete, Upload Chunk) | string | ID del adjunto                             |
| fileName     | query     | Sí (Create) | string | Nombre del archivo a subir                  |
| api-version  | query     | Sí        | string | Versión de la API. Usar '7.2-preview'       |

### Cuerpo de la petición (Request Body)

- Para Create: binario del archivo a subir (ejemplo: @archivo.png)
- Para Upload Chunk: fragmento binario del archivo

### Ejemplo de consumo con cURL

**Subir un archivo adjunto (Create):**

```bash
curl -X POST \
  'https://dev.azure.com/{organization}/_apis/wit/attachments?fileName=ejemplo.png&api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>' \
  -H 'Content-Type: application/octet-stream' \
  --data-binary '@ejemplo.png'
```

**Obtener un adjunto:**
```bash
curl -X GET \
  'https://dev.azure.com/{organization}/_apis/wit/attachments/{id}?api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>' \
  -o archivo_descargado.png
```

**Eliminar un adjunto:**
```bash
curl -X DELETE \
  'https://dev.azure.com/{organization}/_apis/wit/attachments/{id}?api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>'
```

**Subir un fragmento (chunk) de un archivo grande:**
```bash
curl -X POST \
  'https://dev.azure.com/{organization}/_apis/wit/attachments/{id}/chunks?api-version=7.2-preview' \
  -H 'Authorization: Basic <BASE64_PAT>' \
  -H 'Content-Type: application/octet-stream' \
  --data-binary '@fragmento.bin'
```

### Ejemplo de respuesta (Create)

```json
{
  "id": "b1c2d3e4-5678-90ab-cdef-1234567890ab",
  "url": "https://dev.azure.com/{organization}/_apis/wit/attachments/b1c2d3e4-5678-90ab-cdef-1234567890ab"
}
```
