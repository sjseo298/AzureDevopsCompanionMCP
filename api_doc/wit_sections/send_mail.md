# Send Mail

Permite enviar un correo electrónico relacionado a un work item específico, útil para notificaciones manuales o flujos personalizados.

**Documentación oficial:** [Send Mail](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/send-mail?view=azure-devops-rest-7.2)

### Operación principal

| Operación | Endpoint | Método |
|-----------|----------|--------|
| Send      | /{organization}/{project}/_apis/wit/sendmail?api-version=7.2-preview.1 | POST   |

---

### Parámetros de URI

| Nombre        | Ubicación | Tipo     | Requerido | Descripción                                 | Ejemplo           |
|---------------|-----------|----------|-----------|---------------------------------------------|-------------------|
| organization  | path      | string   | Sí        | Nombre de la organización de Azure DevOps   | fabrikam          |
| project       | path      | string   | Sí        | Nombre o ID del proyecto                    | Fabrikam-Fiber-Git|
| api-version   | query     | string   | Sí        | Versión de la API                          | 7.2-preview.1     |

### Cuerpo (body) de la petición

```json
{
  "message": {
    "to": ["usuario@dominio.com"],
    "cc": ["otro@dominio.com"],
    "replyTo": ["remitente@dominio.com"],
    "subject": "Asunto del correo",
    "body": "Contenido del mensaje en HTML o texto plano"
  },
  "workItemIds": [123, 456],
  "reason": "Comentario opcional sobre el motivo del envío"
}
```

**Campos del body:**

- `message.to` (array, requerido): Destinatarios principales.
- `message.cc` (array, opcional): Copia.
- `message.replyTo` (array, opcional): Responder a.
- `message.subject` (string, requerido): Asunto.
- `message.body` (string, requerido): Cuerpo del mensaje (HTML o texto).
- `workItemIds` (array, requerido): IDs de work items relacionados.
- `reason` (string, opcional): Motivo del envío.

---

### Ejemplo de uso con curl

```bash
curl -X POST \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "message": {
      "to": ["usuario@dominio.com"],
      "subject": "Notificación de Work Item",
      "body": "<b>Se ha actualizado el work item</b>"
    },
    "workItemIds": [123]
  }' \
  "https://dev.azure.com/fabrikam/Fabrikam-Fiber-Git/_apis/wit/sendmail?api-version=7.2-preview.1"
```

---

### Respuesta de ejemplo

```json
{
  "status": "Mail Sent"
}
```
