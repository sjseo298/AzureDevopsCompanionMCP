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

## Flujo recomendado: Subir y asociar a Work Item en un paso

Para evitar adjuntos huérfanos, se recomienda usar el script `scripts/curl/wit/work_item_attachment_add.sh` que:
- Sube el archivo a Attachments
- Asocia inmediatamente la relación `AttachedFile` al Work Item
- Si el PATCH falla, intenta rollback eliminando el attachment creado

Opciones de origen del archivo:
- Cliente y servidor MCP en la misma instancia: usa ruta local/`file://` con el script.
- Cliente y servidor MCP en instancias separadas: usa el endpoint HTTP multipart para enviar el archivo en streaming (sin base64 ni filesystem compartido):

  POST /mcp/uploads/wit/workitems/{id}/attachment?project={project}
  Content-Type: multipart/form-data

  Partes soportadas:
  - file (obligatoria): el archivo a subir
  - fileName (opcional)
  - comment (opcional)
  - contentType (opcional)
  - apiVersion (opcional, default 7.2-preview)

Uso:

```bash
scripts/curl/wit/work_item_attachment_add.sh \
  --project "Mi Proyecto" \
  --id 12345 \
  --file ./evidencia.png \
  [--name evidencia.png] \
  [--comment "Screenshot del error"] \
  [--content-type image/png]
```

Ejemplos adicionales:
- Con file URI:
  ```bash
  scripts/curl/wit/work_item_attachment_add.sh --project MyProj --id 100 --file "file:///tmp/mi imagen.png" --comment "Adjunto"
  ```
- Con data URI:
  ```bash
  scripts/curl/wit/work_item_attachment_add.sh --project MyProj --id 101 --file "data:text/plain;base64,SG9sYQ==" --name hola.txt
  ```

Notas:
- El Content-Type por defecto es `application/octet-stream`; puedes forzarlo con `--content-type`.
- A nivel de herramienta MCP (`azuredevops_wit_work_item_attachment_add`), se recomienda usar `dataUrl` (data: URI inline) cuando no se pueda compartir filesystem. El soporte a `dataBase64` queda como [DEPRECATED] para compatibilidad.

### Ejemplo de uso del endpoint HTTP multipart (con curl):

```bash
curl -X POST \
  "http://localhost:8080/mcp/uploads/wit/workitems/123/attachment?project=MiProyecto" \
  -H "Accept: application/json" \
  -F "file=@./evidencia.png" \
  -F "fileName=evidencia.png" \
  -F "comment=Screenshot del error"
```
