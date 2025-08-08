---
applyTo: "**"
description: Documento unificado con las reglas de diseño, flujo y validación para implementar APIs de Azure DevOps (MCP Server)
---

## Stack y Alcance
- Backend: Spring Boot (Java)
- Cliente HTTP: WebClient
- Protocolo: MCP
- Auth: Azure DevOps PAT
- Build: Gradle | Tests: JUnit 5 | Config: YAML/JSON
- Alcance: implementar TODO lo documentado en `api_doc/` (incluye subcarpetas referenciadas), siguiendo el orden alfabético de archivos.

## Variables de entorno y Bases
- Requeridas: `AZURE_DEVOPS_ORGANIZATION`, `AZURE_DEVOPS_PAT`
- Opcionales con default: `AZURE_DEVOPS_API_VERSION=7.2-preview.1`, `AZURE_DEVOPS_VSSPS_API_VERSION=7.1`
- Bases:
  - `DEVOPS_BASE=https://dev.azure.com/${AZURE_DEVOPS_ORGANIZATION}` (proyecto/organización)
  - `VSSPS_BASE=https://app.vssps.visualstudio.com` (Accounts/Profiles)
- Seguridad: nunca registrar el PAT.

## Flujo de trabajo obligatorio
1) Elegir endpoint en `api_doc/` (y sus subsecciones referenciadas).
2) Crear script(s) cURL de validación en `scripts/curl/<area>/`, usando `_env.sh`.
3) Ejecutar y ajustar hasta validar (sin 4xx/5xx con datos válidos).
4) Implementar herramienta MCP usando el cliente unificado.
5) Añadir tests mínimos, documentar uso y actualizar progreso.

## Reglas para scripts cURL
- Ubicación: `scripts/curl/<area>/<operacion>.sh`.
- Incluir `scripts/curl/_env.sh` (valida variables, define `curl_json`, `DEVOPS_BASE`, `VSSPS_BASE`).
- Aceptar parámetros por argumentos/flags (IDs, filtros) y construir URL correcta.
- Usar `curl_json "$URL"` y, si procede, formatear con `jq`.
- Para nombres con espacios o caracteres especiales (project/team), codificar en URL.

### Nivel de documentación requerido para cURL
- Cada script DEBE derivarse explícitamente del archivo en `api_doc/<area>/<endpoint>.md` correspondiente. No inventar rutas no documentadas localmente.
- Incluir un bloque `usage` que referencie el archivo MD fuente e indique:
  - Parámetros obligatorios/opcionales y flags admitidos.
  - Ejemplos de uso (incluyendo valores con espacios y caracteres especiales correctamente citados).
- Validar y exigir los parámetros marcados como obligatorios en la doc. Si en nuestra organización un parámetro opcional es de facto requerido, indicarlo (“obligatorio en nuestra org”).
- Respetar el nivel del recurso según la doc local: nivel proyecto, equipo o tablero. Si existen variantes, crear scripts separados y con nombres inequívocos (p.ej., `get_boardrows_project.sh` vs `get_boardrows.sh`).
- Codificar SIEMPRE todos los segmentos de ruta con `jq -rn --arg s "$val" '$s|@uri'`.
- Usar `"${AZURE_DEVOPS_API_VERSION}"` por defecto. Donde la doc local especifique una preview distinta, usar esa versión (ej.: capacities `7.2-preview.3`, cardsettings/cardrulesettings `7.2-preview.2`).
- Evitar endpoints no documentados en la doc local (p.ej., no usar `/work/columns` si `columns.md` exige board).
- Consideraciones de salida/seguridad:
  - Mostrar JSON formateado con `jq` cuando aplique; si la respuesta es binaria, mostrar headers para validar.
  - No imprimir secretos (PAT) ni variables sensibles.
  - El script debe mostrar `usage` y salir con código != 0 si faltan argumentos requeridos.

## Reglas para herramientas MCP
- Todas extienden `AbstractAzureDevOpsTool` y usan SIEMPRE `AzureDevOpsClientService` (sin lógica HTTP duplicada).
- Esquema de entrada: incluir `project` (requerido) y `team` (opcional). Parámetros específicos mínimos necesarios.
- Cliente HTTP:
  - Endpoints VSSPS: `getVsspsApi(pathWithQuery)`.
  - Endpoints de proyecto (work/core/etc.): helpers como `getWorkApi(project, team, path)`.
- Convenciones de nombres:
  - Formato: `azuredevops_<area>_<operacion>`.
  - Ejemplos: `azuredevops_core_get_projects`, `azuredevops_core_create_project`, `azuredevops_work_get_boards`, `azuredevops_accounts_get_accounts`, `azuredevops_profile_get_my_memberid`.
  - La clase Java mantiene el sufijo `Tool` (ej. `ProjectsTool`), y los tests `<Clase>Test`.
- Manejo de errores consistente y mensajes claros; no exponer secretos.
- Formateo de salida:
  - Listas (`count + value[]`): enumerar elementos.
  - Respuestas unitarias: mostrar campos clave.
  - Fallback a JSON/string si es complejo.

## Estándar de nombres por área (nuevo)
- Para identificar claramente el área de la API, los tools deben seguir `azuredevops_<area>_<operacion>`.
- Áreas comunes: `core`, `work`, `accounts`, `profile`, `wit`, `build`, etc.
- Ejemplos:
  - Core: `azuredevops_core_get_projects`, `azuredevops_core_get_teams`, `azuredevops_core_create_project`.
  - Work: `azuredevops_work_get_boards`, `azuredevops_work_get_backlogs`.
  - Accounts: `azuredevops_accounts_get_accounts`.
  - Profile: `azuredevops_profile_get_my_memberid`.
- Migración: renombrar herramientas existentes que no cumplan el estándar y actualizar tests/documentación.

## Manejo genérico de errores HTTP (Azure DevOps)
- El cliente `AzureDevOpsClientService` captura respuestas HTTP de error y las devuelve con estructura enriquecida:
  - `isHttpError: true`, `httpStatus`, `httpReason` y el cuerpo JSON original (por ejemplo: `message`, `typeKey`, `typeName`, `errorCode`).
- `AbstractAzureDevOpsTool` expone `tryFormatRemoteError(Map)` para formatear estos errores en texto claro para el agente.
- Reglas para herramientas:
  - Tras cada llamada a la API, invocar `tryFormatRemoteError(resp)`; si devuelve texto, retornar `success(textoError)` para mostrar un mensaje consistente al agente (sin stack traces).
  - Mantener el mismo formato en todas las herramientas para errores provenientes de Azure DevOps.
- Ejemplo de error formateado (del endpoint Boards):
  - Entrada JSON: `{ "message": "TF400499: You have not set your team field.", "typeKey": "InvalidTeamSettingsException", ... }`
  - Salida al agente: `Error remoto: TF400499: You have not set your team field. (type: InvalidTeamSettingsException)`

## Testing mínimo por herramienta
- Validar definición (nombre, descripción, schema) y parámetros obligatorios (`project`).
- (Opcional) Test de formateo con datos simulados si la lógica lo amerita.

## Criterios de Hecho
- Script(s) cURL creados y validados (sin 4xx/5xx con datos válidos).
- Herramienta MCP implementa la misma ruta y parámetros que el script validado.
- Tests mínimos en verde y documentación/progreso actualizados.

