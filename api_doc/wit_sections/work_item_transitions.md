# Work Item Transitions

Permite consultar los posibles estados siguientes para uno o varios work items en Azure DevOps, según las reglas de transición definidas en el proceso. Útil para validar o automatizar flujos de trabajo.

**Documentación oficial:** [Work Item Transitions](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/work-item-transitions?view=azure-devops-rest-7.1)

> Nota (actualización agosto 2025): Anteriormente la doc local indicaba un segmento de proyecto en la ruta (`/{project}/_apis/...`), lo cual causaba HTML "Page not found". El endpoint correcto es a nivel organización: `/{organization}/_apis/wit/workitemtransitions`. Probado nuevamente con la ruta correcta (ids: 877023, 875205, 584494, 718959) usando `api-version=7.1` y sigue devolviendo HTML en esta organización. Se asume deshabilitado (feature flag) o restringido. Mantener en observación y no implementar Tool MCP hasta recibir JSON válido.

## Operaciones principales

| Operación | Endpoint                                                                                     | Método |
|-----------|----------------------------------------------------------------------------------------------|--------|
| List      | /{organization}/_apis/wit/workitemtransitions?ids={ids}&api-version=7.1                      | GET    |

## Parámetros de URI

| Nombre       | Ubicación | Requerido | Tipo   | Descripción                                                  |
|--------------|-----------|-----------|--------|--------------------------------------------------------------|
| organization | path      | Sí        | string | Nombre de la organización de Azure DevOps                    |
| ids          | query     | Sí        | string | Lista de IDs de work items separados por coma                |
| action       | query     | No        | string | Acción (sólo `checkin` soportado oficialmente, opcional)     |
| api-version  | query     | Sí        | string | Versión de la API. Usar '7.1' (estable)                      |

## Ejemplo de consumo con cURL

```bash
curl -X GET \
  "https://dev.azure.com/{organization}/_apis/wit/workitemtransitions?ids=123,456&api-version=7.1" \
  -H "Authorization: Basic <BASE64_PAT>"
```

## Ejemplo de respuesta (esperada según doc oficial – no recibida en la organización)

```json
{
  "count": 2,
  "value": [
    { "id": 123, "stateOnTransition": "Closed" },
    { "id": 456, "stateOnTransition": "Active" }
  ]
}
```

## Filtros y propiedades a incluir (cuando esté disponible)

- `value`: array de objetos `WorkItemNextStateOnTransition`:
  - `id`: ID del work item (int)
  - `stateOnTransition`: siguiente estado posible (string)
  - `errorCode` / `message`: presentes si no hay transición determinada

## Notas

- Endpoint organization-level (no incluye segmento de proyecto).
- Scope mínimo requerido: `vso.work`.
- Actualmente devuelve HTML en la organización (ver nota superior); sin implementación de script/tool definitiva aún.

## Alternativas para inferir transiciones (workaround)

1. Estados del tipo de work item: `/_apis/wit/workitemtypes/{type}/states`.
2. Historial de revisiones: grafo observado de cambios de estado reales.
3. API de procesos (workitemtrackingprocess) para reglas/estados si el proceso es heredado.
4. Intentos controlados de PATCH (entorno de prueba) para validar transición.
5. Catálogo interno mantenido manualmente.

Actualizar este documento y añadir scripts/tools MCP cuando el endpoint empiece a devolver JSON válido.
